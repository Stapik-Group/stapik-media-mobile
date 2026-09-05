package pl.stapik.media.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun RetroColorScheme.toMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = textOnHeader,
    secondary = accent,
    onSecondary = textOnHeader,
    background = windowBackground,
    onBackground = textDark,
    surface = cardBackground,
    onSurface = textDark,
    surfaceVariant = cardBackground,
    onSurfaceVariant = textMuted,
    outline = borderDark,
    outlineVariant = borderLight,
    error = Color(0xFFB00020),
    onError = Color.White,
)