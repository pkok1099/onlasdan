package com.onlasdan.comic.ui.screens.sourcebrowse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.components.AppSearchBar
import com.onlasdan.comic.ui.components.ComicGrid
import com.onlasdan.comic.ui.components.EmptyState
import com.onlasdan.comic.ui.components.ErrorState
import com.onlasdan.comic.ui.components.PlaceholderGrid
import com.onlasdan.comic.ui.components.SecondaryButton
import com.onlasdan.comic.ui.components.SourceFilterSheet
import com.onlasdan.comic.ui.components.comicGridItems
import com.onlasdan.comic.ui.source.AppErrorHandler
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.PureBlack
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.theme.TextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layar jelajah generik di atas abstraksi ComicSource.
 * Filter, pencarian, dan paginasi semuanya digerakkan data dari source —
 * tidak ada cabang per source di sini.
 */
sealed interface SourceBrowseUiState {
    data object Loading : SourceBrowseUiState
    data class Content(val items: List<Comic>, val page: Int, val totalPages: Int?, val loadingMore: Boolean) : SourceBrowseUiState
    data class Error(val message: String) : SourceBrowseUiState
    data object Empty : SourceBrowseUiState
}

@Composable
fun SourceBrowseScreen(
    sourceId: String,
    onComicClick: (comicId: String, sourceId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFilters: Map<String, String> = emptyMap(),
) {
    val scope = rememberCoroutineScope()
    val source = remember(sourceId) { SourceRegistry.get(sourceId) }
    val declaredFilters = remember(sourceId) { source.availableFilters() }

    var query by remember(sourceId) { mutableStateOf("") }
    var appliedQuery by remember(sourceId) { mutableStateOf("") }
    var draftFilters by remember(sourceId, initialFilters) { mutableStateOf<Map<String, String>>(initialFilters) }
    var appliedFilters by remember(sourceId, initialFilters) { mutableStateOf<Map<String, String>>(initialFilters) }
    var uiState by remember(sourceId) { mutableStateOf<SourceBrowseUiState>(SourceBrowseUiState.Loading) }
    var showFilterSheet by remember(sourceId) { mutableStateOf(false) }
    var searchJob by remember(sourceId) { mutableStateOf<Job?>(null) }

    fun load(page: Int, append: Boolean) {
        scope.launch {
            if (!append) {
                uiState = SourceBrowseUiState.Loading
            } else {
                (uiState as? SourceBrowseUiState.Content)?.let {
                    uiState = it.copy(loadingMore = true)
                }
            }
            try {
                val result = source.search(
                    query = appliedQuery,
                    filters = appliedFilters,
                    page = page,
                )
                val merged = if (append) {
                    val prev = (uiState as? SourceBrowseUiState.Content)?.items.orEmpty()
                    (prev + result.items).distinctBy { it.id }
                } else {
                    result.items
                }
                uiState = if (merged.isEmpty()) {
                    SourceBrowseUiState.Empty
                } else {
                    SourceBrowseUiState.Content(
                        items = merged,
                        page = result.page ?: page,
                        totalPages = result.totalPages,
                        loadingMore = false,
                    )
                }
            } catch (e: Exception) {
                if (!append) {
                    uiState = SourceBrowseUiState.Error(AppErrorHandler.toUserMessage(e))
                } else {
                    (uiState as? SourceBrowseUiState.Content)?.let {
                        uiState = it.copy(loadingMore = false)
                    }
                }
            }
        }
    }

    fun reload() = load(page = 1, append = false)

    // Pencarian debounce 300ms.
    LaunchedEffect(query) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            if (appliedQuery != query) {
                appliedQuery = query
            }
        }
    }

    LaunchedEffect(sourceId, appliedQuery, appliedFilters) { reload() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = TextPrimary,
                    )
                }
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                // Tombol filter hanya muncul bila source mendeklarasikan filter.
                if (declaredFilters.isNotEmpty()) {
                    BadgedBox(
                        badge = {
                            if (appliedFilters.isNotEmpty()) {
                                Badge { Text(appliedFilters.size.toString()) }
                            }
                        },
                    ) {
                        IconButton(onClick = {
                            draftFilters = appliedFilters
                            showFilterSheet = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.browse_btn_filters),
                                tint = TextPrimary,
                            )
                        }
                    }
                }
            }

            AppSearchBar(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                placeholder = stringResource(R.string.browse_search_placeholder_id),
            )

            when (val state = uiState) {
                SourceBrowseUiState.Loading -> PlaceholderGrid(
                    modifier = Modifier.padding(top = Spacing.md),
                )

                is SourceBrowseUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { reload() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg),
                )

                SourceBrowseUiState.Empty -> EmptyState(
                    message = stringResource(R.string.browse_empty_result),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg),
                )

                is SourceBrowseUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    comicGridItems(
                        comics = state.items,
                        columns = 3,
                        onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                    )
                    if (state.totalPages == null || state.page < state.totalPages) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                contentAlignment = Alignment.Center,
                            ) {
                                SecondaryButton(
                                    text = stringResource(R.string.browse_btn_load_more),
                                    onClick = { load(page = state.page + 1, append = true) },
                                    enabled = !state.loadingMore,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            SourceFilterSheet(
                filters = declaredFilters,
                selected = draftFilters,
                onSelectionChange = { draftFilters = it },
                onApply = {
                    appliedFilters = draftFilters
                    showFilterSheet = false
                },
                onReset = { draftFilters = emptyMap() },
                onDismiss = { showFilterSheet = false },
            )
        }
    }
}
