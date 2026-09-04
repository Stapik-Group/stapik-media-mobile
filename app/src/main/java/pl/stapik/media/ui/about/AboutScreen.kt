package pl.stapik.media.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.util.AppInfo

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(stringResource(R.string.about_title))
        Text(stringResource(R.string.about_version, AppInfo.VERSION_NAME))
        Text(stringResource(R.string.about_description))
    }
}