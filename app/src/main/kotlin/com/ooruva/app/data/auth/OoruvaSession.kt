package com.ooruva.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.ooruva.app.data.models.UserRole

/**
 * The signed-in identity, as OORUVA understands it.
 *
 * Firebase proves the phone number; this is what the platform decided that
 * number is. [accessToken] is the Supabase session minted by the auth-bootstrap
 * edge function — every PostgREST and Storage call carries it, and every RLS
 * policy reads [userId] out of it as auth.uid().
 */
data class OoruvaSession(
    val userId: String,
    val role: UserRole,
    val phone: String,
    val accessToken: String,
    /** Epoch millis. The token is refused by PostgREST after this. */
    val expiresAt: Long,
) {
    /**
     * Treated as expired a minute early, so a call started just under the wire
     * does not arrive just over it.
     */
    val isValid: Boolean
        get() = System.currentTimeMillis() < expiresAt - 60_000
}

/**
 * Persists the session across launches.
 *
 * Stored in app-private SharedPreferences, which the OS sandboxes per
 * application. That is adequate for a token with a one-hour life: the realistic
 * threat is a rooted device, and on a rooted device the encryption key held by
 * the same app is reachable too. It is recorded here rather than left implicit
 * because the trade-off should be revisited if the TTL ever grows or a refresh
 * token is introduced — at that point EncryptedSharedPreferences earns its keep.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): OoruvaSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val roleName = prefs.getString(KEY_ROLE, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return null

        return OoruvaSession(
            userId = userId,
            role = role,
            phone = prefs.getString(KEY_PHONE, "").orEmpty(),
            accessToken = token,
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L),
        )
    }

    fun save(session: OoruvaSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ROLE, session.role.name)
            .putString(KEY_PHONE, session.phone)
            .putString(KEY_TOKEN, session.accessToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAt)
            .apply()
    }

    /** Signing out has to leave nothing behind, including on a shared handset. */
    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE = "ooruva_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
        const val KEY_PHONE = "phone"
        const val KEY_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
