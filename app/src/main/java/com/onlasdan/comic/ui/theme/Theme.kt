package com.onlasdan.comic.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NoirColorScheme = darkColorScheme(
    primary = PrimaryPink,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = AccentPink,
    secondary = AccentPink,
    onSecondary = PureBlack,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentPink,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = MutedRed,
    onError = PureBlack,
)

@Composable
fun ComicReaderTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NoirColorScheme,
        typography = NoirTypography,
        shapes = NoirShapes,
        content = content,
    )
}
