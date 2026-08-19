package com.ooruva.app.data.repository

import com.ooruva.app.BuildConfig
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.RewardRuleDto
import com.ooruva.app.data.remote.RewardTransactionDto
import com.ooruva.app.data.remote.Supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AwardRequest(
    val action: String = "award",
    @SerialName("activity_type") val activityType: String,
    @SerialName("reference_id") val referenceId: String? = null,
)

@Serializable
private data class RedeemRequest(
    val action: String = "redeem",
    @SerialName("offer_id") val offerId: String,
)

@Serializable
private data class AwardResponse(
    val awarded: Boolean = false,
    val points: Int = 0,
    val balance: Int? = null,
    val reason: String? = null,
)

@Serializable
private data class RedeemResponse(
    val redeemed: Boolean = false,
    val offer: String? = null,
    @SerialName("points_spent") val pointsSpent: Int = 0,
    val balance: Int? = null,
)

@Serializable
private data class RewardError(
    val error: String,
    val message: String? = null,
    val balance: Int? = null,
    val required: Int? = null,
)

/** What the app tells the customer after asking for points. */
sealed interface AwardOutcome {
    data class Awarded(val points: Int, val balance: Int?) : AwardOutcome

    /**
     * The action had already been paid for. Not an error: a retry after a
     * dropped connection lands here, and the customer's points are safe.
     */
    data object AlreadyAwarded : AwardOutcome

    data class CapReached(val message: String) : AwardOutcome
    data class Failed(val message: String) : AwardOutcome
}

sealed interface RedeemOutcome {
    data class Redeemed(val offer: String, val pointsSpent: Int, val balance: Int?) : RedeemOutcome
    data class NotEnoughPoints(val balance: Int, val required: Int) : RedeemOutcome
    data class Failed(val message: String) : RedeemOutcome
}

/**
 * Points: reading the balance, and asking the server to move it.
 *
 * Nothing here writes reward_transactions. It cannot — there is no client
 * INSERT policy on that table, by design. Every mutation goes through the
 * `rewards` edge function, which re-checks that the activity being claimed
 * actually exists and belongs to the caller before paying for it.
 *
 * A repository that could adjust a balance locally would be a repository worth
 * decompiling.
 */
object RewardRepository {

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val endpoint: String
        get() = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/rewards"

    /**
     * The signed-in customer's balance, computed by the database from the
     * ledger. Never cached locally: a stale balance shown next to a redeem
     * button is how someone taps redeem on points they no longer have.
     */
    suspend fun balance(): DataResult<Int> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")

        runCatching {
            client.postgrest.rpc("reward_balance").decodeAs<Int>()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure("Could not load your points.") }
        )
    }

    /** The published earning rates, so the app can explain what earns what. */
    suspend fun rules(): DataResult<List<RewardRuleDto>> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

        runCatching {
            client.from("reward_rules")
                .select { filter { eq("active", true) } }
                .decodeList<RewardRuleDto>()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure("Could not load the rewards list.") }
        )
    }

    /** The customer's own ledger. RLS scopes this to them. */
    suspend fun history(limit: Int = 50): DataResult<List<RewardTransactionDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            runCatching {
                client.from("reward_transactions").select {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }.decodeList<RewardTransactionDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure("Could not load your points history.") }
            )
        }

    /**
     * Asks the server to pay for something the customer just did.
     *
     * [referenceId] is the id of the review, post or check-in. The server looks
     * it up and refuses if it does not exist or belongs to someone else, so
     * passing a made-up id earns nothing.
     */
    suspend fun award(activityType: String, referenceId: String): AwardOutcome =
        withContext(Dispatchers.IO) {
            val token = Supabase.tokenProvider?.invoke()
                ?: return@withContext AwardOutcome.Failed("You need to be signed in to earn points.")

            val response: HttpResponse = runCatching {
                http.post(endpoint) {
                    header("Authorization", "Bearer $token")
                    header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    contentType(ContentType.Application.Json)
                    setBody(AwardRequest(activityType = activityType, referenceId = referenceId))
                }
            }.getOrElse {
                // Deliberately quiet. The customer's review was saved; the
                // points are a side effect, and an alarming error about them
                // would suggest the review itself failed.
                android.util.Log.w("OORUVA", "award request failed", it)
                return@withContext AwardOutcome.Failed("Points could not be added right now.")
            }

            if (response.status.value == 429) {
                val err = runCatching { response.body<RewardError>() }.getOrNull()
                return@withContext AwardOutcome.CapReached(
                    err?.message ?: "You have reached today's limit for this. It resets tomorrow."
                )
            }

            if (response.status.value !in 200..299) {
                return@withContext AwardOutcome.Failed("Points could not be added right now.")
            }

            val body = runCatching { response.body<AwardResponse>() }.getOrElse {
                return@withContext AwardOutcome.Failed("Points could not be added right now.")
            }

            when {
                body.awarded -> AwardOutcome.Awarded(body.points, body.balance)
                body.reason == "already_awarded" -> AwardOutcome.AlreadyAwarded
                else -> AwardOutcome.Failed("Points could not be added right now.")
            }
        }

    /** Spends points on an offer. */
    suspend fun redeem(offerId: String): RedeemOutcome = withContext(Dispatchers.IO) {
        val token = Supabase.tokenProvider?.invoke()
            ?: return@withContext RedeemOutcome.Failed("You need to be signed in to redeem.")

        val response: HttpResponse = runCatching {
            http.post(endpoint) {
                header("Authorization", "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(RedeemRequest(offerId = offerId))
            }
        }.getOrElse {
            android.util.Log.w("OORUVA", "redeem request failed", it)
            return@withContext RedeemOutcome.Failed(
                "Could not reach OORUVA. Your points have not been used."
            )
        }

        if (response.status.value !in 200..299) {
            val err = runCatching { response.body<RewardError>() }.getOrNull()
            return@withContext when (err?.error) {
                "insufficient_points" -> RedeemOutcome.NotEnoughPoints(
                    balance = err.balance ?: 0,
                    required = err.required ?: 0,
                )
                // The server reverses the debit before returning this, so it is
                // safe to tell the customer their points are intact.
                "redemption_failed" -> RedeemOutcome.Failed(
                    err.message ?: "That offer could not be redeemed. Your points have not been taken."
                )
                else -> RedeemOutcome.Failed("That offer could not be redeemed.")
            }
        }

        val body = runCatching { response.body<RedeemResponse>() }.getOrElse {
            return@withContext RedeemOutcome.Failed("That offer could not be redeemed.")
        }

        if (!body.redeemed) return@withContext RedeemOutcome.Failed("That offer could not be redeemed.")

        RedeemOutcome.Redeemed(
            offer = body.offer.orEmpty(),
            pointsSpent = body.pointsSpent,
            balance = body.balance,
        )
    }
}
