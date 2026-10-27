package comic.sourcebrowse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import comic.R
import comic.components.AppSearchBar
import comic.components.AppToast
import comic.components.ComicGrid
import comic.components.EmptyState
import comic.components.ErrorState
import comic.components.PlaceholderGrid
import comic.components.SecondaryButton
import comic.components.SourceFilterSheet
import comic.components.comicGridItems
import comic.source.AppErrorHandler
import comic.source.Comic
import comic.source.ComicSource
import comic.source.SourceRegistry
import comic.theme.PureBlack
import comic.theme.Spacing
import comic.theme.TextPrimary
import kotlin.random.Random
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

    // Hoisted agar reset scroll bisa dipicu perubahan source/query/filter (lihat efek
    // reload di bawah) — tanpa ini LazyColumn memakai saveable state internal yang
    // mewarisi posisi scroll lama antar set hasil.
    val listState = rememberLazyListState()

    var query by remember(sourceId) { mutableStateOf("") }
    var appliedQuery by remember(sourceId) { mutableStateOf("") }
    var draftFilters by remember(sourceId, initialFilters) { mutableStateOf<Map<String, String>>(initialFilters) }
    var appliedFilters by remember(sourceId, initialFilters) { mutableStateOf<Map<String, String>>(initialFilters) }
    var uiState by remember(sourceId) { mutableStateOf<SourceBrowseUiState>(SourceBrowseUiState.Loading) }
    var showFilterSheet by remember(sourceId) { mutableStateOf(false) }
    var searchJob by remember(sourceId) { mutableStateOf<Job?>(null) }
    var isRollingRandom by remember(sourceId) { mutableStateOf(false) }
    val context = LocalContext.current

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

    LaunchedEffect(sourceId, appliedQuery, appliedFilters) {
        reload()
        // Set hasil baru selalu dari atas (bug UX: pindah source mendarat di dasar list).
        listState.scrollToItem(0)
    }

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
                // F3: komik acak — halaman katalog acak, lalu satu judul acak dari halaman itu.
                IconButton(
                    enabled = !isRollingRandom,
                    onClick = {
                        scope.launch {
                            isRollingRandom = true
                            val picked = rollRandomComic(source)
                            isRollingRandom = false
                            if (picked != null) {
                                onComicClick(picked.id, picked.sourceId ?: sourceId)
                            } else {
                                AppToast.error(context.getString(R.string.browse_random_error))
                            }
                        }
                    },
                ) {
                    if (isRollingRandom) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = stringResource(R.string.browse_random_cd),
                            tint = TextPrimary,
                        )
                    }
                }
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
                    actionLabel = stringResource(R.string.common_try_again),
                    onAction = { reload() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg),
                )

                is SourceBrowseUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
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

// Batas halaman katalog untuk roll acak saat source tidak mengungkap totalPages.
private const val RANDOM_PAGE_POOL = 6

/** F3: pilih satu komik acak dari katalog source — halaman acak dulu, baru item acak. */
private suspend fun rollRandomComic(source: ComicSource): Comic? {
    val first = runCatching { source.search(query = "", filters = emptyMap(), page = 1) }.getOrNull() ?: return null
    if (first.items.isEmpty()) return null
    val maxPage = (first.totalPages ?: RANDOM_PAGE_POOL).coerceIn(1, RANDOM_PAGE_POOL)
    val targetPage = Random.nextInt(1, maxPage + 1)
    val pool = if (targetPage == 1) {
        first.items
    } else {
        runCatching { source.search(query = "", filters = emptyMap(), page = targetPage).items }.getOrDefault(emptyList())
    }
    return pool.ifEmpty { first.items }.randomOrNull()
}
