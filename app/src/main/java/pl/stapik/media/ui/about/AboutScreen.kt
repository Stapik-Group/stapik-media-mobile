package pl.stapik.media.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.ui.common.ScreenHeader
import pl.stapik.media.ui.theme.RetroButton
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.retroPanel
import pl.stapik.media.util.AppInfo

@Composable
fun AboutScreen(scheme: RetroColorScheme, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.about_title), scheme = scheme, onBack = onBack)

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .retroPanel(scheme)
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.about_version, AppInfo.VERSION_NAME), color = scheme.textDark)
            Text(stringResource(R.string.about_author), color = scheme.textMuted, modifier = Modifier.padding(top = 4.dp))
            Text(
                text = stringResource(R.string.about_description),
                color = scheme.textMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
            RetroButton(
                text = stringResource(R.string.about_source_code),
                onClick = { uriHandler.openUri("https://github.com/Stapik-Group/stapik-media-mobile") },
                scheme = scheme,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}