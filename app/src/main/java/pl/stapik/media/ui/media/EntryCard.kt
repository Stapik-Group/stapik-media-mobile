package pl.stapik.media.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.retroPanel

@Composable
fun EntryCard(
    entry: MediaEntry,
    scheme: RetroColorScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .retroPanel(scheme)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(text = entry.title, color = scheme.textDark, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.media_watched, entry.consumed.month, entry.consumed.year),
            color = scheme.textMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}