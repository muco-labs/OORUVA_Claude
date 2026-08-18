package com.ooruva.app.data.remote

import com.ooruva.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
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
 */
object Supabase {

    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

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
                install(Postgrest)
                install(Storage)
                install(Auth)
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
