package pl.stapik.media.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.ThemeShape
import pl.stapik.media.ui.theme.retroBevel

@Composable
fun ScreenHeader(
    title: String,
    scheme: RetroColorScheme,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (scheme.shape) {
        ThemeShape.BEVEL -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(scheme.headerGradientStart, scheme.headerGradientEnd)))
                .retroBevel(scheme, raised = true)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .background(scheme.cardBackground)
                        .retroBevel(scheme, raised = true)
                        .clickable(onClick = onBack)
                        .padding(6.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.detail_back),
                        tint = scheme.textDark,
                    )
                }
            }

            Text(
                text = title,
                color = scheme.textOnHeader,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        ThemeShape.FLAT -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (onBack != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = scheme.cardBackground) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                            tint = scheme.textDark,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = scheme.accent,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = title,
                        color = scheme.textOnHeader,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}