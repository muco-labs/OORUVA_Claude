package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.CountUpNumber
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Forest
import com.ooruva.app.ui.theme.ForestLight
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.OoruvaToolTheme

@Composable
fun AdminDashboardScreen(onBackClick: () -> Unit = {}) {
    OoruvaToolTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ToolHeader(
                    eyebrow = "Admin",
                    title = "System",
                    status = "ALL HEALTHY",
                    onBackClick = onBackClick
                )
            }

            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    SectionHeader(eyebrow = "Key metrics", title = "Platform")
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        MetricColumn(2500, "Users", "+12% wk")
                        MetricColumn(450, "Vendors", "+4% wk")
                        MetricColumn(12500, "Check-ins", "+22% wk")
                    }

                    Spacer(Modifier.height(34.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(28.dp))

                    SectionHeader(eyebrow = "Seven days", title = "Check-in volume")
                    Spacer(Modifier.height(20.dp))
                    WeekBars(listOf(0.42f, 0.55f, 0.48f, 0.7f, 0.88f, 1f, 0.8f))

                    Spacer(Modifier.height(34.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(28.dp))

                    SectionHeader(eyebrow = "Needs a human", title = "Pending actions")
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                QueueRow("Vendor verification", 23, GoldBright)
                QueueRow("Review moderation", 8, ForestLight)
                QueueRow("Fraud signals", 2, MaterialTheme.colorScheme.error)
            }

            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(28.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(eyebrow = "Live", title = "Recent activity")
                    Spacer(Modifier.height(18.dp))

                    LogRow("New vendor signup", "Juice Corner, Anna Nagar", "5 min ago")
                    LogRow("Review reported", "Beauty Studio", "12 min ago")
                    LogRow("User check-in", "Chai Wali", "23 min ago")
                    LogRow("Payout processed", "₹4,200 to 14 vendors", "1 hr ago")
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(value: Int, label: String, delta: String) {
    Column {
        CountUpNumber(
            target = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = delta,
            style = MaterialTheme.typography.labelSmall,
            color = Forest.copy(alpha = 0.9f).let { ForestLight }
        )
    }
}

@Composable
private fun QueueRow(title: String, count: Int, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = accent
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LogRow(action: String, detail: String, ago: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = ago.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
