package com.itantra.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.itantra.app.domain.model.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary              = ItantraTeal80,
    onPrimary            = DarkBackground,
    primaryContainer     = DarkSurfaceVariant,
    onPrimaryContainer   = ItantraTeal80,
    secondary            = ItantraAmber80,
    onSecondary          = DarkBackground,
    secondaryContainer   = DarkCardBackground,
    onSecondaryContainer = ItantraAmber80,
    tertiary             = ItantraIndigo80,
    onTertiary           = DarkBackground,
    tertiaryContainer    = Color(0xFF1A2850),
    onTertiaryContainer  = ItantraIndigo80,
    background           = DarkBackground,
    onBackground         = Color(0xFFE4EAF4),
    surface              = DarkSurface,
    onSurface            = Color(0xFFE4EAF4),
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFB0BCCC),
    surfaceContainerLow  = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainer     = DarkCardBackground,
    error                = StatusError,
    onError              = Color.White,
    outline              = Color(0xFF3A4F6A),
    outlineVariant       = Color(0xFF2A3D58)
)

private val LightColorScheme = lightColorScheme(
    primary              = ItantraTeal40,
    onPrimary            = Color.White,
    primaryContainer     = LightSurfaceVariant,
    onPrimaryContainer   = ItantraTeal40,
    secondary            = ItantraAmber40,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFF3D0),
    onSecondaryContainer = ItantraAmber40,
    tertiary             = ItantraIndigo40,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDDE3F9),
    onTertiaryContainer  = ItantraIndigo40,
    background           = LightBackground,
    onBackground         = Color(0xFF0D1B2A),
    surface              = LightSurface,
    onSurface            = Color(0xFF0D1B2A),
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = Color(0xFF455A6E),
    surfaceContainerLow  = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainer     = LightCardBackground,
    error                = StatusError,
    onError              = Color.White,
    outline              = Color(0xFFB0BCCC),
    outlineVariant       = Color(0xFFD0D8E4)
)

/**
 * iTantra Material 3 shape system.
 * Consistent corner radii across all component types.
 */
val ItantraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun ItantraTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.DARK   -> true
        ThemePreference.LIGHT  -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use WindowCompat to control status bar icon appearance only.
            // window.statusBarColor is deprecated on API 35+; edge-to-edge
            // display handles background colouring via the system.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ItantraTypography,
        shapes      = ItantraShapes,
        content     = content
    )
}
