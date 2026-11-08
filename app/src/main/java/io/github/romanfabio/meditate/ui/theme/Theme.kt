package io.github.romanfabio.meditate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3C79B),
    onPrimary = Color(0xFF2A2112),
    primaryContainer = Color(0xFF4A3D26),
    onPrimaryContainer = Color(0xFFF6E3C0),
    secondary = Color(0xFF9AA3E0),
    onSecondary = Color(0xFF1B2050),
    secondaryContainer = Color(0xFF2B3070),
    onSecondaryContainer = Color(0xFFDDE0FF),
    background = Color(0xFF14172B),
    onBackground = Color(0xFFE8E6F0),
    surface = Color(0xFF14172B),
    onSurface = Color(0xFFE8E6F0),
    surfaceVariant = Color(0xFF232849),
    onSurfaceVariant = Color(0xFFB9BCD6),
    surfaceContainer = Color(0xFF1B1F3A),
    outline = Color(0xFF6C7096),
    outlineVariant = Color(0xFF383D66),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4A8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DBFF),
    onPrimaryContainer = Color(0xFF241552),
    secondary = Color(0xFF8A6A33),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E2BC),
    onSecondaryContainer = Color(0xFF2E1F00),
    background = Color(0xFFF6F1E8),
    onBackground = Color(0xFF1F1B2A),
    surface = Color(0xFFF6F1E8),
    onSurface = Color(0xFF1F1B2A),
    surfaceVariant = Color(0xFFE9E2D3),
    onSurfaceVariant = Color(0xFF4B4658),
    surfaceContainer = Color(0xFFEFE8DA),
    outline = Color(0xFF7C7688),
    outlineVariant = Color(0xFFCDC5B6),
)

@Composable
fun MeditateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
