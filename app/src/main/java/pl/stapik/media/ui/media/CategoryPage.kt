package pl.stapik.media.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.theme.RetroColorScheme
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
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val filter = filters[category] ?: CategoryFilter.None

    when (val state = uiState) {
        is MediaUiState.Loading -> Box(modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        is MediaUiState.NotConnected -> Box(modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.media_error_not_connected),
                color = scheme.textDark,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is MediaUiState.Error -> Box(modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.connect_error_prefix, state.message),
                color = scheme.textDark,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        is MediaUiState.Success -> {
            val entries = state.entries.filteredFor(category, filter)
            val years = state.entries.availableYearsFor(category)

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
                    if (state.isStale) {
                        item { StaleDataBanner(updatedAt = state.updatedAt, scheme = scheme) }
                    }

                    item {
                        FilterBar(
                            years = years,
                            selectedYear = filter.year,
                            selectedMonth = filter.month,
                            onYearSelected = { viewModel.setYearFilter(category, it) },
                            onMonthSelected = { viewModel.setMonthFilter(category, it) },
                            scheme = scheme,
                        )
                    }

                    if (entries.isEmpty()) {
                        item { Text(text = stringResource(R.string.media_no_entries), color = scheme.textMuted) }
                    }

                    items(entries, key = { it.title + it.consumed.year + it.consumed.month }) { entry ->
                        EntryCard(entry = entry, scheme = scheme, onClick = { onEntryClick(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StaleDataBanner(updatedAt: String, scheme: RetroColorScheme) {
    Text(
        text = stringResource(R.string.media_stale_banner, formatUpdatedAt(updatedAt)),
        color = scheme.textDark,
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.borderLight)
            .padding(8.dp),
    )
}

@Composable
private fun FilterBar(
    years: List<Int>,
    selectedYear: Int?,
    selectedMonth: Int?,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (Int?) -> Unit,
    scheme: RetroColorScheme,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        YearDropdown(years, selectedYear, onYearSelected, scheme)
        if (selectedYear != null) {
            MonthDropdown(selectedMonth, onMonthSelected, scheme)
        }
    }
}

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

private fun monthLabel(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }