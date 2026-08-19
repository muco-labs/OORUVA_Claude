package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.CountUpNumber
import com.ooruva.app.ui.components.MucoLabsCredit
import com.ooruva.app.ui.components.PremiumButton
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Forest
import com.ooruva.app.ui.theme.ForestLight
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.OoruvaToolTheme
import com.ooruva.app.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// Vendor home
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendorHomeScreen(
    onOpenBusinessInfo: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    OoruvaToolTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                VendorTopBar(
                    eyebrow = "Vendor dashboard",
                    title = "Your business",
                    verified = false,
                    onProfile = onOpenProfile
                )
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(eyebrow = "Today", title = "Trading")
                    Spacer(Modifier.height(Spacing.md))

                    // Orders and payments do not exist yet, so there is nothing
                    // truthful to count. An empty state beats an invented number.
                    PendingCapability(
                        "Sales, orders and customer counts appear here once " +
                            "transactions are enabled on OORUVA."
                    )

                    Spacer(Modifier.height(Spacing.xl))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.lg))

                    SectionHeader(eyebrow = "Quick actions", title = "Manage")
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            item {
                VendorActionRow(Icons.Default.Edit, "Business info", "Hours, phone, description", onOpenBusinessInfo)
                VendorActionRow(Icons.Default.PhotoLibrary, "Photos", "4 uploaded", onOpenPhotos)
                VendorActionRow(Icons.Default.BarChart, "Analytics", "Seven-day trend", onOpenAnalytics)
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    Spacer(Modifier.height(Spacing.lg))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.lg))
                    SectionHeader(eyebrow = "Last seven days", title = "Footfall")
                    Spacer(Modifier.height(Spacing.md))
                    PendingCapability(
                        "Profile views and check-ins are recorded from the day " +
                            "your listing goes live."
                    )
                    Spacer(Modifier.height(Spacing.xl))
                    MucoLabsCredit(onDark = true)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Business info
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendorBusinessInfoScreen(onBackClick: () -> Unit = {}) {
    OoruvaToolTheme {
        var saved by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ToolHeader(
                    eyebrow = "Business info",
                    title = "Details",
                    status = null,
                    onBackClick = onBackClick
                )
            }
            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    // Values arrive from businesses once the vendor has
                    // registered. Until then these are genuinely unset, not
                    // populated with a sample business.
                    FieldRow("Business name", null)
                    FieldRow("Category", null)
                    FieldRow("Address", null)
                    FieldRow("Phone", null)
                    FieldRow("Opening hours", null)
                    FieldRow("Description", null)

                    Spacer(Modifier.height(Spacing.xl))

                    PremiumButton(
                        label = if (saved) "Saved" else "Save changes",
                        icon = if (saved) Icons.Default.Check else null,
                        container = if (saved) Forest else GoldBright,
                        onClick = { saved = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String?) {
    Column(Modifier.padding(vertical = Spacing.md)) {
        Text(label.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = value ?: "Not set",
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Photos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendorPhotosScreen(onBackClick: () -> Unit = {}) {
    OoruvaToolTheme {
        var mainIndex by remember { mutableIntStateOf(0) }
        val photos = remember { listOf("Storefront", "Counter", "Masala chai", "Evening queue") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ToolHeader(
                eyebrow = "Gallery",
                title = "Photos",
                status = "" + photos.size + " UPLOADED",
                onBackClick = onBackClick
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(photos.size) { index ->
                    PhotoTile(
                        label = photos[index],
                        isMain = index == mainIndex,
                        onSetMain = { mainIndex = index }
                    )
                }
                items(1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Upload",
                                tint = GoldBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "UPLOAD",
                                style = EyebrowStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(label: String, isMain: Boolean, onSetMain: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Gold.copy(alpha = 0.55f), MaterialTheme.colorScheme.background)
                )
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onSetMain)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.md)
        )
        if (isMain) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm)
                    .clip(CircleShape)
                    .background(Color(0xCC161311))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("MAIN", style = EyebrowStyle, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analytics
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendorAnalyticsScreen(onBackClick: () -> Unit = {}) {
    OoruvaToolTheme {
        var range by remember { mutableStateOf("7 days") }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ToolHeader(
                    eyebrow = "Analytics",
                    title = "Performance",
                    status = null,
                    onBackClick = onBackClick
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    listOf("7 days", "30 days", "90 days").forEach { option ->
                        val selected = range == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { range = option }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                option,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    Spacer(Modifier.height(Spacing.lg))
                    PendingCapability(
                        "Analytics need real platform events. Views, product " +
                            "opens and offer redemptions start accumulating once " +
                            "your business is verified and visible to customers."
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: String, delta: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = delta,
            style = MaterialTheme.typography.labelMedium,
            color = ForestLight
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vendor profile
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendorProfileScreen(
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    OoruvaToolTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ToolHeader(
                    eyebrow = "Vendor profile",
                    title = "Your business",
                    status = "AWAITING VERIFICATION",
                    onBackClick = onBackClick
                )
            }

            item {
                Column(Modifier.padding(horizontal = Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        VendorMetric(0, "Reviews")
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "RATING",
                                style = EyebrowStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VendorMetric(0, "Years")
                    }

                    Spacer(Modifier.height(Spacing.xl))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.lg))

                    SectionHeader(eyebrow = "Status", title = "Verification")
                    Spacer(Modifier.height(Spacing.md))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Not yet submitted",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Complete your business profile to enter the verification queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.xl))

                    PremiumButton(
                        label = "Log out",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        outlined = true,
                        container = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onLogout
                    )

                    Spacer(Modifier.height(Spacing.xl))
                    MucoLabsCredit(onDark = true)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared vendor pieces
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shown wherever a metric has no truthful source yet. OORUVA never displays an
 * invented sales, revenue or customer figure (spec 19 and 51).
 */
@Composable
fun PendingCapability(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Header for the vendor tool screens: back affordance, eyebrow, title, status. */
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
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.md, bottom = 30.dp)
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
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (status != null) {
                Spacer(Modifier.width(14.dp))
                Text(
                    text = status,
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VendorTopBar(
    eyebrow: String,
    title: String,
    verified: Boolean,
    onProfile: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .padding(top = 60.dp, bottom = Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(eyebrow.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (verified) Icons.Default.Verified else Icons.Default.Star,
                contentDescription = null,
                tint = if (verified) ForestLight else GoldBright,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = if (verified) "VERIFIED BUSINESS" else "VERIFICATION PENDING",
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VendorMetric(value: Int, label: String, prefix: String = "") {
    Column {
        CountUpNumber(
            target = value,
            prefix = prefix,
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

@Composable
private fun VendorActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = GoldBright, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}
