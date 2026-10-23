package com.onlasdan.comic.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.scraper.GenreMasterData
import com.onlasdan.comic.ui.LocalBottomBarVisible
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.chrome.ChromeContext
import com.onlasdan.comic.ui.chrome.ChromeRouteTag
import com.onlasdan.comic.ui.chrome.LocalChromeContext
import com.onlasdan.comic.ui.components.HomeComicCard
import com.onlasdan.comic.ui.components.LayoutAndHdRow
import com.onlasdan.comic.ui.components.NoirBottomSheet
import com.onlasdan.comic.ui.components.NumberPagination
import com.onlasdan.comic.ui.components.SimpleTopBar
import com.onlasdan.comic.ui.screens.BlueprintMissingScreen
import com.onlasdan.comic.ui.source.Paged
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.UiMemoryCache
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.bindAutoHideOnScroll
import com.onlasdan.comic.ui.util.collapsibleHeader
import com.onlasdan.comic.ui.util.extractSlug
import com.onlasdan.comic.ui.util.getImageUrl
import com.onlasdan.comic.ui.util.rememberNoirSpec
import com.onlasdan.comic.ui.util.rememberSelectedSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

enum class BrowseBrick {
    KomikuHeader,
    KomikindoHeader,
    ShinigamiHeader,
    DoujindesuHeader,
    LayoutRow,
    Results,
    Pagination,
    KomikuFilterSheet,
    KomikindoFilterSheet,
    ShinigamiFilterSheet,
    DoujindesuFilterSheet,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776")
fun BrowseScreen(
    source: String = rememberSelectedSource(),
    onComicClick: (String) -> Unit,
    onBack: (() -> Unit)?,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onChooseSource: () -> Unit,
) {
    val sourceBlueprints = remember(source) { BlueprintProvider.forSource(source) }
    if (sourceBlueprints == null) {
        BlueprintMissingScreen(source = source, onBack = { onBack?.invoke() }, onChooseSource = onChooseSource)
        return
    }
    val blueprint = sourceBlueprints.browse.blueprint

    val listState = rememberLazyListState()
    val browseErrorFailed = androidx.compose.ui.res.stringResource(R.string.browse_error_failed)
    val bottomBarVisible = LocalBottomBarVisible.current
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible, bottomBarVisible = bottomBarVisible)
    val chromeContextState = LocalChromeContext.current

    var komikuFilters by remember(source) { mutableStateOf(KomikuFilters()) }
    var komikindoFilters by remember(source) { mutableStateOf(KomikindoFilters()) }
    var shinigamiFilters by remember(source) { mutableStateOf(ShinigamiFilters()) }
    var doujindesuFilters by remember(source) { mutableStateOf(DoujindesuFilters()) }
    var searchTerm by remember(source) { mutableStateOf(TextFieldValue("")) }
    var activeQuery by remember(source) { mutableStateOf<String?>(null) }

    val adapterStateKey = remember(activeQuery, komikuFilters, komikindoFilters, shinigamiFilters, doujindesuFilters) {
        listOf(
            activeQuery,
            komikuFilters.hashCode(),
            komikindoFilters.hashCode(),
            shinigamiFilters.hashCode(),
            doujindesuFilters.hashCode(),
        )
    }

    var showFilters by remember(source) { mutableStateOf(false) }
    var page by remember(source) { mutableIntStateOf(1) }
    val browseCacheKey = remember(source, adapterStateKey, page) { "browse:$source:${adapterStateKey.hashCode()}:$page" }
    var data by remember(browseCacheKey) { mutableStateOf(UiMemoryCache.peek<Paged<com.onlasdan.comic.ui.source.Comic>>(browseCacheKey)) }
    val filterOptions = remember(source) { SourceRegistry.get(source).availableFilters().toBrowseFilterOptions() }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var isLoading by remember(source) { mutableStateOf(true) }
    var isRefreshing by remember(source) { mutableStateOf(false) }

