package com.onlasdan.comic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.LocalBottomBarVisible
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.components.*
import com.onlasdan.comic.ui.screens.updates.UpdatesBrick
import com.onlasdan.comic.ui.screens.updates.UpdatesLayoutState
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

@Composable
@Suppress("kotlin:S3776")
fun UpdatesScreen(
    source: String = rememberSelectedSource(),
    onComicClick: (String) -> Unit,
    onBack: () -> Unit,
    isHd: Boolean,
    onToggleHd: () -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
) {
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.updates?.blueprint }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = onBack, onChooseSource = onBack)
        return
    }
    val failedUpdates = stringResource(R.string.updates_failed)
    var page by remember(source) { mutableIntStateOf(1) }
    val updatesCacheKey = remember(source, page) { "updates:$source:$page" }
    var data by remember(updatesCacheKey) { mutableStateOf(UiMemoryCache.peek<Paged<Comic>>(updatesCacheKey)) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var isLoading by remember(source) { mutableStateOf(true) }
    var isRefreshing by remember(source) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val bottomBarVisible = LocalBottomBarVisible.current
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible, bottomBarVisible = bottomBarVisible)

    LaunchedEffect(source, page) {
        val hasData = data != null
        isLoading = !hasData
        isRefreshing = hasData
        error = null

        if (UiMemoryCache.isFresh(updatesCacheKey) && data != null) {
            isLoading = false
            isRefreshing = false
            return@LaunchedEffect
        }
        try {
            val result = SourceRegistry.get(source).sectionFeed("updates", "", page)
            data = result
            UiMemoryCache.put(updatesCacheKey, result as Any, 5 * 60 * 1000L)
        } catch (_: Exception) {
            error = failedUpdates
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

    val comics = remember(data) { data?.items.orEmpty() }
    val state = remember(isLoading, data, error, comics.size) {
        UpdatesLayoutState(
            isLoading = isLoading,
            hasError = error != null,
            hasData = data != null,
            hasResults = comics.isNotEmpty(),
        )
    }
    val headerPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Header].orEmpty() }
    val bodyPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Body].orEmpty() }
    val enabled = remember(bodyPlacements, state) {
        bodyPlacements.filter { it.predicate(state) }.map { it.brick }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val headerProgress by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
            label = "updatesHeaderProgress",
        )
        val headerAlpha by animateNoirFloatAsState(
            targetValue = if (headerVisible.value) 1f else 0f,
            animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
            label = "updatesHeaderAlpha",
        )
        headerPlacements.forEach { placement ->
            if (!placement.predicate(state)) return@forEach
            when (placement.brick) {
                UpdatesBrick.TopBar -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .collapsibleHeader(
                            progress = { headerProgress },
                            alpha = { headerAlpha },
                        ),
                ) {
                    SimpleTopBar(
                        title = stringResource(R.string.screen_updates),
                        onBack = onBack,
                        actions = {},
                    )
                }

                else -> Unit
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (enabled.contains(UpdatesBrick.ControlsRow)) {
                LayoutAndHdRow(isHd, onToggleHd, columns, onColumnsChange)
                Spacer(Modifier.height(4.dp))
            }

            val showPagination = enabled.contains(UpdatesBrick.Pagination) && data != null

            when {
                enabled.contains(UpdatesBrick.Loading) -> {
                    com.onlasdan.comic.ui.components.PlaceholderGrid()
                }

                enabled.contains(UpdatesBrick.Error) -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(error ?: failedUpdates)
                }

                enabled.contains(UpdatesBrick.Empty) -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(stringResource(R.string.common_no_data_found))
                }

                else -> {
                    var updatesShown by remember { mutableStateOf(false) }
                    SideEffect { updatesShown = true }
                    val updatesAlpha by animateNoirFloatAsState(
                        targetValue = if (updatesShown) 1f else 0f,
                        animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
                        label = "updatesFade",
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = updatesAlpha },
                        contentPadding = PaddingValues(bottom = 100.dp),
                        state = listState,
                    ) {
                        if (enabled.contains(UpdatesBrick.Results)) {
                            homeComicGridItems(comics, columns, isHd, onComicClick)
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
}
