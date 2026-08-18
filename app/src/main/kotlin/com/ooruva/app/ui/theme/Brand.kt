package com.ooruva.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Buttons stay tight; cards and heroes get generous radii. */
val OoruvaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object Brand {
    /** Espresso wash — Auth hero and the two dark tools. */
    val EspressoWash = Brush.linearGradient(listOf(NightBg, NightSurface, Color(0xFF272119)))

    /** Scrim over a hero tile so type stays legible. */
    val HeroScrim = Brush.verticalGradient(
        listOf(Color.Transparent, Color(0x141C1917), Color(0xCC1C1917))
    )

    /**
     * Category tones. Restrained by design: gold, forest, brick and warm neutrals
     * only — no rainbow. Each tile is a duotone of its tone over espresso.
     */
    fun toneFor(category: String): Color = when (category.uppercase()) {
        "CHAI" -> Color(0xFF8A6329)
        "FOOD" -> Gold
        "JUICE" -> Forest
        "SALON" -> Brick
        "SHOP" -> Color(0xFF5A4E42)
        "SERVICE" -> WarmGrey
        else -> EspressoMid
    }

    fun heroFor(category: String): Brush {
        val tone = toneFor(category)
        return Brush.linearGradient(
            listOf(
                tone.copy(alpha = 0.90f),
                tone.copy(alpha = 0.55f),
                Espresso.copy(alpha = 0.88f)
            )
        )
    }
}

/** Elevation ladder — depth comes from a hairline plus a warm shadow, not blocks. */
object Elev {
    val flat = 0.dp
    val card = 1.dp
    val raised = 3.dp
    val floating = 10.dp
}

object Motion {
    const val Screen = 280
    const val Press = 120
    const val Count = 900
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f) // FastOutSlowIn
}

/**
 * Press state for premium surfaces: a 0.97 scale instead of a ripple, which
 * goes muddy over gold.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(Motion.Press, easing = Motion.Standard),
        label = "pressScale"
    )
    return this.scale(scale)
}
