package pl.stapik.media.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.ui.common.ScreenHeader

private val options = listOf(
    AppTheme.CLASSIC to R.string.theme_classic,
    AppTheme.MODERN to R.string.theme_modern,
    AppTheme.CLASSIC_PINK to R.string.theme_classic_pink,
)

@Composable
fun ThemeScreen(
    currentTheme: AppTheme,
    scheme: RetroColorScheme,
    onSelect: (AppTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.settings_theme_label), scheme = scheme, onBack = onBack)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            options.forEach { (theme, labelRes) ->
                ThemeOptionRow(
                    label = stringResource(labelRes),
                    selected = theme == currentTheme,
                    scheme = scheme,
                    onClick = { onSelect(theme) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, scheme: RetroColorScheme, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) scheme.selectedBackground else scheme.cardBackground,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) scheme.selectedText else scheme.textDark,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(20.dp),
        )
    }
}