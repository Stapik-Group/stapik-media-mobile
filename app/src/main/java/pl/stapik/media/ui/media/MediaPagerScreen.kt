package pl.stapik.media.ui.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import pl.stapik.media.R
import pl.stapik.media.data.library.WriteRefusal
import pl.stapik.media.data.library.WriteResult
import pl.stapik.media.ui.entry.EditorTarget
import pl.stapik.media.ui.entry.EditorTargetSaver
import pl.stapik.media.ui.entry.EntryForm
import pl.stapik.media.ui.entry.EntryFormScreen
import pl.stapik.media.ui.entry.EntrySuggestions
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.ThemeShape
import pl.stapik.media.ui.theme.retroBevel

@Composable
fun MediaPagerScreen(
    viewModel: MediaViewModel,
    scheme: RetroColorScheme,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = MediaCategory.entries
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val scope = rememberCoroutineScope()
    var detailEntry by rememberSaveable(stateSaver = MediaEntryStateSaver) { mutableStateOf<MediaEntry?>(null) }
    var editor by rememberSaveable(stateSaver = EditorTargetSaver) { mutableStateOf<EditorTarget?>(null) }
    var saving by remember { mutableStateOf(false) }
    var refusal by remember { mutableStateOf<WriteRefusal?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    val loaded = uiState as? MediaUiState.Success
    val canEdit = loaded?.canEdit == true
    val addedText = stringResource(R.string.entry_added)
    val updatedText = stringResource(R.string.entry_updated)
    val deletedText = stringResource(R.string.entry_deleted)
    val undoLabel = stringResource(R.string.entry_undo)
    val saveFailedText = stringResource(R.string.entry_error_save_failed)

    val announce: (String, suspend () -> Unit) -> Unit = { message, undo ->
        scope.launch {
            val outcome = snackbars.showSnackbar(message = message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (outcome == SnackbarResult.ActionPerformed) {
                try {
                    undo()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbars.showSnackbar(saveFailedText)
                }
            }
        }
    }

    val save: (EditorTarget, MediaEntry) -> Unit = { target, entry ->
        scope.launch {
            saving = true
            refusal = null
            saveFailed = false
            try {
                val result = when (target) {
                    is EditorTarget.New -> viewModel.addEntry(entry)
                    is EditorTarget.Edit -> viewModel.updateEntry(target.entry.id, entry)
                }
                when (result) {
                    is WriteResult.Done -> {
                        editor = null
                        when (target) {
                            is EditorTarget.New -> announce(addedText) { viewModel.deleteEntry(result.entry.id) }
                            is EditorTarget.Edit -> {
                                detailEntry = result.entry
                                announce(updatedText) { viewModel.updateEntry(target.entry.id, target.entry) }
                            }
                        }
                    }
                    is WriteResult.Refused -> refusal = result.refusal
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }

    val delete: (MediaEntry) -> Unit = { entry ->
        scope.launch {
            try {
                val result = viewModel.deleteEntry(entry.id)
                if (result is WriteResult.Done) {
                    detailEntry = null
                    announce(deletedText) { viewModel.addEntry(result.entry) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbars.showSnackbar(saveFailedText)
            }
        }
    }

    // Covers both the system back button and the edge-swipe back gesture.
    BackHandler(enabled = detailEntry != null) { detailEntry = null }

    Box(modifier = modifier.fillMaxSize().background(scheme.windowBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                when (scheme.shape) {
                    ThemeShape.BEVEL -> Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.windowBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                    ) {
                        categories.forEachIndexed { index, category ->
                            val selected = pagerState.currentPage == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(if (selected) scheme.cardBackground else scheme.windowBackground)
                                    .retroBevel(scheme, raised = true)
                                    .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = category.displayName(),
                                    color = scheme.textDark,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }

                    ThemeShape.FLAT -> ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = scheme.windowBackground,
                        contentColor = scheme.textDark,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        categories.forEachIndexed { index, category ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(category.displayName()) },
                            )
                        }
                    }
                }

                // Explicit "there's more" indicators - a partially-cut tab alone
                // isn't a reliable enough affordance.
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = scheme.textDark)
                    }
                }
                if (pagerState.currentPage < categories.lastIndex) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = scheme.textDark)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = detailEntry == null && editor == null, // don't fight the back gesture of the overlays
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                CategoryPage(
                    category = categories[page],
                    viewModel = viewModel,
                    scheme = scheme,
                    onEntryClick = { detailEntry = it },
                    onNavigateToConnect = onNavigateToConnect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (canEdit && detailEntry == null && editor == null) {
            SmallFloatingActionButton(
                onClick = {
                    refusal = null
                    saveFailed = false
                    editor = EditorTarget.New(categories[pagerState.currentPage])
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.entry_add))
            }
        }

        detailEntry?.let { entry ->
            EntryDetailScreen(
                entry = entry,
                scheme = scheme,
                canEdit = canEdit,
                onBack = { detailEntry = null },
                onEdit = {
                    refusal = null
                    saveFailed = false
                    editor = EditorTarget.Edit(entry)
                },
                onDelete = { delete(entry) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        editor?.let { target ->
            val category = when (target) {
                is EditorTarget.New -> target.category
                is EditorTarget.Edit -> target.entry.category
            }
            val initial = remember(target) {
                when (target) {
                    is EditorTarget.New -> EntryForm.blank(target.category, LocalDate.now())
                    is EditorTarget.Edit -> EntryForm.from(target.entry)
                }
            }
            val suggestions = remember(loaded?.entries, category) { EntrySuggestions.collect(loaded?.entries.orEmpty(), category) }
            EntryFormScreen(
                initial = initial,
                suggestions = suggestions,
                isNew = target is EditorTarget.New,
                busy = saving,
                refusal = refusal,
                failureMessage = if (saveFailed) saveFailedText else null,
                scheme = scheme,
                onSave = { entry -> save(target, entry) },
                onCancel = { editor = null },
                modifier = Modifier.fillMaxSize(),
            )
        }

        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}
