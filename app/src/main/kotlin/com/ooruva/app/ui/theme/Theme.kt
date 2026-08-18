package com.ooruva.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun OoruvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Material You is deliberately not wired up: the handset's wallpaper palette
    // would repaint the gold, which is the whole identity.
    val colorScheme = if (darkTheme) OoruvaDarkColors else OoruvaLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OoruvaTypography,
        shapes = OoruvaShapes,
        content = content
    )
}

/**
 * Forces the espresso scheme regardless of system setting — the Vendor Portal and
 * Admin Dashboard are long-session professional tools and read as such in dark.
 */
@Composable
fun OoruvaToolTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = OoruvaDarkColors,
        typography = OoruvaTypography,
        shapes = OoruvaShapes,
        content = content
    )
}
