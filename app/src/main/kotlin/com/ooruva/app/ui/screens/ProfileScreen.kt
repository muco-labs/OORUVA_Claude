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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.MucoLabsCredit
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold

@Composable
fun ProfileScreen(
    onOpenVendorPortal: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            // Editorial: large serif name, no boxed avatar card
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            text = "MEMBER SINCE 2026",
                            style = EyebrowStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Muthu",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "+91 98765 43210",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                // Stats in a clean row, not boxed cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Stat("15", "Check-ins")
                    Stat("8", "Reviews")
                    Stat("23", "Saved")
                }

                Spacer(Modifier.height(36.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(28.dp))

                SectionHeader(eyebrow = "Your account", title = "Activity")
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            SettingRow(Icons.Default.RateReview, "My reviews", "8 written") {}
            SettingRow(Icons.Default.FavoriteBorder, "Saved vendors", "23 places") {}
            SettingRow(Icons.Default.Settings, "Settings", null) {}
            SettingRow(Icons.AutoMirrored.Filled.Help, "Help and support", null) {}
        }

        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(28.dp))
                SectionHeader(eyebrow = "Business tools", title = "Manage")
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            SettingRow(
                Icons.Default.Storefront,
                "Vendor portal",
                "Chai Wali · active",
                onOpenVendorPortal
            )
            SettingRow(
                Icons.Default.AdminPanelSettings,
                "Admin dashboard",
                "System overview",
                onOpenAdmin
            )
            SettingRow(Icons.AutoMirrored.Filled.ExitToApp, "Log out", null) {}
        }

        item {
            Spacer(Modifier.height(44.dp))
            MucoLabsCredit()
            Spacer(Modifier.height(10.dp))
            Text(
                text = "OORUVA · v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Gold,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}
