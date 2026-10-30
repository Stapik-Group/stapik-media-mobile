package pl.stapik.media.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.stapik.media.R
import pl.stapik.media.data.model.MediaCategory

fun MediaCategory.consumedLineResource(): Int = when (this) {
    MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> R.string.media_consumed_watched
    MediaCategory.BOOK -> R.string.media_consumed_read
    MediaCategory.ALBUM -> R.string.media_consumed_listened
    MediaCategory.GAME -> R.string.media_consumed_completed
}

fun MediaCategory.consumedDateLabelResource(): Int = when (this) {
    MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> R.string.media_sort_consumed_watched
    MediaCategory.BOOK -> R.string.media_sort_consumed_read
    MediaCategory.ALBUM -> R.string.media_sort_consumed_listened
    MediaCategory.GAME -> R.string.media_sort_consumed_completed
}

fun MediaCategory.consumedFormLabelResource(): Int = when (this) {
    MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> R.string.entry_consumed_watched
    MediaCategory.BOOK -> R.string.entry_consumed_read
    MediaCategory.ALBUM -> R.string.entry_consumed_listened
    MediaCategory.GAME -> R.string.entry_consumed_completed
}

@Composable
fun MediaCategory.displayName(): String = stringResource(
    when (this) {
        MediaCategory.MOVIE -> R.string.category_movie
        MediaCategory.SERIES -> R.string.category_series
        MediaCategory.CARTOON -> R.string.category_cartoon
        MediaCategory.BOOK -> R.string.category_book
        MediaCategory.ALBUM -> R.string.category_album
        MediaCategory.GAME -> R.string.category_game
    },
)