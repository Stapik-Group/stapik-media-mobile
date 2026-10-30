package pl.stapik.media.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.ui.sync.LoadErrorMessage
import pl.stapik.common.ui.sync.SyncStatusBanner
import pl.stapik.media.R
import pl.stapik.media.data.archive.ArchiveStatus
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.theme.RetroButton
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.util.AppInfo
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPage(
    category: MediaCategory,
    viewModel: MediaViewModel,
    scheme: RetroColorScheme,
    onEntryClick: (MediaEntry) -> Unit,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val filter = filters[category] ?: CategoryFilter.None
    val uriHandler = LocalUriHandler.current

    when (val state = uiState) {
        is MediaUiState.Loading -> Box(modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        is MediaUiState.NotConnected -> CenteredMessageWithAction(
            message = stringResource(R.string.media_error_not_connected),
            actionLabel = stringResource(R.string.media_action_connect),
            onAction = onNavigateToConnect,
            scheme = scheme,
            modifier = modifier,
        )

        is MediaUiState.Error -> CenteredMessageWithAction(
            message = state.error.toDisplayMessage(),
            actionLabel = stringResource(R.string.retry),
            onAction = { viewModel.refresh() },
            scheme = scheme,
            modifier = modifier,
        )

        is MediaUiState.LocalDataError -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadErrorMessage(kind = state.kind, onDiscardLocal = viewModel::discardLocalAndResync)
        }

        is MediaUiState.Success -> {
            val entries = remember(state.entries, category, filter, sort) {
                state.entries.filteredFor(category, filter, sort).withListKeys()
            }
            val years = state.entries.availableYearsFor(category, state.archiveYears)
            val archiveStatus = filter.year?.let { state.archiveStatus[it] }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(isPullToRefresh = true) },
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SyncStatusBanner(
                            state = syncState,
                            onAction = { action ->
                                when (action) {
                                    SyncAction.RETRY -> viewModel.refresh()
                                    SyncAction.RECONNECT, SyncAction.ENTER_PASSPHRASE -> onNavigateToConnect()
                                    SyncAction.UPDATE_APP -> uriHandler.openUri(AppInfo.RELEASES_URL)
                                    SyncAction.CHOOSE_SOURCE -> viewModel.refresh(isPullToRefresh = true)
                                    SyncAction.DISMISS -> Unit
                                }
                            },
                        )
                    }

                    if (state.isStale && state.updatedAt.isNotEmpty()) {
                        item { StaleDataBanner(updatedAt = state.updatedAt, scheme = scheme) }
                    }

                    state.refreshError?.let { error ->
                        item { NoticeBanner(text = error.toDisplayMessage(), scheme = scheme) }
                    }

                    if (state.unreadableEntries > 0) {
                        item {
                            NoticeBanner(
                                text = stringResource(R.string.media_unreadable_entries, state.unreadableEntries),
                                scheme = scheme,
                            )
                        }
                    }

                    item {
                        FilterBar(
                            category = category,
                            years = years,
                            selectedYear = filter.year,
                            selectedMonth = filter.month,
                            query = filter.query,
                            sort = sort,
                            onYearSelected = { viewModel.setYearFilter(category, it) },
                            onMonthSelected = { viewModel.setMonthFilter(category, it) },
                            onQueryChange = { viewModel.setQueryFilter(category, it) },
                            onSortChange = viewModel::setSort,
                            scheme = scheme,
                        )
                    }

                    if (archiveStatus != null && filter.year != null) {
                        item {
                            ArchiveStatusRow(
                                year = filter.year,
                                status = archiveStatus,
                                onRetry = { viewModel.requestArchive(filter.year) },
                                scheme = scheme,
                            )
                        }
                    }

                    if (entries.isEmpty() && archiveStatus == null) {
                        item { Text(text = stringResource(R.string.media_no_entries), color = scheme.textMuted) }
                    }

                    items(entries, key = { it.key }) { keyed ->
                        EntryCard(entry = keyed.entry, scheme = scheme, onClick = { onEntryClick(keyed.entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StaleDataBanner(updatedAt: String, scheme: RetroColorScheme) {
    NoticeBanner(text = stringResource(R.string.media_stale_banner, formatUpdatedAt(updatedAt)), scheme = scheme)
}

@Composable
private fun NoticeBanner(text: String, scheme: RetroColorScheme) {
    Text(
        text = text,
        color = scheme.textDark,
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.borderLight)
            .padding(8.dp),
    )
}

@Composable
private fun ArchiveStatusRow(year: Int, status: ArchiveStatus, onRetry: () -> Unit, scheme: RetroColorScheme) {
    when (status) {
        ArchiveStatus.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = scheme.accent)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = stringResource(R.string.media_archive_loading, year), color = scheme.textDark)
        }

        is ArchiveStatus.Failed -> Column {
            Text(text = stringResource(R.string.media_archive_failed, year), color = scheme.textDark)
            status.outcome?.toMediaLoadError()?.let { cause ->
                Text(text = cause.toDisplayMessage(), color = scheme.textMuted)
            }
            Spacer(modifier = Modifier.height(8.dp))
            RetroButton(text = stringResource(R.string.retry), onClick = onRetry, scheme = scheme)
        }
    }
}

@Composable
private fun FilterBar(
    category: MediaCategory,
    years: List<Int>,
    selectedYear: Int?,
    selectedMonth: Int?,
    query: String,
    sort: EntrySort,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (Int?) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (EntrySort) -> Unit,
    scheme: RetroColorScheme,
) {
    Column {
        SearchField(query, onQueryChange, scheme)
        Row(verticalAlignment = Alignment.CenterVertically) {
            YearDropdown(years, selectedYear, onYearSelected, scheme)
            if (selectedYear != null) {
                MonthDropdown(selectedMonth, onMonthSelected, scheme)
            }
            Spacer(modifier = Modifier.weight(1f))
            SortControls(category, sort, onSortChange, scheme)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, scheme: RetroColorScheme) {
    var text by rememberSaveable { mutableStateOf(query) }
    val clearButton: @Composable (() -> Unit)? = if (text.isEmpty()) {
        null
    } else {
        {
            IconButton(onClick = { text = ""; onQueryChange("") }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.media_search_clear),
                    tint = scheme.textMuted,
                )
            }
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onQueryChange(it) },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.media_search_placeholder)) },
        trailingIcon = clearButton,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = scheme.textDark,
            unfocusedTextColor = scheme.textDark,
            focusedBorderColor = scheme.accent,
            unfocusedBorderColor = scheme.borderDark,
            cursorColor = scheme.accent,
            focusedPlaceholderColor = scheme.textMuted,
            unfocusedPlaceholderColor = scheme.textMuted,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SortControls(category: MediaCategory, sort: EntrySort, onSortChange: (EntrySort) -> Unit, scheme: RetroColorScheme) {
    var expanded by remember { mutableStateOf(false) }
    val directionLabel = stringResource(if (sort.isAscending) R.string.media_sort_ascending else R.string.media_sort_descending)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(sort.field.label(category), color = scheme.accent)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(field.label(category)) },
                        onClick = { onSortChange(sort.copy(field = field)); expanded = false },
                    )
                }
            }
        }
        TextButton(
            onClick = { onSortChange(sort.withReversedDirection()) },
            modifier = Modifier.semantics { contentDescription = directionLabel },
        ) {
            Text(if (sort.isAscending) "\u2191" else "\u2193", color = scheme.accent)
        }
    }
}

