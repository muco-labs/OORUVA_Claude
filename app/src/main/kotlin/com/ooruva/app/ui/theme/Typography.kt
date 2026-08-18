package com.ooruva.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ooruva.app.R

/**
 * Fraunces carries identity — screen titles, business names, reward totals.
 * Manrope does everything functional. Losing the serif collapses the system
 * back to a generic app, so it is never substituted.
 *
 * Bundled rather than pulled through the Google Fonts provider: the files ship
 * in the APK, so the type is identical offline, on first launch, and on handsets
 * whose owner has set a custom system font (this one had, and it rewrote the
 * entire UI in handwriting before the fonts were bundled).
 */

val Fraunces = FontFamily(
    Font(R.font.fraunces_600, FontWeight.SemiBold),
    Font(R.font.fraunces_700, FontWeight.Bold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

val OoruvaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 18.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp
    ),
)

/** Small-caps eyebrow: section leads and timestamps. */
val EyebrowStyle = TextStyle(
    fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
    fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp
)
