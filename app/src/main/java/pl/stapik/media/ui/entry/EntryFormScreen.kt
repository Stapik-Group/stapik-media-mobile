package pl.stapik.media.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import pl.stapik.media.R
import pl.stapik.media.data.library.WriteRefusal
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.media.consumedFormLabelResource
import pl.stapik.media.ui.theme.RetroButton
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.retroTextFieldColors
import pl.stapik.media.util.foldForSearch
import pl.stapik.media.util.monthName

private const val MAX_SUGGESTIONS = 6
private const val YEAR_DIGITS = 4
private const val LEAP_YEAR = 2000
private const val MAX_RATING = 10

@Composable
fun EntryFormScreen(
    initial: EntryForm,
    suggestions: EntrySuggestions.Lists,
    isNew: Boolean,
    busy: Boolean,
    refusal: WriteRefusal?,
    failureMessage: String?,
    scheme: RetroColorScheme,
    onSave: (MediaEntry) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var form by rememberSaveable(initial.id, initial.category, stateSaver = entryFormSaver(initial)) { mutableStateOf(initial) }
    var problems by remember { mutableStateOf(emptySet<FormProblem>()) }

    BackHandler(onBack = onCancel)

    Column(modifier = modifier.fillMaxSize().background(scheme.windowBackground)) {
        FormHeader(isNew, scheme, onCancel)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormTextField(
                label = stringResource(R.string.entry_title_label),
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                scheme = scheme,
                error = if (FormProblem.TITLE_EMPTY in problems) stringResource(R.string.entry_error_empty_title) else null,
            )
            ReleaseDateSection(form, { form = it }, problems, scheme)
            ConsumedSection(form, { form = it }, problems, scheme)
            DetailsSection(form, { form = it }, suggestions, scheme)
            ChoiceField(
                label = stringResource(R.string.detail_rating),
                value = form.rating?.toString() ?: stringResource(R.string.entry_rating_none),
                choices = listOf(stringResource(R.string.entry_rating_none)) + (1..MAX_RATING).map(Int::toString),
                onChoose = { form = form.copy(rating = it.takeIf { index -> index > 0 }) },
                scheme = scheme,
            )
            FormTextField(
                label = stringResource(R.string.detail_note),
                value = form.note,
                onValueChange = { form = form.copy(note = it) },
                scheme = scheme,
                singleLine = false,
            )
            refusal?.let { Text(text = refusalMessage(it, form), color = MaterialTheme.colorScheme.error) }
            failureMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            RetroButton(text = stringResource(R.string.entry_cancel), onClick = onCancel, scheme = scheme)
            RetroButton(
                text = stringResource(R.string.entry_save),
                onClick = {
                    when (val result = form.toEntry()) {
                        is FormResult.Ready -> {
                            problems = emptySet()
                            onSave(result.entry)
                        }
                        is FormResult.Invalid -> problems = result.problems
                    }
                },
                scheme = scheme,
                enabled = !busy,
            )
        }
    }
}

@Composable
private fun FormHeader(isNew: Boolean, scheme: RetroColorScheme, onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back), tint = scheme.textDark)
        }
        Text(
            text = stringResource(if (isNew) R.string.entry_add else R.string.entry_edit),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.textDark,
        )
    }
}

