package com.ooruva.app.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ooruva.app.R
import com.ooruva.app.ui.theme.Brand
import com.ooruva.app.ui.theme.Espresso
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Fraunces
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.Motion
import com.ooruva.app.ui.theme.ShadowWarm
import com.ooruva.app.ui.theme.pressScale
import kotlin.math.roundToInt

/** The pin mark. */
@Composable
fun OoruvaMark(size: Int = 40, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_ooruva_mark),
        contentDescription = "OORUVA",
        modifier = modifier.height(size.dp).width((size * 0.8f).dp)
    )
}

@Composable
fun OoruvaWordmark(
    fontSize: Int = 28,
    color: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "OORUVA",
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize.sp,
        letterSpacing = (fontSize * 0.07f).sp,
        color = color,
        modifier = modifier
    )
}

/**
 * Editorial screen head: gold eyebrow, serif title, generous air. No coloured
 * banner — the whitespace is what does the work.
 */
@Composable
fun EditorialHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (trailing != null) trailing()
    }
}

/** Gold stars — never the default yellow. */
@Composable
fun GoldRating(
    rating: Float,
    reviews: Int? = null,
    onDark: Boolean = false,
    compact: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            repeat(5) { index ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < rating.roundToInt()) Gold
                    else if (onDark) Color.White.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
        )
        if (reviews != null) {
            Text(
                text = "  ·  " + reviews + " reviews",
                style = MaterialTheme.typography.labelSmall,
                color = if (onDark) Color.White.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Quiet metadata pill — outlined, never filled with gold. */
@Composable
fun MetaPill(
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    border: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/**
 * Hero tile standing in for vendor photography: a restrained duotone in the
 * category's tone with a serif monogram.
 */
@Composable
fun HeroTile(
    name: String,
    category: String,
    height: Int = 190,
    corner: Int = 32,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(Brand.heroFor(category))
    ) {
        Text(
            text = name.take(1).uppercase(),
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            fontSize = (height * 0.46f).sp,
            color = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 26.dp)
        )
        Box(Modifier.matchParentSize().background(Brand.HeroScrim))
    }
}

/** Hairline card with a warm shadow and a press-scale, no ripple. */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    corner: Int = 16,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = ShadowWarm,
                spotColor = ShadowWarm
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) { content() }
}

/** Counts a number up on first composition — the one motion flourish that earns its keep. */
@Composable
fun CountUpNumber(
    target: Int,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onBackground,
    prefix: String = "",
    suffix: String = "",
    modifier: Modifier = Modifier,
) {
    var start by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(Motion.Count, easing = LinearOutSlowInEasing),
        label = "countUp"
    )
    androidx.compose.runtime.LaunchedEffect(target) { start = true }

    val shown = (target * progress).roundToInt()
    Text(
        text = prefix + formatGrouped(shown) + suffix,
        style = style,
        color = color,
        modifier = modifier
    )
}

private fun formatGrouped(value: Int): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val sb = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append(',')
    }
    return sb.reverse().toString()
}

/** Gold progress ring — the Rewards centrepiece. */
@Composable
fun GoldRing(
    progress: Float,
    size: Int = 190,
    track: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.Count, easing = LinearOutSlowInEasing),
        label = "ring"
    )
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Gold,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** Founder credit. */
@Composable
fun MucoLabsCredit(onDark: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(Gold))
        Spacer(Modifier.width(9.dp))
        Text(
            text = "FOUNDED BY MUCO LABS",
            style = EyebrowStyle,
            color = if (onDark) Color.White.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(9.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(Gold))
    }
}

/** Gold-underline text field styling is applied per screen; this is the divider it uses. */
@Composable
fun GoldUnderline(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (active) 2.dp else 1.dp)
            .background(if (active) Gold else MaterialTheme.colorScheme.outlineVariant)
    )
}

/** Espresso surface used by the two professional tools. */
@Composable
fun ToolSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Espresso)
    ) { content() }
}
