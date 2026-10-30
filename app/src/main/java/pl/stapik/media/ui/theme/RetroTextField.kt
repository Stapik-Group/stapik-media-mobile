package pl.stapik.media.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun retroTextFieldColors(scheme: RetroColorScheme): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = scheme.textDark,
    unfocusedTextColor = scheme.textDark,
    focusedBorderColor = scheme.accent,
    unfocusedBorderColor = scheme.borderDark,
    cursorColor = scheme.accent,
    focusedLabelColor = scheme.accent,
    unfocusedLabelColor = scheme.textMuted,
    focusedPlaceholderColor = scheme.textMuted,
    unfocusedPlaceholderColor = scheme.textMuted,
)