@Composable
private fun ReleaseDateSection(form: EntryForm, onChange: (EntryForm) -> Unit, problems: Set<FormProblem>, scheme: RetroColorScheme) {
    val year = form.releaseYear.toIntOrNull()?.takeIf { it in EntryForm.RELEASE_YEARS } ?: LEAP_YEAR
    val daysInMonth = YearMonth.of(year, form.releaseMonth).lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.detail_release_date), style = MaterialTheme.typography.labelMedium, color = scheme.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePrecision.entries.forEach { precision ->
                FilterChip(
                    selected = form.releasePrecision == precision,
                    onClick = { onChange(form.copy(releasePrecision = precision)) },
                    label = { Text(stringResource(precision.labelResource())) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            if (form.releasePrecision == DatePrecision.DAY) {
                ChoiceField(
                    label = stringResource(R.string.entry_precision_day),
                    value = minOf(form.releaseDay, daysInMonth).toString(),
                    choices = (1..daysInMonth).map(Int::toString),
                    onChoose = { onChange(form.copy(releaseDay = it + 1)) },
                    scheme = scheme,
                )
            }
            if (form.releasePrecision != DatePrecision.YEAR) {
                ChoiceField(
                    label = stringResource(R.string.entry_precision_month),
                    value = monthName(form.releaseMonth),
                    choices = (1..12).map(::monthName),
                    onChoose = { onChange(form.copy(releaseMonth = it + 1)) },
                    scheme = scheme,
                )
            }
            YearField(
                label = stringResource(R.string.entry_precision_year),
                value = form.releaseYear,
                onValueChange = { onChange(form.copy(releaseYear = it)) },
                error = if (FormProblem.RELEASE_YEAR_INVALID in problems) stringResource(R.string.entry_error_release_year) else null,
                scheme = scheme,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConsumedSection(form: EntryForm, onChange: (EntryForm) -> Unit, problems: Set<FormProblem>, scheme: RetroColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(form.category.consumedFormLabelResource()),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.textMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            ChoiceField(
                label = stringResource(R.string.entry_precision_month),
                value = monthName(form.consumedMonth),
                choices = (1..12).map(::monthName),
                onChoose = { onChange(form.copy(consumedMonth = it + 1)) },
                scheme = scheme,
            )
            YearField(
                label = stringResource(R.string.entry_precision_year),
                value = form.consumedYear,
                onValueChange = { onChange(form.copy(consumedYear = it)) },
                error = if (FormProblem.CONSUMED_YEAR_INVALID in problems) stringResource(R.string.entry_error_consumed_year) else null,
                scheme = scheme,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailsSection(form: EntryForm, onChange: (EntryForm) -> Unit, suggestions: EntrySuggestions.Lists, scheme: RetroColorScheme) {
    val creatorLabel = when (form.category) {
        MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> R.string.detail_director
        MediaCategory.BOOK -> R.string.detail_author
        MediaCategory.ALBUM -> R.string.detail_performer
        MediaCategory.GAME -> R.string.detail_studio
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SuggestedTextField(stringResource(creatorLabel), form.creator, { onChange(form.copy(creator = it)) }, suggestions.creators, scheme)
        if (form.category == MediaCategory.ALBUM || form.category == MediaCategory.GAME) {
            SuggestedTextField(
                stringResource(R.string.detail_publisher), form.publisher, { onChange(form.copy(publisher = it)) }, suggestions.publishers, scheme,
            )
        }
        if (form.category == MediaCategory.GAME) {
            SuggestedTextField(
                stringResource(R.string.detail_platform), form.platform, { onChange(form.copy(platform = it)) }, suggestions.platforms, scheme,
            )
        } else {
            SuggestedTextField(stringResource(R.string.detail_genre), form.genre, { onChange(form.copy(genre = it)) }, suggestions.genres, scheme)
        }
        if (form.category == MediaCategory.BOOK) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = form.isAudiobook, onCheckedChange = { onChange(form.copy(isAudiobook = it)) })
                Text(stringResource(R.string.detail_audiobook), color = scheme.textDark)
            }
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocusChange: (Boolean) -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Sentences),
        colors = retroTextFieldColors(scheme),
        modifier = modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
    )
}

@Composable
private fun YearField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
) {
    FormTextField(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(YEAR_DIGITS)) },
        scheme = scheme,
        modifier = modifier,
        error = error,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun SuggestedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    scheme: RetroColorScheme,
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        FormTextField(label, value, onValueChange, scheme, onFocusChange = { focused = it })
        if (focused) {
            SuggestionRow(options, value, onValueChange)
        }
    }
}

@Composable
private fun SuggestionRow(options: List<String>, value: String, onPick: (String) -> Unit) {
    val typed = value.trim()
    val query = foldForSearch(typed)
    val matches = options
        .filter { it != typed && (query.isEmpty() || foldForSearch(it).contains(query)) }
        .take(MAX_SUGGESTIONS)
    if (matches.isEmpty()) return
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        matches.forEach { option -> SuggestionChip(onClick = { onPick(option) }, label = { Text(option) }) }
    }
}

@Composable
private fun ChoiceField(
    label: String,
    value: String,
    choices: List<String>,
    onChoose: (Int) -> Unit,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.textMuted)
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = scheme.accent) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEachIndexed { index, choice ->
                    DropdownMenuItem(text = { Text(choice) }, onClick = { onChoose(index); expanded = false })
                }
            }
        }
    }
}

private fun DatePrecision.labelResource(): Int = when (this) {
    DatePrecision.DAY -> R.string.entry_precision_day
    DatePrecision.MONTH -> R.string.entry_precision_month
    DatePrecision.YEAR -> R.string.entry_precision_year
}

@Composable
private fun refusalMessage(refusal: WriteRefusal, form: EntryForm): String = when (refusal) {
    WriteRefusal.Duplicate -> stringResource(R.string.entry_error_duplicate, form.title.trim(), form.releaseYear.trim())
    WriteRefusal.UnknownEntry -> stringResource(R.string.entry_error_missing)
    is WriteRefusal.ArchiveNotLoaded -> stringResource(R.string.entry_error_archive_not_loaded)
    is WriteRefusal.Invalid -> stringResource(R.string.error_unknown, refusal.problem.name)
}