@Composable
private fun SortField.label(category: MediaCategory): String = stringResource(
    when (this) {
        SortField.TITLE -> R.string.media_sort_title
        SortField.RELEASE_DATE -> R.string.media_sort_release_date
        SortField.CONSUMED -> category.consumedDateLabelResource()
    },
)

@Composable
private fun YearDropdown(
    years: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    scheme: RetroColorScheme,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedYear?.toString() ?: stringResource(R.string.media_filter_all_years), color = scheme.accent)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.media_filter_all_years)) },
                onClick = { onYearSelected(null); expanded = false },
            )
            years.forEach { year ->
                DropdownMenuItem(text = { Text(year.toString()) }, onClick = { onYearSelected(year); expanded = false })
            }
        }
    }
}

@Composable
private fun MonthDropdown(
    selectedMonth: Int?,
    onMonthSelected: (Int?) -> Unit,
    scheme: RetroColorScheme,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedMonth?.let { monthLabel(it) } ?: stringResource(R.string.media_filter_all_months)

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(label, color = scheme.accent)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.media_filter_all_months)) },
                onClick = { onMonthSelected(null); expanded = false },
            )
            (1..12).forEach { month ->
                DropdownMenuItem(text = { Text(monthLabel(month)) }, onClick = { onMonthSelected(month); expanded = false })
            }
        }
    }
}

@Composable
private fun CenteredMessageWithAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = scheme.textDark)
            Spacer(modifier = Modifier.height(12.dp))
            RetroButton(text = actionLabel, onClick = onAction, scheme = scheme)
        }
    }
}

private fun monthLabel(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }