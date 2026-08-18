package com.ooruva.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.ShadowWarm
import com.ooruva.app.ui.theme.pressScale

/**
 * The one button in the app. Press-scale instead of ripple (ripple muddies on
 * gold), a warm shadow, and a loading state that keeps the footprint stable.
 */
@Composable
fun PremiumButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    container: Color = Gold,
    content: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    val active = enabled && !loading

    val bg = when {
        outlined -> Color.Transparent
        active -> container
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    }
    val fg = when {
        outlined && active -> container
        active -> content
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (outlined) Modifier
                else Modifier.shadow(
                    elevation = if (active) 8.dp else 0.dp,
                    shape = shape,
                    ambientColor = ShadowWarm,
                    spotColor = ShadowWarm
                )
            )
            .clip(shape)
            .background(bg)
            .then(
                if (outlined) Modifier.border(
                    BorderStroke(1.dp, if (active) container else MaterialTheme.colorScheme.outlineVariant),
                    shape
                ) else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = fg
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}
