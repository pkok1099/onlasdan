package com.onlasdan.comic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.LocalBottomBarVisible
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.chrome.ChromeContext
import com.onlasdan.comic.ui.chrome.ChromeRouteTag
import com.onlasdan.comic.ui.chrome.LocalChromeContext
import com.onlasdan.comic.ui.components.ComicGrid
import com.onlasdan.comic.ui.components.NoirBottomSheet
import com.onlasdan.comic.ui.components.NumberPagination
import com.onlasdan.comic.ui.components.SimpleTopBar
import com.onlasdan.comic.ui.components.homeComicGridItems
import com.onlasdan.comic.ui.screens.browse.BrowseFilterOption
import com.onlasdan.comic.ui.screens.browse.BrowseFilterOptions
import com.onlasdan.comic.ui.screens.browse.toBrowseFilterOptions
import com.onlasdan.comic.ui.screens.genres.GenreDetailBrick
import com.onlasdan.comic.ui.screens.genres.GenreDetailLayoutState
import com.onlasdan.comic.ui.screens.genres.GenresListBrick
import com.onlasdan.comic.ui.screens.genres.GenresListLayoutState
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.Paged
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.UiMemoryCache
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.bindAutoHideOnScroll
import com.onlasdan.comic.ui.util.collapsibleHeader
import com.onlasdan.comic.ui.util.rememberNoirSpec
import com.onlasdan.comic.ui.util.rememberSelectedSource
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776")
fun GenresListScreen(onGenreClick: (String) -> Unit, onBack: (() -> Unit)? = null) {
    val source = rememberSelectedSource()
    val chromeContextState = LocalChromeContext.current
    val options = remember(source) { SourceRegistry.get(source).availableFilters().toBrowseFilterOptions() }
    val error: String? = null
    val bottomBarVisible = LocalBottomBarVisible.current
    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible, bottomBarVisible = bottomBarVisible)
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.genres }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = { onBack?.invoke() }, onChooseSource = { onBack?.invoke() })
        return
    }

    var genresSearchOpen by rememberSaveable { mutableStateOf(false) }
    var sortAsc by rememberSaveable { mutableStateOf(true) }
    DisposableEffect(sortAsc) {
        val currentContext = ChromeContext(
            routeTag = ChromeRouteTag.Genres,
            openGenresSearch = { genresSearchOpen = true },
            toggleGenresSort = { sortAsc = !sortAsc },
        )
        chromeContextState.value = currentContext
        onDispose {
            if (chromeContextState.value == currentContext) {
                chromeContextState.value = null
            }
        }
    }

    val state = remember(options) {
        GenresListLayoutState(
            isLoading = false,
            hasError = false,
            hasGenres = options.genres.isNotEmpty(),
            hasDemographics = options.demografis.isNotEmpty(),
            hasThemes = options.tema.isNotEmpty(),
            hasContent = options.konten.isNotEmpty(),
        )
    }
    val headerPlacements = remember(blueprint) { blueprint.list.slots[ScreenSlot.Header].orEmpty() }
    val bodyPlacements = remember(blueprint) { blueprint.list.slots[ScreenSlot.Body].orEmpty() }

    // Auto-hide handled by bindAutoHideOnScroll(listState,...)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val headerProgress by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
            label = "genresListHeaderProgress",
        )
        val headerAlpha by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
            label = "genresListHeaderAlpha",
        )
        headerPlacements.forEach { placement ->
            if (!placement.predicate(state)) return@forEach
            when (placement.brick) {
                GenresListBrick.TopBar -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .collapsibleHeader(
                            progress = { headerProgress },
                            alpha = { headerAlpha },
                        ),
                ) {
                    SimpleTopBar(title = stringResource(R.string.screen_genres), onBack = onBack)
                }

                else -> Unit
            }
        }

        val enabled = remember(bodyPlacements, state) {
            bodyPlacements.filter { it.predicate(state) }.map { it.brick }.toSet()
        }

        if (genresSearchOpen) {
            NoirBottomSheet(onDismiss = { genresSearchOpen = false }) {
                var q by rememberSaveable { mutableStateOf("") }
                val all = remember(options) {
                    (options.genres + options.demografis + options.tema + options.konten)
                        .distinctBy { it.value }
                }
                val sorted = remember(all, sortAsc) {
                    val base = all.sortedBy { it.label.lowercase(Locale.getDefault()) }
                    if (sortAsc) base else base.asReversed()
                }
                val filtered = remember(sorted, q) {
                    val t = q.trim()
                    if (t.isEmpty()) {
                        sorted
                    } else {
                        sorted.filter { it.label.contains(t, ignoreCase = true) }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp),
                ) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.chrome_context_search_genres)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {}),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        overscrollEffect = null,
                    ) {
                        items(
                            items = filtered,
                            key = { it.value },
                            contentType = { "genre_search_item" },
                        ) { item ->
                            ListItem(
                                headlineContent = { Text(item.label) },
                                modifier = Modifier.clickable {
                                    genresSearchOpen = false
                                    onGenreClick(item.value)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        when {
            enabled.contains(GenresListBrick.Loading) -> {
                com.onlasdan.comic.ui.components.PlaceholderGrid(modifier = Modifier.weight(1f))
            }

            enabled.contains(GenresListBrick.Error) -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "Error")
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (enabled.contains(GenresListBrick.GenresSection)) {
                    item { GenresSection(stringResource(R.string.genres_section_genres), options.genres, onGenreClick) }
                }
                if (enabled.contains(GenresListBrick.DemographicsSection)) {
                    item { GenresSection(stringResource(R.string.genres_section_demographics), options.demografis, onGenreClick) }
                }
                if (enabled.contains(GenresListBrick.ThemesSection)) {
                    item { GenresSection(stringResource(R.string.genres_section_themes), options.tema, onGenreClick) }
                }
                if (enabled.contains(GenresListBrick.ContentSection)) {
                    item { GenresSection(stringResource(R.string.genres_section_content), options.konten, onGenreClick) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GenresSection(
    title: String,
    items: List<BrowseFilterOption>,
    onGenreClick: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Column {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                SuggestionChip(
                    onClick = { onGenreClick(item.value) },
                    label = { Text(item.label) },
                )
            }
        }
    }
}

@Composable
@Suppress("kotlin:S3776")
fun GenreDetailScreen(
    genreSlug: String,
    onComicClick: (String) -> Unit,
    onBack: () -> Unit,
    isHd: Boolean,
    columns: Int,
) {
    val source = rememberSelectedSource()
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    val cacheKey = remember(source, genreSlug, page) { "genres:detail:$source:$genreSlug:$page" }
    var data by remember(cacheKey) { mutableStateOf(UiMemoryCache.peek<Paged<Comic>>(cacheKey)) }
    var genreName by remember(source, genreSlug) { mutableStateOf<String?>(null) }
    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }
    val bottomBarVisible = LocalBottomBarVisible.current
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible, bottomBarVisible = bottomBarVisible)
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.genres }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = onBack, onChooseSource = onBack)
        return
    }
    val failedLoadGenre = stringResource(R.string.genres_failed_load_genre)

    LaunchedEffect(cacheKey) {
        if (data != null && UiMemoryCache.isFresh(cacheKey)) return@LaunchedEffect
        try {
            val result = SourceRegistry.get(source).search("", mapOf("genre" to genreSlug), page)
            data = result
            UiMemoryCache.put(cacheKey, result as Any, 24 * 60 * 60 * 1000L)
        } catch (_: Exception) {
            error = failedLoadGenre
        }
    }

    LaunchedEffect(source, genreSlug) {
        genreName = runCatching {
            SourceRegistry.get(source).allGenres().firstOrNull { it.id.equals(genreSlug, ignoreCase = true) }?.name
        }.getOrNull()
    }

    LaunchedEffect(page) {
        listState.scrollToItem(0)
    }

    val comics = data?.items.orEmpty()
    val state = remember(data, error, comics.size) {
        GenreDetailLayoutState(
            isLoading = data == null && error == null,
            hasError = error != null,
            hasComics = comics.isNotEmpty(),
        )
    }
    val headerPlacements = remember(blueprint) { blueprint.detail.slots[ScreenSlot.Header].orEmpty() }
    val bodyPlacements = remember(blueprint) { blueprint.detail.slots[ScreenSlot.Body].orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val headerProgress by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
            label = "genreDetailHeaderProgress",
        )
        val headerAlpha by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
            label = "genreDetailHeaderAlpha",
        )
        headerPlacements.forEach { placement ->
            if (!placement.predicate(state)) return@forEach
            when (placement.brick) {
                GenreDetailBrick.TopBar -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .collapsibleHeader(
                            progress = { headerProgress },
                            alpha = { headerAlpha },
                        ),
                ) {
                    SimpleTopBar(title = genreName ?: stringResource(R.string.screen_genre), onBack = onBack)
                }

                else -> Unit
            }
        }

        val enabled = remember(bodyPlacements, state) {
            bodyPlacements.filter { it.predicate(state) }.map { it.brick }.toSet()
        }
        val showPagination = enabled.contains(GenreDetailBrick.Pagination) && data != null

        when {
            enabled.contains(GenreDetailBrick.Loading) -> {
                com.onlasdan.comic.ui.components.PlaceholderGrid(modifier = Modifier.weight(1f))
            }

            enabled.contains(GenreDetailBrick.Error) -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: failedLoadGenre)
            }

            enabled.contains(GenreDetailBrick.Empty) -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.genres_no_comics_found))
            }

            else -> Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    // Tanpa efek memantul bawaan di batas atas/bawah — sisa gerakan
                    // berhenti keras seperti daftar chapter di bottom sheet detail.
                    overscrollEffect = null,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (enabled.contains(GenreDetailBrick.Results)) {
                        homeComicGridItems(
                            comics = comics,
                            columns = columns,
                            isHd = isHd,
                            onComicClick = onComicClick,
                        )
                    }
                    if (showPagination) {
                        item {
                            NumberPagination(
                                currentPage = page,
                                totalPages = data?.totalPages,
                                onPageSelected = { page = it },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Spacer(Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }
}
