package com.ooruva.app.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.ShadowWarm
import com.ooruva.app.ui.theme.pressScale

data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * The floating bar.
 *
 * One gold capsule marks where you are. Moving between destinations does not
 * jump it: the outgoing item contracts while the incoming one expands on the
 * same spring, so the gold reads as a single mass flowing along the bar. The
 * label unspools out of its icon rather than fading in place.
 *
 * The bar itself sits inset from every edge on a translucent surface, so the
 * ivory (or espresso) ground shows through and it reads as resting above the
 * content rather than welded to the bottom of the screen.
 */
@Composable
fun FloatingNavBar(
    destinations: List<NavDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val surface = if (dark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = ShadowWarm,
                    spotColor = ShadowWarm
                )
                .clip(CircleShape)
                .background(surface)
                // A single light edge along the top sells the glass without a
                // real backdrop blur, which Android cannot do below API 31.
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.06f else 0.55f),
                        0.5f to Color.Transparent
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            destinations.forEach { destination ->
                NavItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.route) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val motion = rememberMotionScale()

    // The capsule grows on arrival and shrinks on departure. Because both run
    // on the same spring, the gold appears to travel rather than blink across.
    // Medium bounce gives a visible overshoot as it settles, so the movement
    // registers at a glance rather than being felt only subconsciously.
    val horizontal by animateDpAsState(
        targetValue = if (selected) 22.dp else 9.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "capsulePad"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) (-6).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconLift"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.22f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    // The capsule itself breathes vertically on arrival, which reads as the
    // gold landing rather than simply appearing.
    val capsuleHeight by animateDpAsState(
        targetValue = if (selected) 46.dp else 38.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "capsuleHeight"
    )
    val glow by animateDpAsState(
        targetValue = if (selected) 10.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "capsuleGlow"
    )

    val activeTint = if (MaterialTheme.colorScheme.background.luminanceIsDark()) GoldBright else Gold
    val tint by animateColorAsState(
        targetValue = if (selected) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = (220 * motion).toInt().coerceAtLeast(1)),
        label = "iconTint"
    )
    val capsule by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(durationMillis = (260 * motion).toInt().coerceAtLeast(1)),
        label = "capsule"
    )

    Row(
        modifier = Modifier
            .pressScale(interaction)
            .height(capsuleHeight)
            // A gold-tinted shadow only under the active item, so the selected
            // destination sits proud of the bar rather than merely tinted.
            .shadow(
                elevation = glow,
                shape = CircleShape,
                ambientColor = activeTint,
                spotColor = activeTint
            )
            .clip(CircleShape)
            .background(capsule)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = tint,
            modifier = Modifier
                .padding(top = lift)
                .size(21.dp)
                .scale(scale)
        )

        // The label unspools out of the icon rather than appearing in place.
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween((220 * motion).toInt().coerceAtLeast(1))) +
                slideInHorizontally(
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { -it },
            exit = fadeOut(tween((110 * motion).toInt().coerceAtLeast(1))) +
                slideOutHorizontally { -it }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTint
                )
            }
        }
    }
}

/**
 * Honours the system animation setting. When someone has turned animations off
 * — for motion sensitivity or on a slow handset — durations collapse to near
 * zero rather than the UI ignoring the preference.
 */
@Composable
private fun rememberMotionScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f).coerceIn(0f, 1f)
    }
}

/** Cheap luminance test so the bar can pick its own glass treatment. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299 * red + 0.587 * green + 0.114 * blue) < 0.5

/**
 * Vendor destinations. Business info stays a drill-down from Home rather than a
 * fifth tab: it is edited occasionally, not visited constantly.
 */
val vendorNavDestinations = listOf(
    NavDestination("vendor_home", "Home", Icons.Default.Home),
    NavDestination("vendor_photos", "Photos", Icons.Default.PhotoLibrary),
    NavDestination("vendor_analytics", "Insights", Icons.Default.BarChart),
    NavDestination("vendor_profile", "You", Icons.Default.Person),
)

/** Customer destinations. Group Finder lives inside Home, not on the bar. */
val customerNavDestinations = listOf(
    NavDestination("customer_home", "Home", Icons.Default.Home),
    NavDestination("map", "Map", Icons.Default.Map),
    NavDestination(
        "community", "Feed",
        Icons.AutoMirrored.Filled.Chat
    ),
    NavDestination(
        "rewards", "Rewards",
        Icons.Default.CardGiftcard
    ),
    NavDestination("customer_profile", "You", Icons.Default.Person),
)
