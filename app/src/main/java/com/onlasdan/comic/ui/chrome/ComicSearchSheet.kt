package com.onlasdan.comic.ui.chrome

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.components.*
import com.onlasdan.comic.ui.components.comicGridItems
import com.onlasdan.comic.ui.source.CombinedSearchResult
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.noirClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocalSearchItem(
    val title: String,
    val slug: String,
    val source: String,
    val subtitle: String,
    val isHistory: Boolean,
)

@Composable
fun ComicSearchSheet(
    source: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectComic: (comicId: String, sourceId: String) -> Unit,
    anchorXFraction: Float = 0.15f,
) {
    NoirMorphingSheet(
        visible = visible,
        onDismiss = onDismiss,
        anchorXFraction = anchorXFraction,
        maxHeight = 480.dp,
    ) {
        ShimmerScope {
            ComicSearchSheetContent(
                source = source,
                onDismiss = onDismiss,
                onSelectComic = onSelectComic,
            )
        }
    }
}

@Composable
fun ComicSearchSheetContent(
    source: String,
    onDismiss: () -> Unit,
    onSelectComic: (comicId: String, sourceId: String) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Dismiss keyboard otomatis saat list di-scroll oleh user
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val scrollDismissConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f && source == NestedScrollSource.UserInput) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var localResults by remember { mutableStateOf<List<LocalSearchItem>>(emptyList()) }
    var remoteResults by remember { mutableStateOf<List<Comic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Filter source: null = semua sumber (gabungan), selain itu satu source + filternya.
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var draftFilters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeFilters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Pagination hasil remote.
    var remotePage by remember { mutableIntStateOf(1) }
    var remoteHasMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshFailed by remember { mutableStateOf(false) }
    var searchRetry by remember { mutableIntStateOf(0) }
    var loadMoreJob by remember { mutableStateOf<Job?>(null) }

    val sources = remember { SourceRegistry.sources }
    val selectedSource = remember(selectedSourceId) {
        selectedSourceId?.let { SourceRegistry.get(it) }
    }
    val declaredFilters = remember(selectedSourceId) {
        selectedSource?.availableFilters().orEmpty()
    }
    val activeFilterCount = activeFilters.size

    suspend fun searchLocal(q: String): List<LocalSearchItem> {
        if (q.length < 2) return emptyList()
        val like = "%${q.trim()}%"
        val db = ComicDatabase.get(context)
        return withContext(Dispatchers.IO) {
            val bookmarks = db.bookmarkDao().searchByTitle(like, limit = 8).map {
                LocalSearchItem(
                    title = it.title,
                    slug = it.comicSlug,
                    source = it.source,
                    subtitle = "Tersimpan di Bookmark",
                    isHistory = false,
                )
            }
            val history = db.historyDao().searchByTitle(like, limit = 8).map {
                LocalSearchItem(
                    title = it.title,
                    slug = it.comicSlug,
                    source = it.source,
                    subtitle = "Riwayat Baca",
                    isHistory = true,
                )
            }
            (bookmarks + history).distinctBy { "${it.source}:${it.slug}" }.take(8)
        }
    }

    /**
     * Cari remote halaman [page]. Mode gabungan (semua source) atau satu source
     * dengan filter — digerakkan data, tanpa cabang per source di sini.
     * @return Pair(hasil, masihAdaHalamanBerikutnya)
     */
    suspend fun searchRemote(
        q: String,
        page: Int,
        sourceId: String?,
        filters: Map<String, String>,
    ): Pair<List<Comic>, Boolean>? {
        val srcId = sourceId
        if (srcId == null) {
            return when (val result = SourceRegistry.combinedSearch(query = q, page = page)) {
                is CombinedSearchResult.Success -> result.items to result.items.isNotEmpty()
                CombinedSearchResult.TotalFailure -> null
            }
        }
        val src = SourceRegistry.get(srcId)
        return try {
            val paged = src.search(query = q, filters = filters, page = page)
            paged.items to paged.hasNextPage
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun loadMore() {
        loadMoreJob?.cancel()
        loadMoreJob = scope.launch {
            loadingMore = true
            try {
                val (items, hasMore) = searchRemote(
                    debouncedQuery,
                    remotePage + 1,
                    selectedSourceId,
                    activeFilters,
                ) ?: return@launch
                remoteResults = (remoteResults + items).distinctBy { it.id to it.sourceId }
                remotePage += 1
                remoteHasMore = hasMore
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Hasil yang sudah ada dipertahankan.
            } finally {
                loadingMore = false
            }
        }
    }

    // Tahap 1: debounce — query mentah menjadi debouncedQuery setelah 300ms tenang.
    LaunchedEffect(query) {
        delay(300)
        val q = query.trim()
        if (debouncedQuery != q) debouncedQuery = q
    }

    // Tahap 2: search berjalan saat debouncedQuery, source, atau filter berubah.
    // Structured concurrency: search lama otomatis dibatalkan saat input baru tiba.
    // CancellationException tidak pernah ditelan — job basi tidak bisa menghapus hasil baru.
    LaunchedEffect(debouncedQuery, selectedSourceId, activeFilters, searchRetry) {
        loadMoreJob?.cancel()
        val q = debouncedQuery
        val srcId = selectedSourceId
        val filters = activeFilters
        if (q.length < 2) {
            localResults = emptyList()
            remoteResults = emptyList()
            isLoading = false
            remoteHasMore = false
            refreshFailed = false
        } else {
            isLoading = true
            try {
                // Lokal & remote berpisah nasib: gagal satu sisi tidak menghapus hasil sisi lain.
                try {
                    val local = searchLocal(q)
                    localResults = if (srcId == null) {
                        local
                    } else {
                        local.filter { it.source == srcId }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Hasil lokal lama dipertahankan.
                }
                val outcome = searchRemote(q, 1, srcId, filters)
                if (outcome != null) {
                    remoteResults = outcome.first
                    remotePage = 1
                    remoteHasMore = outcome.second
                    refreshFailed = false
                } else {
                    refreshFailed = true
                }
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nestedScroll(scrollDismissConnection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                    },
                )
            }
            .padding(horizontal = Spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.search_hint_all),
                onSearch = {
                    focusManager.clearFocus()
                    if (query.isNotBlank()) {
                        debouncedQuery = query.trim()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            // Tombol filter: hanya bila satu source dipilih dan punya filter.
            if (selectedSourceId != null && declaredFilters.isNotEmpty()) {
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge(containerColor = PrimaryPink) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    color = TextPrimary,
                                )
                            }
                        }
                    },
                ) {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        draftFilters = activeFilters
                        showFilterSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.browse_filters_title),
                            tint = if (activeFilterCount > 0) PrimaryPink else TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Pilihan source: Semua + tiap source terdaftar (data-driven).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppChip(
                text = stringResource(R.string.search_source_all),
                selected = selectedSourceId == null,
                onClick = {
                    focusManager.clearFocus()
                    selectedSourceId = null
                    activeFilters = emptyMap()
                    draftFilters = emptyMap()
                },
            )
            sources.forEach { src ->
                AppChip(
                    text = src.name,
                    selected = selectedSourceId == src.id,
                    onClick = {
                        focusManager.clearFocus()
                        selectedSourceId = src.id
                        activeFilters = emptyMap()
                        draftFilters = emptyMap()
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (isLoading) {
                item {
                    PlaceholderGrid(rows = 2)
                }
            }

            if (localResults.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.search_section_saved),
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentPink,
                    )
                }
                items(
                    items = localResults,
                    key = { "local_${it.source}_${it.slug}" },
                    contentType = { "search_local_item" },
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = NoirMotion.Effects.Float,
                                fadeOutSpec = NoirMotion.Effects.Float,
                                placementSpec = NoirMotion.Spatial.asOffset,
                            )
                            .noirClickable(pressedScale = 0.96f) {
                                onDismiss()
                                onSelectComic(item.slug, item.source)
                            }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (item.isHistory) Icons.Default.History else Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = if (item.isHistory) TextSecondary else PrimaryPink,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.subtitle} • ${item.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.regular)
                }
            }

            val showSearchEmpty = !isLoading && !refreshFailed && debouncedQuery.length >= 2 &&
                localResults.isEmpty() && remoteResults.isEmpty()
            if (!isLoading && refreshFailed) {
                item {
                    TextButton(onClick = { searchRetry++ }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.search_refresh_failed) + " \u00B7 " + stringResource(R.string.common_try_again),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            if (!isLoading && remoteResults.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.search_section_results, remoteResults.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                    )
                }
                comicGridItems(
                    comics = remoteResults,
                    columns = 3,
                    showSourceTag = selectedSourceId == null,
                    onComicClick = { comic ->
                        onDismiss()
                        onSelectComic(comic.id, comic.sourceId ?: source)
                    },
                )
                if (remoteHasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            SecondaryButton(
                                text = stringResource(R.string.browse_btn_load_more),
                                onClick = { loadMore() },
                                enabled = !loadingMore,
                            )
                        }
                    }
                }
            } else if (showSearchEmpty) {
                item {
                    EmptyState(message = stringResource(R.string.search_empty_result, debouncedQuery), actionLabel = stringResource(R.string.search_reset), onAction = { query = "" })
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
                activeFilters = draftFilters
                showFilterSheet = false
            },
            onReset = { draftFilters = emptyMap() },
            onDismiss = { showFilterSheet = false },
        )
    }
}
