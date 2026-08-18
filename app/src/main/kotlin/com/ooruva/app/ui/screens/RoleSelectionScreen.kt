package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.ui.components.MucoLabsCredit
import com.ooruva.app.ui.components.OoruvaMark
import com.ooruva.app.ui.theme.Brand
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.NightOnBg
import com.ooruva.app.ui.theme.Spacing
import com.ooruva.app.ui.theme.pressScale

@Composable
fun RoleSelectionScreen(onRoleSelected: (UserRole) -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.EspressoWash)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(88.dp))

            OoruvaMark(size = 72)

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "Ooruva.",
                style = MaterialTheme.typography.displayLarge,
                color = NightOnBg
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "The street food of your neighbourhood, kept properly. Tell us which side of the counter you are on.",
                style = MaterialTheme.typography.bodyLarge,
                color = NightOnBg.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(Spacing.xxl))

            RoleCard(
                icon = Icons.Default.TravelExplore,
                eyebrow = "For eating",
                title = "I'm a customer",
                body = "Find stalls near you, split a budget across a group, collect points.",
                onClick = { onRoleSelected(UserRole.CUSTOMER) }
            )

            Spacer(Modifier.height(Spacing.md))

            RoleCard(
                icon = Icons.Default.Storefront,
                eyebrow = "For trading",
                title = "I'm a vendor",
                body = "List your stall, keep your hours current, watch your footfall.",
                onClick = { onRoleSelected(UserRole.VENDOR) }
            )

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "You can switch later from your profile.",
                style = MaterialTheme.typography.bodySmall,
                color = NightOnBg.copy(alpha = 0.35f)
            )

            Spacer(Modifier.weight(1f))
            MucoLabsCredit(onDark = true)
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(1.dp, Gold.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = eyebrow.uppercase(), style = EyebrowStyle, color = Gold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = NightOnBg
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = NightOnBg.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = NightOnBg.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}
