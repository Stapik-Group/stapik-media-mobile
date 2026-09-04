package pl.stapik.media.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.stapik.media.R
import pl.stapik.media.data.model.MediaCategory

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