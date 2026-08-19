package com.ooruva.app.data.repository

import android.content.Context
import android.net.Uri
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.Supabase
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Uploads to the two Supabase buckets.
 *
 * PATHS ARE NOT COSMETIC. Migration 09 scopes every storage write to
 * `<business_id>/<filename>`, and refuses anything whose first path segment is
 * not a business the caller owns. Building a path any other way here does not
 * produce a misfiled object — it produces a refused upload.
 *
 *  - `photos`    public bucket, shopfront and product images
 *  - `documents` private bucket, licences and registrations. Read only through
 *                a short-lived signed URL, never a public link.
 */
object StorageRepository {

    /** Anything larger is refused before it leaves the phone. */
    private const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024

    private val ALLOWED_IMAGE = setOf("jpg", "jpeg", "png", "webp")
    private val ALLOWED_DOCUMENT = ALLOWED_IMAGE + setOf("pdf")

    /**
     * Uploads a shopfront or product photo.
     *
     * Returns the storage path, not a URL. The path is what goes in the
     * database; a URL is derived at read time, so moving buckets or turning on
     * a CDN later does not mean rewriting stored rows.
     */
    suspend fun uploadPhoto(
        context: Context,
        businessId: String,
        uri: Uri,
    ): DataResult<String> = upload(context, Supabase.BUCKET_PHOTOS, businessId, uri, ALLOWED_IMAGE)

    /** Uploads a licence or registration document to the private bucket. */
    suspend fun uploadDocument(
        context: Context,
        businessId: String,
        documentType: String,
        uri: Uri,
    ): DataResult<String> =
        upload(context, Supabase.BUCKET_DOCUMENTS, businessId, uri, ALLOWED_DOCUMENT, documentType)

    private suspend fun upload(
        context: Context,
        bucket: String,
        businessId: String,
        uri: Uri,
        allowedExtensions: Set<String>,
        prefix: String? = null,
    ): DataResult<String> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        val extension = extensionOf(context, uri)
        if (extension !in allowedExtensions) {
            return@withContext DataResult.Failure(
                "That file type is not accepted. Use " +
                    allowedExtensions.sorted().joinToString(", ") + "."
            )
        }

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
            ?: return@withContext DataResult.Failure("Could not read that file.")

        if (bytes.isEmpty()) return@withContext DataResult.Failure("That file is empty.")
        if (bytes.size > MAX_UPLOAD_BYTES) {
            return@withContext DataResult.Failure(
                "That file is " + (bytes.size / (1024 * 1024)) + " MB. The limit is 8 MB."
            )
        }

        // A random component, not the original filename. Two vendors both
        // uploading "IMG_0001.jpg" would otherwise collide, and the original
        // name can carry the person's own directory structure with it.
        val name = buildString {
            if (prefix != null) append(prefix).append('-')
            append(UUID.randomUUID().toString().take(8))
            append('.').append(extension)
        }
        val path = "$businessId/$name"

        runCatching {
            client.storage.from(bucket).upload(path, bytes)
            path
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { error ->
                android.util.Log.w("OORUVA", "upload failed for $bucket/$path", error)
                DataResult.Failure(
                    // The common causes are a dead connection and a path the
                    // caller does not own. Neither is helped by echoing a
                    // PostgREST message at the vendor.
                    "Could not upload that file. Check your connection and try again."
                )
            }
        )
    }

    /**
     * A time-limited link to a private document.
     *
     * Ten minutes. Long enough to open a PDF, short enough that a link copied
     * into a support chat stops working before it becomes a problem.
     */
    suspend fun signedDocumentUrl(path: String): DataResult<String> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.storage.from(Supabase.BUCKET_DOCUMENTS)
                .createSignedUrl(path, 10.minutes())
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure("Could not open that document.") }
        )
    }

    /** Public URL for a photo. Cheap and needs no round trip. */
    fun photoUrl(path: String): String? {
        val client = Supabase.client ?: return null
        return runCatching {
            client.storage.from(Supabase.BUCKET_PHOTOS).publicUrl(path)
        }.getOrNull()
    }

    private fun extensionOf(context: Context, uri: Uri): String {
        val fromResolver = context.contentResolver.getType(uri)
            ?.substringAfterLast('/', "")
            ?.lowercase()
            ?.let { if (it == "jpeg") "jpg" else it }

        if (!fromResolver.isNullOrBlank()) return fromResolver

        // content:// URIs often have no usable path, so this is a fallback
        // rather than the primary route.
        return uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
    }

    private fun Int.minutes() = kotlin.time.Duration.parse("${this}m")
}
