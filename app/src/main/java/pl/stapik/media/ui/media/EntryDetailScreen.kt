package pl.stapik.media.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.ui.theme.RetroColorScheme
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EntryDetailScreen(
    entry: MediaEntry,
    scheme: RetroColorScheme,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(scheme.windowBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.detail_back),
                    tint = scheme.textDark,
                )
            }
            Text(entry.title, style = MaterialTheme.typography.titleLarge, color = scheme.textDark)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            DetailRow(stringResource(R.string.detail_release_date), formatReleaseDate(entry.releaseDate), scheme)
            DetailRow(
                stringResource(R.string.detail_consumed),
                stringResource(R.string.media_watched, entry.consumed.month, entry.consumed.year),
                scheme,
            )

            when (val details = entry.details) {
                is MediaDetails.Screen -> {
                    DetailRow(stringResource(R.string.detail_director), details.director, scheme)
                    DetailRow(stringResource(R.string.detail_genre), details.genre, scheme)
                }

                is MediaDetails.Book -> {
                    DetailRow(stringResource(R.string.detail_author), details.author, scheme)
                    DetailRow(stringResource(R.string.detail_genre), details.genre, scheme)
                    DetailRow(
                        stringResource(R.string.detail_audiobook),
                        stringResource(if (details.isAudiobook) R.string.detail_yes else R.string.detail_no),
                        scheme,
                    )
                }

                is MediaDetails.Album -> {
                    DetailRow(stringResource(R.string.detail_performer), details.performer, scheme)
                    DetailRow(stringResource(R.string.detail_publisher), details.publisher, scheme)
                    DetailRow(stringResource(R.string.detail_genre), details.genre, scheme)
                }

                is MediaDetails.Game -> {
                    DetailRow(stringResource(R.string.detail_studio), details.studio, scheme)
                    DetailRow(stringResource(R.string.detail_publisher), details.publisher, scheme)
                    DetailRow(stringResource(R.string.detail_platform), details.platform, scheme)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, scheme: RetroColorScheme) {
    if (value.isBlank()) return // skip empty fields (e.g. a placeholder entry with no director/genre yet)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.textMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.textDark)
    }
}

private fun formatReleaseDate(releaseDate: ReleaseDate): String {
    val date = releaseDate.date
    return when (releaseDate.precision) {
        DatePrecision.DAY -> "%02d %s %d".format(
            date.dayOfMonth, date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), date.year,
        )

        DatePrecision.MONTH -> "%s %d".format(date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), date.year)
        DatePrecision.YEAR -> date.year.toString()
    }
}