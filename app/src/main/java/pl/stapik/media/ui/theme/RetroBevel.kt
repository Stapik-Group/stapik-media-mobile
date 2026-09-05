package pl.stapik.media.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.retroPanel(
    scheme: RetroColorScheme,
    cornerRadius: Dp = 8.dp,
): Modifier = when (scheme.shape) {
    ThemeShape.BEVEL -> this
        .background(scheme.cardBackground, RectangleShape)
        .border(2.dp, scheme.borderDark, RectangleShape) // bottom/right (dark)
        .border(2.dp, scheme.borderLight, RectangleShape) // approximated top/left (light)

    ThemeShape.FLAT -> this
        .clip(RoundedCornerShape(cornerRadius))
        .background(scheme.cardBackground)
        .border(1.dp, scheme.borderLight, RoundedCornerShape(cornerRadius))
}
