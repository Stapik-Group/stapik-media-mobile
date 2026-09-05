package pl.stapik.media.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun RetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    when (scheme.shape) {
        ThemeShape.BEVEL -> Column(modifier = modifier) {
            Text(label, color = scheme.textMuted, style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(scheme.cardBackground)
                    .retroBevel(scheme, raised = false)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    textStyle = TextStyle(color = scheme.textDark),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.weight(1f),
                )
                trailingContent?.invoke()
            }
        }

        ThemeShape.FLAT -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            trailingIcon = trailingContent,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.accent),
            modifier = modifier,
        )
    }
}