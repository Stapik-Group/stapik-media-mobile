package pl.stapik.media.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (scheme.shape) {
        ThemeShape.BEVEL -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(scheme.cardBackground)
                .retroBevel(scheme, raised = enabled)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(text, color = if (enabled) scheme.textDark else scheme.textMuted)
        }

        ThemeShape.FLAT -> Button(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(text)
        }
    }
}
