package com.ooruva.app.data.remote

import com.ooruva.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Single Supabase entry point.
 *
 * The keys come from local.properties via BuildConfig. When they are blank —
 * a fresh clone, or before Day 1 setup is done — [isConfigured] is false and
 * every repository falls back to mock data rather than throwing. That keeps the
 * app runnable for design work without a backend.
 *
 * AUTHENTICATION
 * The Auth plugin is deliberately not installed. OORUVA does not use Supabase's
 * own sign-in: identity comes from Firebase phone OTP, and the auth-bootstrap
 * edge function mints the Supabase session. [tokenProvider] hands that minted
 * JWT to every request, which is what makes auth.uid() resolve and therefore
 * what makes every RLS policy work. Installing Auth as well would give the
 * client a second, conflicting idea of who is signed in.
 *
 * With no token the requests still go out, carrying only the anon key. That is
 * correct and intended: public reads (verified businesses, the category
 * taxonomy) work signed-out, and everything owner-scoped fails closed.
 */
object Supabase {

    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /**
     * Set once at application start. Returns the current session's access
     * token, or null when signed out.
     *
     * A provider rather than a stored string because the token has a one-hour
     * life: caching it at client-construction time would pin the first token
     * for the whole process and start failing silently an hour in.
     */
    @Volatile
    var tokenProvider: (() -> String?)? = null

    val client: SupabaseClient? by lazy {
        if (!isConfigured) {
            android.util.Log.w(
                "OORUVA",
                "Supabase keys missing — running on mock data. See supabase/SETUP.md"
            )
            null
        } else {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                accessToken = { tokenProvider?.invoke() }
                install(Postgrest)
                install(Storage)
            }
        }
    }

    /** Buckets created by 02_rls.sql. */
    const val BUCKET_PHOTOS = "photos"
    const val BUCKET_DOCUMENTS = "documents"
}

/** Uniform result type so screens can render loading, data and failure honestly. */
sealed interface DataResult<out T> {
    data object Loading : DataResult<Nothing>
    data class Success<T>(val data: T, val fromMock: Boolean = false) : DataResult<T>
    data class Failure(val message: String) : DataResult<Nothing>
}
