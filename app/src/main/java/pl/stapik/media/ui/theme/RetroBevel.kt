package pl.stapik.media.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.retroBevel(
    scheme: RetroColorScheme,
    raised: Boolean = true,
    thickness: Dp = 2.dp,
): Modifier = this.drawBehind {
    val stroke = thickness.toPx()
    val topLeftColor = if (raised) scheme.borderLight else scheme.borderDark
    val bottomRightColor = if (raised) scheme.borderDark else scheme.borderLight

    drawLine(topLeftColor, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
    drawLine(topLeftColor, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
    drawLine(bottomRightColor, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
    drawLine(bottomRightColor, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
}

fun Modifier.retroPanel(
    scheme: RetroColorScheme,
    cornerRadius: Dp = 8.dp,
    raised: Boolean = true,
): Modifier = when (scheme.shape) {
    ThemeShape.BEVEL -> this
        .background(scheme.cardBackground)
        .retroBevel(scheme, raised = raised)

    ThemeShape.FLAT -> this
        .shadow(elevation = if (raised) 2.dp else 0.dp, shape = RoundedCornerShape(cornerRadius))
        .clip(RoundedCornerShape(cornerRadius))
        .background(scheme.cardBackground)
}