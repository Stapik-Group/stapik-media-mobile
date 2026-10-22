package pl.stapik.media.ui.theme

import androidx.compose.ui.graphics.Color
import pl.stapik.common.theme.Rgb
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.theme.ThemePalette
import pl.stapik.common.theme.ThemePalettes

/** The colours the app's own screens use, taken from the shared theme palette. */
data class RetroColorScheme(
    val shape: ThemeShape,
    val windowBackground: Color,
    val cardBackground: Color,
    val textOnHeader: Color,
    val textDark: Color,
    val textMuted: Color,
    val accent: Color,
    val borderLight: Color,
    val borderDark: Color,
)

private fun Rgb.color(): Color = Color(0xFF000000.toInt() or this)

fun ThemePalette.toRetroColors(): RetroColorScheme = RetroColorScheme(
    shape = if (usesBevel) ThemeShape.BEVEL else ThemeShape.FLAT,
    windowBackground = background.color(),
    cardBackground = surface.color(),
    textOnHeader = onAccent.color(),
    textDark = text.color(),
    textMuted = textMuted.color(),
    accent = accent.color(),
    borderLight = borderLight.color(),
    borderDark = borderDark.color(),
)

fun colorsFor(theme: ThemeId): RetroColorScheme = ThemePalettes.of(theme).toRetroColors()
