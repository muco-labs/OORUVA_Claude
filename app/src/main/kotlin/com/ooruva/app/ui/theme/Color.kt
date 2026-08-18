package com.ooruva.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand colors
val Primary = Color(0xFF6200EE)
val PrimaryDark = Color(0xFF3700B3)
val Secondary = Color(0xFF03DAC6)
val SecondaryDark = Color(0xFF018786)
val Tertiary = Color(0xFF03DAC6)
val Background = Color(0xFFFFFBFE)
val Surface = Color(0xFFFFFBFE)
val Error = Color(0xFFB3261E)

val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEF7F0),
    onSecondaryContainer = Color(0xFF004D4A),
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA4EADF),
    onTertiaryContainer = Color(0xFF002019),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Background,
    onBackground = Color(0xFF1C1B1F),
    surface = Surface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC7D0),
    scrim = Color.Black,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF370B1E),
    primaryContainer = Color(0xFF6200EE),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xFF003D38),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFA4EADF),
    tertiary = Color(0xFF7FD8BE),
    onTertiary = Color(0xFF003D38),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFFA4EADF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE7E1E6),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE7E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC7D0),
    outline = Color(0xFF95919B),
    outlineVariant = Color(0xFF49454E),
    scrim = Color.Black,
)