    DisposableEffect(isHd, columns, source) {
        val currentContext = ChromeContext(
            routeTag = ChromeRouteTag.Browse,
            isHd = isHd,
            columns = columns,
            openBrowseFilter = { showFilters = true },
            resetBrowseFilter = {
                komikuFilters = KomikuFilters()
                komikindoFilters = KomikindoFilters()
                shinigamiFilters = ShinigamiFilters()
                doujindesuFilters = DoujindesuFilters()
                searchTerm = TextFieldValue("")
                activeQuery = null
                page = 1
            },
        )
        chromeContextState.value = currentContext
        onDispose {
            if (chromeContextState.value == currentContext) {
                chromeContextState.value = null
            }
        }
    }

    LaunchedEffect(source, page, adapterStateKey) {
        val hasData = data != null
        isLoading = !hasData
        isRefreshing = hasData
        error = null

        if (UiMemoryCache.isFresh(browseCacheKey) && data != null) {
            isLoading = false
            isRefreshing = false
            return@LaunchedEffect
        }
        try {
            val (query, filterMap) = buildBrowseFilterMap(source, activeQuery, komikuFilters, komikindoFilters, shinigamiFilters, doujindesuFilters)
            val result = SourceRegistry.get(source).search(query, filterMap, page)
            data = result
            UiMemoryCache.put(browseCacheKey, result as Any, 5 * 60 * 1000L)
        } catch (_: Exception) {
            error = browseErrorFailed
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(source) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(page) {
        listState.scrollToItem(0)
    }

    val state = BrowseState(
        source = source,
        showFilters = showFilters,
        page = page,
        data = data,
        filterOptions = filterOptions,
        error = error,
        isLoading = isLoading,
        komiku = komikuFilters,
        komikindo = komikindoFilters,
        shinigami = shinigamiFilters,
        doujindesu = doujindesuFilters,
        searchTerm = searchTerm,
        activeQuery = activeQuery,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        val headerProgress by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
            label = "browseHeaderProgress",
        )
        val headerAlpha by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
            label = "browseHeaderAlpha",
        )

        // Keep the TopBar always visible; only the extra controls auto-hide/collapse.
        SimpleTopBar(
            title = androidx.compose.ui.res.stringResource(R.string.screen_browse),
            onBack = onBack,
            compact = true,
            actions = { },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .collapsibleHeader(
                    progress = { headerProgress },
                    alpha = { headerAlpha },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Compact header padding (avoid covering too much screen).
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                blueprint.slots[ScreenSlot.Header].orEmpty().forEach { placement ->
                    if (placement.predicate(state)) {
                        RenderBrick(
                            brick = placement.brick,
                            state = state,
                            isHd = isHd,
                            onToggleHd = onToggleHd,
                            columns = columns,
                            onColumnsChange = onColumnsChange,
                            onOpenFilters = { showFilters = true },
                            onReset = {
                                komikuFilters = KomikuFilters()
                                komikindoFilters = KomikindoFilters()
                                shinigamiFilters = ShinigamiFilters()
                                doujindesuFilters = DoujindesuFilters()
                                page = 1
                            },
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Body slot
            val placements = blueprint.slots[ScreenSlot.Body].orEmpty()
            val showResults = !state.isLoading || state.data != null || state.error != null
            val showPagination = remember(placements, state) {
                placements.any { it.brick == BrowseBrick.Pagination && it.predicate(state) }
            }
            placements.forEach { placement ->
                if (!placement.predicate(state)) return@forEach
                when (placement.brick) {
                    BrowseBrick.LayoutRow -> {
                        Unit // Controls moved into header next to Filters/Reset.
                    }

                    BrowseBrick.Results -> {
                        if (!showResults) {
                            com.onlasdan.comic.ui.components.PlaceholderGrid()
                        } else {
                            when {
                                state.isLoading && state.data == null -> {
                                    com.onlasdan.comic.ui.components.PlaceholderGrid()
                                }

                                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(state.error)
                                }

                                state.data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.common_no_data_found))
                                }

                                else -> {
                                    val data = requireNotNull(state.data)
                                    Box(modifier = Modifier.weight(1f)) {
                                        var browseShown by remember { mutableStateOf(false) }
                                        SideEffect { browseShown = true }
                                        val browseAlpha by animateNoirFloatAsState(
                                            targetValue = if (browseShown) 1f else 0f,
                                            animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
                                            label = "browseFade",
                                        )
                                        Box(Modifier.graphicsLayer { alpha = browseAlpha }) {
                                            val safeColumns = columns.coerceAtLeast(2)
                                            val items = data.items
                                            val rows = remember(items, safeColumns) { items.chunked(safeColumns) }

                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                state = listState,
                                                contentPadding = PaddingValues(bottom = 100.dp),
                                            ) {
                                                itemsIndexed(
                                                    items = rows,
                                                    key = { idx, row -> "${idx}_${row.firstOrNull()?.id ?: row.firstOrNull()?.title.orEmpty()}" },
                                                    contentType = { _, _ -> "browse_comic_row" },
                                                ) { _, row ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .animateItem()
                                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    ) {
                                                        row.forEach { comic ->
                                                            Box(Modifier.weight(1f)) {
                                                                HomeComicCard(
                                                                    comic.title,
                                                                    getImageUrl(comic.cover, isHd),
                                                                    comic.type,
                                                                    comic.latestChapter,
                                                                    sharedKey = "cover-${comic.id}",
                                                                ) { onComicClick(comic.id) }
                                                            }
                                                        }
                                                        if (row.size < safeColumns) repeat(safeColumns - row.size) { Spacer(Modifier.weight(1f)) }
                                                    }
                                                }
                                                if (showPagination) {
                                                    item {
                                                        NumberPagination(
                                                            currentPage = page,
                                                            totalPages = data.totalPages,
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
                        }
                    }

                    BrowseBrick.Pagination -> Unit

                    // Rendered as a footer for better UX.
                    else -> Unit
                }
            }
        }

        // BottomSheet slot
        blueprint.slots[ScreenSlot.BottomSheet].orEmpty().forEach { placement ->
            if (!placement.predicate(state)) return@forEach
            when (placement.brick) {
                BrowseBrick.KomikuFilterSheet -> {
                    if (!showFilters) return@forEach
                    NoirBottomSheet(onDismiss = { showFilters = false }) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            item {
                                KomikuFilterSheet(
                                    options = state.filterOptions,
                                    filters = komikuFilters,
                                    onChange = { komikuFilters = it },
                                    onApply = {
                                        val f = komikuFilters
                                        komikuFilters = if (f.orderby != "meta_value_num") {
                                            f.copy(sorttime = "")
                                        } else if (f.sorttime.isBlank()) {
                                            f.copy(sorttime = "all")
                                        } else {
                                            f
                                        }
                                        page = 1
                                        showFilters = false
                                    },
                                    onReset = {
                                        komikuFilters = KomikuFilters()
                                        page = 1
                                        showFilters = false
                                    },
                                )
                            }
                        }
                    }
                }

                BrowseBrick.KomikindoFilterSheet -> {
                    if (!showFilters) return@forEach
                    NoirBottomSheet(onDismiss = { showFilters = false }) {
                        var pending by remember { mutableStateOf(komikindoFilters) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            item {
                                KomikindoFilterSheet(
                                    options = state.filterOptions,
                                    filters = pending,
                                    onChange = { pending = it },
                                    onApply = {
                                        komikindoFilters = pending
                                        page = 1
                                        showFilters = false
                                    },
                                    onReset = { pending = KomikindoFilters() },
                                )
                            }
                        }
                    }
                }

                BrowseBrick.ShinigamiFilterSheet -> {
                    if (!showFilters) return@forEach
                    NoirBottomSheet(onDismiss = { showFilters = false }) {
                        var pending by remember { mutableStateOf(shinigamiFilters) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            item {
                                ShinigamiFilterSheet(
                                    options = state.filterOptions,
                                    filters = pending,
                                    onChange = { pending = it },
                                    onApply = {
                                        shinigamiFilters = pending
                                        page = 1
                                        showFilters = false
                                    },
                                    onReset = { pending = ShinigamiFilters() },
                                )
                            }
                        }
                    }
                }

                BrowseBrick.DoujindesuFilterSheet -> {
                    if (!showFilters) return@forEach
                    NoirBottomSheet(onDismiss = { showFilters = false }) {
                        var pending by remember { mutableStateOf(doujindesuFilters) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            item {
                                DoujindesuFilterSheet(
                                    filters = pending,
                                    onChange = { pending = it },
                                    onApply = {
                                        doujindesuFilters = pending
                                        page = 1
                                        showFilters = false
                                    },
                                    onReset = { pending = DoujindesuFilters() },
                                )
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun RenderBrick(
    brick: BrowseBrick,
    state: BrowseState,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    when (brick) {
        BrowseBrick.KomikuHeader -> KomikuBrowseHeader(
            orderby = state.komiku.orderby,
            tipe = state.komiku.tipe,
            status = state.komiku.status,
            genre = state.komiku.genre,
            sorttime = state.komiku.sorttime,
            isHd = isHd,
            onToggleHd = onToggleHd,
            columns = columns,
            onColumnsChange = onColumnsChange,
            onOpenFilters = onOpenFilters,
            onReset = onReset,
        )

        BrowseBrick.KomikindoHeader -> KomikindoBrowseHeader(
            enabled = !state.isLoading,
            isHd = isHd,
            onToggleHd = onToggleHd,
            columns = columns,
            onColumnsChange = onColumnsChange,
            onOpenFilters = onOpenFilters,
            onReset = onReset,
        )

        BrowseBrick.ShinigamiHeader -> ShinigamiBrowseHeader(
            sort = state.shinigami.sort,
            format = state.shinigami.format,
            status = state.shinigami.status,
            genre = state.shinigami.genre,
            isHd = isHd,
            onToggleHd = onToggleHd,
            columns = columns,
            onColumnsChange = onColumnsChange,
            onOpenFilters = onOpenFilters,
            onReset = onReset,
        )

        BrowseBrick.DoujindesuHeader -> DoujindesuBrowseHeader(
            sort = state.doujindesu.sort,
            type = state.doujindesu.type,
            status = state.doujindesu.status,
            genre = state.doujindesu.genre,
            isHd = isHd,
            onToggleHd = onToggleHd,
            columns = columns,
            onColumnsChange = onColumnsChange,
            onOpenFilters = onOpenFilters,
            onReset = onReset,
        )

        else -> Unit
    }
}

@Composable
private fun HeaderControlIcons(
    enabled: Boolean,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onReset, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.browse_btn_reset))
        }
    }
}

@Composable
private fun KomikuBrowseHeader(
    orderby: String,
    tipe: String,
    status: String,
    genre: String,
    sorttime: String,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HeaderControlIcons(
                enabled = true,
                isHd = isHd,
                onToggleHd = onToggleHd,
                columns = columns,
                onColumnsChange = onColumnsChange,
                onOpenFilters = onOpenFilters,
                onReset = onReset,
            )
        }
        val chips = listOfNotNull(
            orderby.takeIf { it.isNotBlank() }?.let { "orderby=$it" },
            tipe.takeIf { it.isNotBlank() }?.let { "tipe=$it" },
            status.takeIf { it.isNotBlank() }?.let { "status=$it" },
            genre.takeIf { it.isNotBlank() }?.let { "genre=$it" },
            sorttime.takeIf { it.isNotBlank() }?.let { "sorttime=$it" },
        )
        if (chips.isNotEmpty()) {
            Text(chips.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}

@Composable
private fun KomikindoBrowseHeader(
    enabled: Boolean,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HeaderControlIcons(
                enabled = enabled,
                isHd = isHd,
                onToggleHd = onToggleHd,
                columns = columns,
                onColumnsChange = onColumnsChange,
                onOpenFilters = onOpenFilters,
                onReset = onReset,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun ShinigamiBrowseHeader(
    sort: String,
    format: String,
    status: String,
    genre: String,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenFilters, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.browse_btn_filters),
                )
            }
            HeaderControlIcons(
                enabled = true,
                isHd = isHd,
                onToggleHd = onToggleHd,
                columns = columns,
                onColumnsChange = onColumnsChange,
                onOpenFilters = onOpenFilters,
                onReset = onReset,
            )
        }
        val chips = listOfNotNull(
            sort.takeIf { it.isNotBlank() }?.let { "sort=$it" },
            format.takeIf { it.isNotBlank() }?.let { "format=$it" },
            status.takeIf { it.isNotBlank() }?.let { "status=$it" },
            genre.takeIf { it.isNotBlank() }?.let { "genre=$it" },
        )
        if (chips.isNotEmpty()) {
            Text(chips.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}

@Composable
private fun DoujindesuBrowseHeader(
    sort: String,
    type: String,
    status: String,
    genre: String,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenFilters, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.browse_btn_filters),
                )
            }
            HeaderControlIcons(
                enabled = true,
                isHd = isHd,
                onToggleHd = onToggleHd,
                columns = columns,
                onColumnsChange = onColumnsChange,
                onOpenFilters = onOpenFilters,
                onReset = onReset,
            )
        }
        val chips = listOfNotNull(
            sort.takeIf { it.isNotBlank() }?.let { "sort=$it" },
            type.takeIf { it.isNotBlank() }?.let { "type=$it" },
            status.takeIf { it.isNotBlank() }?.let { "status=$it" },
            genre.takeIf { it.isNotBlank() }?.let { "genre=$it" },
        )
        if (chips.isNotEmpty()) {
            Text(chips.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}

@Composable
private fun DoujindesuFilterSheet(
    filters: DoujindesuFilters,
    onChange: (DoujindesuFilters) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.browse_filters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SimpleSelect(
            label = stringResource(R.string.browse_order_by),
            value = filters.sort,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_sort_latest_chapter), "latest_chapter"),
                BrowseFilterOption(stringResource(R.string.browse_sort_popular), "popular"),
                BrowseFilterOption(stringResource(R.string.browse_sort_rating), "rating"),
                BrowseFilterOption(stringResource(R.string.browse_sort_views), "views"),
            ),
            onChange = { onChange(filters.copy(sort = it.ifBlank { "latest_chapter" })) },
            placeholder = stringResource(R.string.browse_sort_latest_chapter),
        )

        SimpleSelect(
            label = stringResource(R.string.browse_type),
            value = filters.type,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_all), ""),
                BrowseFilterOption(stringResource(R.string.browse_type_doujinshi), "doujinshi"),
                BrowseFilterOption(stringResource(R.string.browse_type_manga), "manga"),
                BrowseFilterOption(stringResource(R.string.browse_type_manhwa), "manhwa"),
            ),
            onChange = { onChange(filters.copy(type = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        SimpleSelect(
            label = stringResource(R.string.browse_status),
            value = filters.status,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_all), ""),
                BrowseFilterOption(stringResource(R.string.browse_status_ongoing), "publishing"),
                BrowseFilterOption(stringResource(R.string.browse_status_completed), "completed"),
            ),
            onChange = { onChange(filters.copy(status = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        val allLabel = stringResource(R.string.browse_all)
        val genreOptions = remember(allLabel) {
            listOf(BrowseFilterOption(allLabel, "")) +
                GenreMasterData.doujindesuMasterGenres.map {
                    BrowseFilterOption(it.name, it.id)
                }
        }
        SimpleSelect(
            label = stringResource(R.string.browse_genre),
            value = filters.genre,
            items = genreOptions,
            onChange = { onChange(filters.copy(genre = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_reset), style = MaterialTheme.typography.labelMedium) }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_apply_filter), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun ShinigamiFilterSheet(
    options: BrowseFilterOptions?,
    filters: ShinigamiFilters,
    onChange: (ShinigamiFilters) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.browse_filters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SimpleSelect(
            label = stringResource(R.string.browse_order_by),
            value = filters.sort,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_sort_latest), "latest"),
                BrowseFilterOption(stringResource(R.string.browse_sort_popular), "popularity"),
                BrowseFilterOption(stringResource(R.string.browse_sort_rating), "rating"),
            ),
            onChange = { onChange(filters.copy(sort = it.ifBlank { "latest" })) },
            placeholder = stringResource(R.string.browse_sort_latest),
        )

        SimpleSelect(
            label = stringResource(R.string.browse_format),
            value = filters.format,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_all), ""),
                BrowseFilterOption(stringResource(R.string.browse_format_manga), "manga"),
                BrowseFilterOption(stringResource(R.string.browse_format_manhwa), "manhwa"),
                BrowseFilterOption(stringResource(R.string.browse_format_manhua), "manhua"),
            ),
            onChange = { onChange(filters.copy(format = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        SimpleSelect(
            label = stringResource(R.string.browse_status),
            value = filters.status,
            items = listOf(
                BrowseFilterOption(stringResource(R.string.browse_all), ""),
                BrowseFilterOption(stringResource(R.string.browse_status_ongoing), "ongoing"),
                BrowseFilterOption(stringResource(R.string.browse_status_completed), "completed"),
                BrowseFilterOption(stringResource(R.string.browse_status_hiatus), "hiatus"),
            ),
            onChange = { onChange(filters.copy(status = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        val allLabel = stringResource(R.string.browse_all)
        val genreOptions = remember(options?.genres, allLabel) {
            val fromOptions = options?.genres?.map { BrowseFilterOption(it.label, it.value) }.orEmpty()
            if (fromOptions.isNotEmpty()) {
                listOf(BrowseFilterOption(allLabel, "")) + fromOptions
            } else {
                listOf(BrowseFilterOption(allLabel, "")) +
                    GenreMasterData.shinigamiMasterGenres.map { BrowseFilterOption(it.name, it.id) }
            }
        }
        SimpleSelect(
            label = stringResource(R.string.browse_genres),
            value = filters.genre,
            items = genreOptions,
            onChange = { onChange(filters.copy(genre = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_reset), style = MaterialTheme.typography.labelMedium) }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_apply_filter), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun KomikindoFilterSheet(
    options: BrowseFilterOptions?,
    filters: KomikindoFilters,
    onChange: (KomikindoFilters) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.browse_filters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = filters.title.orEmpty(),
            onValueChange = { onChange(filters.copy(title = it.trim().ifBlank { null })) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.browse_search_placeholder_id)) },
            singleLine = true,
        )

        SimpleSelect(label = stringResource(R.string.browse_type), value = filters.type, items = options?.type ?: emptyList(), onChange = { onChange(filters.copy(type = it)) }, placeholder = stringResource(R.string.browse_all))
        SimpleSelect(label = stringResource(R.string.browse_status), value = filters.status, items = options?.status ?: emptyList(), onChange = { onChange(filters.copy(status = it)) }, placeholder = stringResource(R.string.browse_all))
        SimpleSelect(label = stringResource(R.string.browse_format), value = filters.format, items = options?.format ?: emptyList(), onChange = { onChange(filters.copy(format = it)) }, placeholder = stringResource(R.string.browse_all))
        SimpleSelect(label = stringResource(R.string.browse_order), value = filters.order, items = options?.orderBy ?: emptyList(), onChange = { onChange(filters.copy(order = it)) }, placeholder = stringResource(R.string.browse_default))

        MultiSelectSection(stringResource(R.string.browse_genres), options?.genres ?: emptyList(), filters.genres) { onChange(filters.copy(genres = it)) }
        MultiSelectSection(stringResource(R.string.browse_demografis), options?.demografis ?: emptyList(), filters.demografis) { onChange(filters.copy(demografis = it)) }
        MultiSelectSection(stringResource(R.string.browse_konten), options?.konten ?: emptyList(), filters.konten) { onChange(filters.copy(konten = it)) }
        MultiSelectSection(stringResource(R.string.browse_tema), options?.tema ?: emptyList(), filters.tema) { onChange(filters.copy(tema = it)) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_reset), style = MaterialTheme.typography.labelMedium) }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_apply_filter), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun MultiSelectSection(
    title: String,
    options: List<BrowseFilterOption>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
) {
    if (options.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
        items(
            items = options,
            key = { it.value },
            contentType = { "filter_option" },
        ) { opt ->
            val checked = selected.contains(opt.value)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectedChange(
                            if (checked) selected - opt.value else selected + opt.value,
                        )
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        onSelectedChange(
                            if (isChecked) selected + opt.value else selected - opt.value,
                        )
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(opt.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun KomikuFilterSheet(
    options: BrowseFilterOptions?,
    filters: KomikuFilters,
    onChange: (KomikuFilters) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.browse_filters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SimpleSelect(
            label = stringResource(R.string.browse_order_by),
            value = filters.orderby,
            items = (options?.orderBy ?: emptyList()).ifEmpty {
                listOf(
                    BrowseFilterOption("Modified", "modified"),
                    BrowseFilterOption("Date", "date"),
                    BrowseFilterOption("Popular", "meta_value_num"),
                    BrowseFilterOption("Random", "rand"),
                )
            },
            onChange = { onChange(filters.copy(orderby = it)) },
            placeholder = stringResource(R.string.browse_placeholder_modified),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                SimpleSelect(
                    label = stringResource(R.string.browse_tipe),
                    value = filters.tipe,
                    items = (options?.type ?: emptyList()).ifEmpty {
                        listOf(
                            BrowseFilterOption(stringResource(R.string.browse_all), ""),
                            BrowseFilterOption("Manga", "manga"),
                            BrowseFilterOption("Manhwa", "manhwa"),
                            BrowseFilterOption("Manhua", "manhua"),
                        )
                    },
                    onChange = { onChange(filters.copy(tipe = it)) },
                    placeholder = stringResource(R.string.browse_all),
                )
            }
            Box(Modifier.weight(1f)) {
                SimpleSelect(
                    label = stringResource(R.string.browse_status),
                    value = filters.status,
                    items = (options?.status ?: emptyList()).ifEmpty {
                        listOf(
                            BrowseFilterOption(stringResource(R.string.browse_all), ""),
                            BrowseFilterOption("Ongoing", "ongoing"),
                            BrowseFilterOption("Completed", "completed"),
                        )
                    },
                    onChange = { onChange(filters.copy(status = it)) },
                    placeholder = stringResource(R.string.browse_all),
                )
            }
        }

        SimpleSelect(
            label = stringResource(R.string.browse_genre),
            value = filters.genre,
            items = (options?.genres ?: emptyList()).ifEmpty { GenreMasterData.komikuMasterGenres.map { BrowseFilterOption(it.name, it.id) } },
            onChange = { onChange(filters.copy(genre = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        SimpleSelect(
            label = stringResource(R.string.browse_sorttime),
            value = filters.sorttime,
            items = (options?.sortTime ?: emptyList()),
            onChange = { onChange(filters.copy(sorttime = it)) },
            placeholder = stringResource(R.string.browse_all),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_reset), style = MaterialTheme.typography.labelMedium) }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) { Text(stringResource(R.string.browse_btn_apply_filter), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun SimpleSelect(
    label: String,
    value: String,
    items: List<BrowseFilterOption>,
    onChange: (String) -> Unit,
    placeholder: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        var expanded by remember { mutableStateOf(false) }
        val currentLabel = items.firstOrNull { it.value == value }?.label
            ?: value.takeIf { it.isNotBlank() }
            ?: placeholder

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(currentLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browse_all)) },
                    onClick = {
                        onChange("")
                        expanded = false
                    },
                )
                items.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        onClick = {
                            onChange(opt.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
