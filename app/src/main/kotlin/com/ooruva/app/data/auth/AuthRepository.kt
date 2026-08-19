package com.ooruva.app.data.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.ooruva.app.BuildConfig
import com.ooruva.app.data.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** What the caller needs to know after asking for a code. */
sealed interface OtpRequest {
    /** A code is on its way; pass [verificationId] back with the digits. */
    data class Sent(val verificationId: String) : OtpRequest

    /**
     * Android auto-read the SMS, or Play Integrity vouched for the device, so
     * there is nothing for the person to type. The screen should skip straight
     * to the dashboard rather than showing an empty code field.
     */
    data class AutoVerified(val session: OoruvaSession) : OtpRequest

    data class Failed(val message: String) : OtpRequest
}

sealed interface AuthResult {
    data class Success(val session: OoruvaSession) : AuthResult

    /**
     * The number is registered to the other app. Worth its own case: telling
     * someone "wrong code" when the real problem is "you are a customer trying
     * to sign into the vendor app" sends them round the loop forever.
     */
    data class WrongApp(val registeredRole: String, val message: String) : AuthResult

    data class Failed(val message: String) : AuthResult
}

@Serializable
private data class BootstrapRequest(val role: String)

@Serializable
private data class BootstrapResponse(
    @SerialName("user_id") val userId: String,
    val role: String,
    val phone: String,
    val created: Boolean = false,
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
private data class BootstrapError(
    val error: String,
    @SerialName("registered_role") val registeredRole: String? = null,
    val message: String? = null,
)

/**
 * Phone sign-in, end to end.
 *
 * Firebase proves the number. The auth-bootstrap edge function turns that proof
 * into an OORUVA identity and a Supabase session — it is the only thing that
 * may create a user row, because the anon key is public and a self-inserted row
 * could claim role = 'admin'.
 *
 * When Firebase is not configured ([BuildConfig.HAS_FIREBASE] is false, i.e. no
 * google-services.json) every entry point returns [AuthResult.Failed] with a
 * message saying so. It does not quietly pretend to sign someone in: a fake
 * session would produce a dashboard with no data and no explanation, which is
 * worse than an honest refusal.
 */
class AuthRepository(private val sessionStore: SessionStore) {

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val firebase: FirebaseAuth? by lazy {
        if (BuildConfig.HAS_FIREBASE) runCatching { FirebaseAuth.getInstance() }.getOrNull()
        else null
    }

    val isConfigured: Boolean
        get() = BuildConfig.HAS_FIREBASE &&
            firebase != null &&
            BuildConfig.SUPABASE_URL.isNotBlank()

    fun currentSession(): OoruvaSession? = sessionStore.load()?.takeIf { it.isValid }

    fun signOut() {
        runCatching { firebase?.signOut() }
        sessionStore.clear()
    }

    /**
     * Asks Firebase to send a code to +91[phone].
     *
     * [activity] is required by Firebase for the reCAPTCHA fallback it shows
     * when Play Integrity cannot vouch for the device.
     */
    suspend fun requestOtp(
        activity: Activity,
        phone: String,
        role: UserRole,
    ): OtpRequest {
        val auth = firebase ?: return OtpRequest.Failed(NOT_CONFIGURED)

        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Instant verification: no SMS was needed. Finish the whole
                    // exchange here so the caller never shows a code field.
                    CoroutineScope(Dispatchers.IO).launch {
                        when (val result = exchange(credential, role)) {
                            is AuthResult.Success ->
                                if (cont.isActive) cont.resume(OtpRequest.AutoVerified(result.session))
                            is AuthResult.WrongApp ->
                                if (cont.isActive) cont.resume(OtpRequest.Failed(result.message))
                            is AuthResult.Failed ->
                                if (cont.isActive) cont.resume(OtpRequest.Failed(result.message))
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    if (cont.isActive) cont.resume(OtpRequest.Failed(readable(e)))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    if (cont.isActive) cont.resume(OtpRequest.Sent(verificationId))
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phone")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    /** Verifies the typed code and exchanges it for an OORUVA session. */
    suspend fun verifyOtp(
        verificationId: String,
        code: String,
        role: UserRole,
    ): AuthResult {
        if (firebase == null) return AuthResult.Failed(NOT_CONFIGURED)
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        return exchange(credential, role)
    }

    /**
     * Firebase credential -> Firebase ID token -> OORUVA session.
     *
     * The ID token never reaches Postgres. It is proof for the edge function,
     * which issues the session Postgres will actually accept.
     */
    private suspend fun exchange(
        credential: PhoneAuthCredential,
        role: UserRole,
    ): AuthResult = withContext(Dispatchers.IO) {
        val auth = firebase ?: return@withContext AuthResult.Failed(NOT_CONFIGURED)

        val idToken = runCatching {
            val signIn = auth.signInWithCredential(credential).awaitResult()
            signIn.user?.getIdToken(false)?.awaitResult()?.token
        }.getOrElse { e ->
            return@withContext AuthResult.Failed(readable(e))
        } ?: return@withContext AuthResult.Failed("Could not confirm that code. Try again.")

        bootstrap(idToken, role)
    }

    private suspend fun bootstrap(idToken: String, role: UserRole): AuthResult {
        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/auth-bootstrap"

        val response: HttpResponse = runCatching {
            http.post(endpoint) {
                header("Authorization", "Bearer $idToken")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(BootstrapRequest(role.name.lowercase()))
            }
        }.getOrElse { e ->
            return AuthResult.Failed(
                "Could not reach OORUVA. Check your connection and try again."
                    .also { android.util.Log.w("OORUVA", "bootstrap transport failed", e) }
            )
        }

        if (response.status == HttpStatusCode.Conflict) {
            val err = runCatching { response.body<BootstrapError>() }.getOrNull()
            return AuthResult.WrongApp(
                registeredRole = err?.registeredRole ?: "another role",
                message = err?.message ?: "This number is registered to a different OORUVA app.",
            )
        }

        if (!response.status.isSuccess()) {
            val err = runCatching { response.body<BootstrapError>() }.getOrNull()
            return AuthResult.Failed(
                when (err?.error) {
                    "Account suspended" ->
                        "This account has been suspended. Contact OORUVA support."
                    else -> "Sign-in failed. Please try again."
                }
            )
        }

        val body = runCatching { response.body<BootstrapResponse>() }.getOrElse {
            return AuthResult.Failed("Sign-in failed. Please try again.")
        }

        val session = OoruvaSession(
            userId = body.userId,
            role = if (body.role.equals("vendor", true)) UserRole.VENDOR else UserRole.CUSTOMER,
            phone = body.phone,
            accessToken = body.accessToken,
            expiresAt = System.currentTimeMillis() + body.expiresIn * 1000,
        )
        sessionStore.save(session)
        return AuthResult.Success(session)
    }

    /**
     * Firebase messages are written for developers. These are the cases a real
     * person actually hits, in words that tell them what to do next.
     */
    private fun readable(e: Throwable): String = when {
        e.message?.contains("INVALID_CODE", true) == true ||
            e.message?.contains("invalid verification code", true) == true ->
            "That code was not right. Check the message and try again."

        e.message?.contains("SESSION_EXPIRED", true) == true ->
            "That code has expired. Ask for a new one."

        e.message?.contains("TOO_MANY_REQUESTS", true) == true ||
            e.message?.contains("quota", true) == true ->
            "Too many attempts from this number. Wait a few minutes and try again."

        e.message?.contains("INVALID_PHONE_NUMBER", true) == true ->
            "That does not look like a valid mobile number."

        e.message?.contains("network", true) == true ->
            "No connection. The code cannot be sent until you are back online."

        else -> "Something went wrong sending the code. Please try again."
    }

    private companion object {
        const val NOT_CONFIGURED =
            "Phone sign-in is not configured on this build. " +
                "See supabase/SETUP.md — google-services.json is missing."
    }
}

private fun HttpStatusCode.isSuccess() = value in 200..299
