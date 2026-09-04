package pl.stapik.media.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors for one theme. Values are taken directly from stapik-media's
 * style-classic.css / style-modern.css / style-classic-pink.css so the
 * mobile companion stays visually in sync with the desktop app.
 */
data class RetroColorScheme(
    val shape: ThemeShape,
    val windowBackground: Color,
    val cardBackground: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val textOnHeader: Color,
    val textDark: Color,
    val textMuted: Color,
    val accent: Color,
    val borderLight: Color,
    val borderDark: Color,
    val selectedBackground: Color,
    val selectedText: Color,
)

val ClassicColors = RetroColorScheme(
    shape = ThemeShape.BEVEL,
    windowBackground = Color(0xFFC0C0C0),
    cardBackground = Color(0xFFECECEC),
    headerGradientStart = Color(0xFF000080),
    headerGradientEnd = Color(0xFF1084D0),
    textOnHeader = Color(0xFFFFFFFF),
    textDark = Color(0xFF000000),
    textMuted = Color(0xFF606060),
    accent = Color(0xFF000080),
    borderLight = Color(0xFFFFFFFF),
    borderDark = Color(0xFF808080),
    selectedBackground = Color(0xFF1010A0),
    selectedText = Color(0xFFFFFFFF),
)

val ModernColors = RetroColorScheme(
    shape = ThemeShape.FLAT,
    windowBackground = Color(0xFFF5F5F7),
    cardBackground = Color(0xFFFFFFFF),
    headerGradientStart = Color(0xFFF5F5F7),
    headerGradientEnd = Color(0xFFF5F5F7),
    textOnHeader = Color(0xFF1C1C1E),
    textDark = Color(0xFF1C1C1E),
    textMuted = Color(0xFF6E6E73),
    accent = Color(0xFF007AFF),
    borderLight = Color(0xFFE2E2E5),
    borderDark = Color(0xFFDCDCE0),
    selectedBackground = Color(0xFF007AFF),
    selectedText = Color(0xFFFFFFFF),
)

val ClassicPinkColors = RetroColorScheme(
    shape = ThemeShape.FLAT,
    windowBackground = Color(0xFFF4B6DD),
    cardBackground = Color(0xFFF9CBE6),
    headerGradientStart = Color(0xFFFF6EC7),
    headerGradientEnd = Color(0xFF7B2FF7),
    textOnHeader = Color(0xFFFFFFFF),
    textDark = Color(0xFF4A0E4E),
    textMuted = Color(0xFF8C3A72),
    accent = Color(0xFF7B2FF7),
    borderLight = Color(0xFFFFE0F3),
    borderDark = Color(0xFF8C3A72),
    selectedBackground = Color(0xFF7B2FF7),
    selectedText = Color(0xFFFFFFFF),
)

fun colorsFor(theme: AppTheme): RetroColorScheme = when (theme) {
    AppTheme.CLASSIC -> ClassicColors
    AppTheme.MODERN -> ModernColors
    AppTheme.CLASSIC_PINK -> ClassicPinkColors
}
