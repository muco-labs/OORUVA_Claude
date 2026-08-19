package com.ooruva.app.data.repository

import com.ooruva.app.data.remote.BusinessDocumentDto
import com.ooruva.app.data.remote.BusinessDto
import com.ooruva.app.data.remote.CatalogueItemDto
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.NearbyBusinessDto
import com.ooruva.app.data.remote.Supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NearbyArgs(
    @SerialName("origin_lat") val originLat: Double,
    @SerialName("origin_lng") val originLng: Double,
    @SerialName("radius_km") val radiusKm: Double,
    @SerialName("type_filter") val typeFilter: String? = null,
    @SerialName("max_results") val maxResults: Int = 100,
)

/**
 * Businesses, on the `businesses` model rather than the older
 * `vendor_profiles`.
 *
 * Visibility is not enforced here. Every query below relies on the biz_read
 * policy, which shows a customer verified listings only, a vendor their own
 * drafts as well, and an admin everything. Re-checking status in Kotlin would
 * duplicate that rule in a place no test covers and where it could drift.
 */
object BusinessRepository {

    /** Discovery: what is open near this point, nearest first. */
    suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusKm: Double = 5.0,
        businessTypeId: String? = null,
    ): DataResult<List<NearbyBusinessDto>> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

        runCatching {
            client.postgrest.rpc(
                function = "nearby_businesses",
                parameters = NearbyArgs(lat, lng, radiusKm, businessTypeId),
            ).decodeList<NearbyBusinessDto>()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not load nearby businesses") }
        )
    }

    /**
     * Text search across visible businesses.
     *
     * `ilike` is adequate at launch scale and needs no extra extension. It will
     * not survive a large catalogue — the replacement is a tsvector column plus
     * a GIN index, which is a migration rather than a rewrite of this method.
     */
    suspend fun search(query: String, limit: Int = 50): DataResult<List<BusinessDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            runCatching {
                client.from("businesses").select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("description", "%$query%")
                        }
                    }
                    order("name", Order.ASCENDING)
                    limit(limit.toLong())
                }.decodeList<BusinessDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not search") }
            )
        }

    suspend fun byId(businessId: String): DataResult<BusinessDto> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.from("businesses")
                .select { filter { eq("id", businessId) } }
                .decodeSingle<BusinessDto>()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not load this business") }
        )
    }

    /**
     * Every business belonging to the signed-in vendor, drafts included.
     *
     * Takes no vendor id: biz_read already scopes this to the caller, and
     * accepting one would invite a caller to pass someone else's and assume it
     * worked because no error came back.
     */
    suspend fun mine(): DataResult<List<BusinessDto>> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.from("businesses")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<BusinessDto>()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not load your businesses") }
        )
    }

    /**
     * Creates or updates a draft. Called after every onboarding step so a
     * vendor on a patchy connection never loses what they typed.
     */
    suspend fun saveDraft(business: BusinessDto): DataResult<BusinessDto> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Failure("Backend not configured")

            runCatching {
                client.from("businesses")
                    .upsert(business) { select() }
                    .decodeSingle<BusinessDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not save. Your changes are still on this device.") }
            )
        }

    /**
     * Hands a completed draft to the verification queue.
     *
     * Sets status to 'submitted' only. It deliberately cannot set 'verified' —
     * RLS would allow the owner to write that column, so the restraint has to
     * live somewhere, and a vendor approving their own listing is exactly what
     * the queue exists to prevent. The 02 suite asserts the database refuses it
     * too, so this is defence in depth rather than the only guard.
     */
    suspend fun submitForVerification(businessId: String): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Failure("Backend not configured")

            runCatching {
                client.from("businesses").update(
                    mapOf(
                        "status" to "submitted",
                        "submitted_at" to nowIso(),
                    )
                ) { filter { eq("id", businessId) } }
                Unit
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not submit for verification") }
            )
        }

    // == Catalogue ============================================================

    suspend fun catalogue(businessId: String): DataResult<List<CatalogueItemDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            runCatching {
                client.from("products").select {
                    filter { eq("business_id", businessId) }
                    order("sort_order", Order.ASCENDING)
                }.decodeList<CatalogueItemDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not load the catalogue") }
            )
        }

    suspend fun saveItem(item: CatalogueItemDto): DataResult<Unit> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.from("products").upsert(item)
            Unit
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not save that item") }
        )
    }

    suspend fun deleteItem(itemId: String): DataResult<Unit> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.from("products").delete { filter { eq("id", itemId) } }
            Unit
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not remove that item") }
        )
    }

    // == Documents ============================================================

    /**
     * Records a submitted document. The file itself goes to the private
     * `documents` bucket; only its path is stored here, never a public URL.
     */
    suspend fun saveDocument(document: BusinessDocumentDto): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Failure("Backend not configured")

            runCatching {
                client.from("business_documents").upsert(document)
                Unit
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not record that document") }
            )
        }

    suspend fun documents(businessId: String): DataResult<List<BusinessDocumentDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            runCatching {
                client.from("business_documents")
                    .select { filter { eq("business_id", businessId) } }
                    .decodeList<BusinessDocumentDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not load documents") }
            )
        }

    private fun nowIso(): String =
        java.time.Instant.now().toString()
}
