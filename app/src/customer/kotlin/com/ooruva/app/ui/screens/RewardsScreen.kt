package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.RewardRuleDto
import com.ooruva.app.data.remote.RewardTransactionDto
import com.ooruva.app.data.remote.Supabase
import com.ooruva.app.data.repository.RewardRepository
import com.ooruva.app.ui.components.EditorialHeader
import com.ooruva.app.ui.components.GoldRing
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle

/**
 * What this screen knows about the customer's points.
 *
 * The balance is whatever the database computed from the ledger. There is no
 * local arithmetic anywhere in this file: a balance the app worked out for
 * itself would eventually disagree with the one the server will enforce at
 * redemption, and the customer would be told they cannot afford something the
 * screen just said they could.
 */
private data class RewardsState(
    val balance: Int? = null,
    val rules: List<RewardRuleDto> = emptyList(),
    val history: List<RewardTransactionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val connected: Boolean = false,
)

/**
 * The ring fills towards the cheapest thing worth having. Without a target the
 * ring is decoration; with one it answers "how close am I?".
 */
private const val NEXT_REWARD_TARGET = 100

@Composable
fun RewardsScreen() {
    var state by remember { mutableStateOf(RewardsState()) }

    LaunchedEffect(Unit) {
        if (!Supabase.isConfigured) {
            state = state.copy(loading = false, connected = false)
            return@LaunchedEffect
        }

        val balance = when (val result = RewardRepository.balance()) {
            is DataResult.Success -> result.data
            else -> null
        }
        val rules = when (val result = RewardRepository.rules()) {
            is DataResult.Success -> result.data
            else -> emptyList()
        }
        val history = when (val result = RewardRepository.history()) {
            is DataResult.Success -> result.data
            else -> emptyList()
        }

        state = state.copy(
            balance = balance,
            rules = rules.sortedByDescending { it.points },
            history = history,
            loading = false,
            connected = true,
            error = if (balance == null) "Could not load your points. Pull down to retry." else null,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 108.dp)
    ) {
        item {
            EditorialHeader(eyebrow = "Rewards", title = "Your\nstanding.")
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val balance = state.balance ?: 0

                GoldRing(
                    progress = (balance.toFloat() / NEXT_REWARD_TARGET).coerceIn(0f, 1f),
                    size = 230
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            // A dash while loading, not a zero. A zero that
                            // turns into 340 a second later reads as though
                            // points were just found, or just lost.
                            text = if (state.loading) "—" else balance.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "POINTS",
                            style = EyebrowStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                when {
                    state.loading -> Unit

                    !state.connected -> Text(
                        text = "Points are not available on this build.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.error != null -> Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    balance == 0 -> {
                        Text(
                            text = "No points yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Check in at a stall, write a review or post a discovery. " +
                                "Points are credited after verification.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Text(
                        text = if (balance >= NEXT_REWARD_TARGET) "Enough to redeem."
                        else "" + (NEXT_REWARD_TARGET - balance) + " more to your next reward.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(28.dp))

                OutlinedButton(
                    onClick = { /* Offer picker opens here once offers are listed. */ },
                    // Enabled only when there is something to spend. A live
                    // button that answers "you have no points" is a worse
                    // answer than a button that is plainly not ready.
                    enabled = state.connected && (state.balance ?: 0) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Redeem points", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(36.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(28.dp))
                SectionHeader(eyebrow = "Ledger", title = "Recent activity")
                Spacer(Modifier.height(20.dp))
            }
        }

        if (state.history.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Nothing here yet. Your earned and redeemed points " +
                            "will be listed as they happen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.history.size) { index ->
                LedgerRow(state.history[index])
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "HOW POINTS WORK",
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))

                    // Read from reward_rules rather than written out here. The
                    // prose this replaced promised twenty-five points for a
                    // review while the database paid ten -- the kind of drift
                    // that is invisible until a customer counts.
                    if (state.rules.isEmpty()) {
                        Text(
                            text = "Earning rates are published in the app once you are online.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.rules.forEach { rule ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = rule.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "" + rule.points,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(transaction: RewardTransactionDto) {
    val isCredit = transaction.direction == "credit"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.note ?: transaction.activityType.replace('_', ' ')
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Status is shown for anything not yet settled. A pending credit
            // that looked identical to a credited one would have the customer
            // counting points they cannot spend.
            if (transaction.status != "credited") {
                Text(
                    text = transaction.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = (if (isCredit) "+" else "−") + transaction.points,
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.status != "credited") {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else if (isCredit) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}
