package pl.stapik.media.ui.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.theme.RetroColorScheme

@Composable
fun MediaPagerScreen(
    viewModel: MediaViewModel,
    scheme: RetroColorScheme,
    modifier: Modifier = Modifier,
) {
    val categories = MediaCategory.entries
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val scope = rememberCoroutineScope()
    var detailEntry by remember { mutableStateOf<MediaEntry?>(null) }

    // Covers both the system back button and the edge-swipe back gesture.
    BackHandler(enabled = detailEntry != null) { detailEntry = null }

    Box(modifier = modifier.fillMaxSize().background(scheme.windowBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = scheme.headerGradientStart,
                    contentColor = scheme.textOnHeader,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(scheme.headerGradientStart, scheme.headerGradientEnd))),
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(category.displayName()) },
                        )
                    }
                }

                // Explicit "there's more" indicators - a partially-cut tab alone
                // isn't a reliable enough affordance.
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = scheme.textOnHeader)
                    }
                }
                if (pagerState.currentPage < categories.lastIndex) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = scheme.textOnHeader)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = detailEntry == null, // don't fight the detail screen's back gesture
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                CategoryPage(
                    category = categories[page],
                    viewModel = viewModel,
                    scheme = scheme,
                    onEntryClick = { detailEntry = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        detailEntry?.let { entry ->
            EntryDetailScreen(
                entry = entry,
                scheme = scheme,
                onBack = { detailEntry = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}