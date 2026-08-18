package com.ooruva.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Quiet Luxury — warm editorial palette. Deep espresso, aged gold, forest green.
 * Gold is precious: text, icons, borders and CTAs only, never a background fill,
 * and never more than roughly a tenth of a screen.
 */

// Light — daylight surfaces (Home, Map, forms, detail)
val Ivory = Color(0xFFFAF7F1)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceElevated = Color(0xFFFFFDFA)
val Espresso = Color(0xFF1C1917)
val EspressoMid = Color(0xFF4A4038)
val WarmGrey = Color(0xFF6B6259)
val OutlineWarm = Color(0xFFE4DDD1)

val Gold = Color(0xFFB8863B)
val GoldBright = Color(0xFFD4A85C)
val GoldContainer = Color(0xFFF3E6CC)
val GoldDeep = Color(0xFF8A6329)

val Forest = Color(0xFF2F4A3C)
val ForestLight = Color(0xFF5C8770)
val ForestContainer = Color(0xFFDCE7E0)

val Brick = Color(0xFFA23B2E)
val BrickContainer = Color(0xFFF2DED9)

// Dark — the professional tools (Vendor Portal, Admin) and night mode
val NightBg = Color(0xFF161311)
val NightSurface = Color(0xFF1F1B18)
val NightElevated = Color(0xFF2A2521)
val NightOutline = Color(0xFF3A3430)
val NightOnBg = Color(0xFFF3EEE7)
val GoldContainerDark = Color(0xFF3D3020)

/** Warm shadow — espresso at 10%, never pure black. */
val ShadowWarm = Color(0x1A1C1917)

val OoruvaLightColors = lightColorScheme(
    primary = Gold,
    onPrimary = Color.White,
    primaryContainer = GoldContainer,
    onPrimaryContainer = Color(0xFF4A3312),
    inversePrimary = GoldBright,

    secondary = Forest,
    onSecondary = Color.White,
    secondaryContainer = ForestContainer,
    onSecondaryContainer = Color(0xFF16261E),

    tertiary = EspressoMid,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE6DC),
    onTertiaryContainer = Espresso,

    error = Brick,
    onError = Color.White,
    errorContainer = BrickContainer,
    onErrorContainer = Color(0xFF44120C),

    background = Ivory,
    onBackground = Espresso,
    surface = SurfaceWhite,
    onSurface = Espresso,
    surfaceVariant = Color(0xFFF1EBE1),
    onSurfaceVariant = WarmGrey,
    surfaceContainer = SurfaceElevated,
    surfaceTint = Gold,
    inverseSurface = Espresso,
    inverseOnSurface = Ivory,

    outline = Color(0xFFCFC5B6),
    outlineVariant = OutlineWarm,
    scrim = Color(0x661C1917),
)

val OoruvaDarkColors = darkColorScheme(
    primary = GoldBright,
    onPrimary = Color(0xFF2A1E0B),
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = Color(0xFFF3E6CC),
    inversePrimary = Gold,

    secondary = ForestLight,
    onSecondary = Color(0xFF10201A),
    secondaryContainer = Color(0xFF25392F),
    onSecondaryContainer = ForestContainer,

    tertiary = Color(0xFFD8CEC1),
    onTertiary = Espresso,
    tertiaryContainer = NightElevated,
    onTertiaryContainer = NightOnBg,

    error = Color(0xFFD9897C),
    onError = Color(0xFF3A0F09),
    errorContainer = Color(0xFF5C1E15),
    onErrorContainer = BrickContainer,

    background = NightBg,
    onBackground = NightOnBg,
    surface = NightSurface,
    onSurface = NightOnBg,
    surfaceVariant = NightElevated,
    onSurfaceVariant = Color(0xFFB6ABA0),
    surfaceContainer = NightElevated,
    surfaceTint = GoldBright,
    inverseSurface = Ivory,
    inverseOnSurface = Espresso,

    outline = Color(0xFF56504A),
    outlineVariant = NightOutline,
    scrim = Color(0xB3000000),
)
