package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.OoruvaToolTheme

@Composable
fun VendorPortalScreen(onBackClick: () -> Unit = {}) {
    // Professional tool: espresso by default, regardless of system setting.
    OoruvaToolTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ToolHeader(
                    eyebrow = "Vendor portal",
                    title = "Chai Wali",
                    status = "ACTIVE",
                    onBackClick = onBackClick
                )
            }

            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    SectionHeader(eyebrow = "Today", title = "Performance")
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        ToolStat(12, "Check-ins")
                        ToolStat(3, "Reviews")
                        ToolStat(45, "Profile views")
                    }

                    Spacer(Modifier.height(34.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(28.dp))

                    SectionHeader(eyebrow = "This week", title = "Trend")
                    Spacer(Modifier.height(20.dp))
                    WeekBars(listOf(0.35f, 0.5f, 0.42f, 0.68f, 0.85f, 1f, 0.72f))

                    Spacer(Modifier.height(36.dp))

                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldBright,
                            contentColor = Color(0xFF2A1E0B)
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Edit business info", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Upload photos", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolHeader(
    eyebrow: String,
    title: String,
    status: String?,
    onBackClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 30.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = eyebrow.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (status != null) {
                Spacer(Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Forest))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = status,
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ToolStat(value: Int, label: String) {
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
    }
}

/** Restrained data-viz: gold bars on a warm neutral track, nothing else. */
@Composable
fun WeekBars(values: List<Float>, labels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S")) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((92 * value).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (value >= 0.99f) GoldBright
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
