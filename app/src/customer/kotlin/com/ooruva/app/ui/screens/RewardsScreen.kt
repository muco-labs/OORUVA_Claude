package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.CountUpNumber
import com.ooruva.app.ui.components.EditorialHeader
import com.ooruva.app.ui.components.GoldRing
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Forest
import com.ooruva.app.ui.theme.Gold

private const val POINTS = 2450
private const val NEXT_TIER = 3000

@Composable
fun RewardsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 108.dp)
    ) {
        item {
            EditorialHeader(
                eyebrow = "Rewards",
                title = "Your\nstanding."
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoldRing(progress = POINTS / NEXT_TIER.toFloat(), size = 230) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CountUpNumber(
                            target = POINTS,
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

                Text(
                    text = "₹245 in vouchers available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "" + (NEXT_TIER - POINTS) + " points to Gold tier",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
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

        val activity = listOf(
            Triple("Check-in at Chai Wali", "2 hours ago", 50),
            Triple("Review posted", "1 day ago", 25),
            Triple("Referred a friend", "3 days ago", 100),
            Triple("Check-in at Street Samosa", "6 days ago", 50),
        )

        items(activity.size) { index ->
            val (action, time, points) = activity[index]
            ActivityRow(action, time, points)
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
                    Text(
                        text = "Fifty for a check-in, twenty-five for a review, a hundred when a " +
                            "friend joins. Points become vouchers at the stalls you already visit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(action: String, time: String, points: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = time.uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = "+" + points,
            style = MaterialTheme.typography.titleLarge,
            color = Forest
        )
    }
}
