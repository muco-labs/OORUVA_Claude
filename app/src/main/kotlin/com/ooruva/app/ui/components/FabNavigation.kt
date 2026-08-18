package com.ooruva.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.Motion
import com.ooruva.app.ui.theme.ShadowWarm
import com.ooruva.app.ui.theme.pressScale

data class FabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Replaces the bottom bar. A single gold FAB expands into the six customer
 * destinations, each with its own label, so the chrome stays out of the way of
 * the content until it is wanted.
 */
@Composable
fun FabNavigationGroup(
    destinations: List<FabDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(Motion.Screen, easing = Motion.Standard),
        label = "fabRotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim so the sheet of options reads over any content
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(Motion.Screen)),
            exit = fadeOut(tween(Motion.Screen))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = false }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.End
        ) {
            destinations.reversed().forEachIndexed { index, destination ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(160, delayMillis = index * 30)) +
                        slideInVertically(tween(220, delayMillis = index * 30)) { it / 2 } +
                        scaleIn(tween(220, delayMillis = index * 30), initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f)
                ) {
                    MiniFab(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        onClick = {
                            expanded = false
                            onNavigate(destination.route)
                        }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .pressScale(interaction)
                    .size(60.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = ShadowWarm,
                        spotColor = ShadowWarm
                    )
                    .clip(CircleShape)
                    .background(Gold)
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun MiniFab(
    destination: FabDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = ShadowWarm,
                    spotColor = ShadowWarm
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Gold else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = ShadowWarm,
                    spotColor = ShadowWarm
                )
                .clip(CircleShape)
                .background(
                    if (selected) Gold else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

val customerDestinations = listOf(
    FabDestination("customer_home", "Home", Icons.Default.Home),
    FabDestination("group_finder", "Group finder", Icons.Default.Groups),
    FabDestination("map", "Map", Icons.Default.Map),
    FabDestination("community", "Community", Icons.AutoMirrored.Filled.Chat),
    FabDestination("rewards", "Rewards", Icons.Default.CardGiftcard),
    FabDestination("customer_profile", "Profile", Icons.Default.Person),
)
