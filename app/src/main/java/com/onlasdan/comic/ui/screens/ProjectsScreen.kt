package com.onlasdan.comic.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.components.*
import com.onlasdan.comic.ui.screens.projects.ProjectsBrick
import com.onlasdan.comic.ui.screens.projects.ProjectsLayoutState
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.util.UiMemoryCache
import com.onlasdan.comic.ui.util.rememberSelectedSource
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ProjectsScreen(
    onComicClick: (String) -> Unit,
    onBack: () -> Unit,
    isHd: Boolean,
    columns: Int,
) {
    val source = rememberSelectedSource()
    val refreshFailedText = stringResource(R.string.common_refresh_failed)
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.projects?.blueprint }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = onBack, onChooseSource = onBack)
        return
    }
    val cacheKey = remember(source) { "projects:$source" }
    var data by remember(cacheKey) { mutableStateOf(UiMemoryCache.peek<List<Comic>>(cacheKey)) }
    var failed by remember(source) { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(cacheKey, reloadKey) {
        try {
            loadProjectsCached(source, cacheKey, force = reloadKey > 0)?.let {
                data = it
                failed = false
            }
        } catch (_: Exception) {
            // Refresh gagal saat data lama masih tampil: layar tidak
            // ditimpa error penuh — cukup toast (Fase B2).
            if (data == null) failed = true else AppToast.error(refreshFailedText)
        }
    }

    when {
        data != null -> ProjectsContent(data.orEmpty(), onComicClick, onBack, isHd, columns, blueprint)
        failed -> ErrorState(message = stringResource(R.string.common_failed_load), onRetry = { reloadKey++ }, modifier = Modifier.fillMaxSize())
        else -> PlaceholderGrid()
    }
}

/** Null = cache masih segar, tidak perlu memuat ulang. */
private suspend fun loadProjectsCached(source: String, cacheKey: String, force: Boolean): List<Comic>? {
    if (!force && UiMemoryCache.isFresh(cacheKey)) return null
    val items = SourceRegistry.get(source).sectionFeed("projects", "", 1).items
    UiMemoryCache.put(cacheKey, items as Any, 10 * 60 * 1000L)
    return items
}

@Composable
private fun ProjectsContent(
    data: List<Comic>,
    onComicClick: (String) -> Unit,
    onBack: () -> Unit,
    isHd: Boolean,
    columns: Int,
    blueprint: com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint<ProjectsLayoutState, ProjectsBrick>,
) {
    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }
    val layoutState = remember(data.size) { ProjectsLayoutState(hasProjects = data.isNotEmpty()) }
    val headerPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Header].orEmpty() }
    val bodyPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Body].orEmpty() }

    var shown by remember { mutableStateOf(false) }
    SideEffect { shown = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(300),
        label = "projectsContentFade",
    )
    Column(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
        headerPlacements.forEach { placement ->
            if (!placement.predicate(layoutState)) return@forEach
            when (placement.brick) {
                ProjectsBrick.TopBar -> SimpleTopBar(title = stringResource(R.string.projects_title), onBack = onBack)
                else -> Unit
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            bodyPlacements.forEach { placement ->
                if (!placement.predicate(layoutState)) return@forEach
                when (placement.brick) {
                    ProjectsBrick.Empty -> item {
                        EmptyState(message = stringResource(R.string.projects_empty), modifier = Modifier.fillMaxSize())
                    }

                    ProjectsBrick.Grid -> {
                        val safeColumns = columns.coerceAtLeast(2)
                        homeComicGridItems(
                            comics = data,
                            columns = safeColumns,
                            isHd = isHd,
                            onComicClick = { onComicClick(it) },
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}
