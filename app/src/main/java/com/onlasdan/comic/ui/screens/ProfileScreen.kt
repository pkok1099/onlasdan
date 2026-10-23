package com.onlasdan.comic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.onlasdan.comic.R
import com.onlasdan.comic.api.HomeComicItem
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.LocalBottomBarVisible
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.chrome.ChromeContext
import com.onlasdan.comic.ui.chrome.ChromeRouteTag
import com.onlasdan.comic.ui.chrome.LocalChromeContext
import com.onlasdan.comic.ui.components.LatestGrid
import com.onlasdan.comic.ui.screens.profile.ProfileBrick
import com.onlasdan.comic.ui.screens.profile.ProfileLayoutState
import com.onlasdan.comic.ui.util.BookmarkEntry
import com.onlasdan.comic.ui.util.DatabaseBackupStore
import com.onlasdan.comic.ui.util.HistoryEntry
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.bindAutoHideOnScroll
import com.onlasdan.comic.ui.util.clearHistory
import com.onlasdan.comic.ui.util.collapsibleHeader
import com.onlasdan.comic.ui.util.rememberNoirSpec
import com.onlasdan.comic.ui.util.rememberSelectedSource
import com.onlasdan.comic.ui.util.removeHistoryEntry
import com.onlasdan.comic.ui.util.setSelectedSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HistorySort { Newest, Oldest, Title }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onComicClick: (String) -> Unit,
    onHistoryClick: (comicSlug: String, source: String, chapterSlug: String) -> Unit,
    onSettingsClick: () -> Unit,
    initialTab: String = "bookmark",
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val bottomBarVisible = LocalBottomBarVisible.current
    val chromeContextState = LocalChromeContext.current
    val source = rememberSelectedSource()
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.profile?.blueprint }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = {}, onChooseSource = {})
        return
    }
    var selectedTab by remember(initialTab) {
        mutableIntStateOf(
            when (initialTab) {
                "history" -> 1
                "downloads" -> 2
                else -> 0
            },
        )
    }
    var bookmarks by remember { mutableStateOf<List<BookmarkEntry>>(emptyList()) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var historySort by remember { mutableStateOf(HistorySort.Newest) }
    var historyMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible, bottomBarVisible = bottomBarVisible)

    fun setHistorySort(sort: HistorySort) {
        historySort = sort
        historyMenuOpen = false
    }

    DisposableEffect(historySort) {
        val currentContext = ChromeContext(
            routeTag = ChromeRouteTag.Profile,
            setHistorySort = { key ->
                setHistorySort(
                    when (key) {
                        "oldest" -> HistorySort.Oldest
                        "title" -> HistorySort.Title
                        else -> HistorySort.Newest
                    },
                )
            },
        )
        chromeContextState.value = currentContext
        onDispose {
            if (chromeContextState.value == currentContext) {
                chromeContextState.value = null
            }
        }
    }

    suspend fun reload() {
        val b = withContext(Dispatchers.IO) {
            ComicDatabase.get(context).bookmarkDao().getAll().map {
                BookmarkEntry(it.comicSlug, it.title, it.thumbnail, it.source, it.createdAt)
            }
        }
        val h = withContext(Dispatchers.IO) {
            ComicDatabase.get(context).historyDao().getAll().map {
                HistoryEntry(it.comicSlug, it.title, it.thumbnail, it.chapterSlug, it.chapterTitle, it.source, it.readAt)
            }
        }
        bookmarks = b
        history = h
    }

    LaunchedEffect(Unit) { reload() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-hide handled by bindAutoHideOnScroll(listState,...)

    val layoutState = remember(bookmarks.size, history.size) {
        ProfileLayoutState(hasBookmarks = bookmarks.isNotEmpty(), hasHistory = history.isNotEmpty())
    }
    val headerPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Header].orEmpty() }
    val bodyPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Body].orEmpty() }

    Scaffold(
        topBar = {
            val headerProgress by animateNoirFloatAsState(
                targetValue = if (headerVisible.value) 1f else 0f,
                animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
                label = "profileHeaderProgress",
            )
            val headerAlpha by animateNoirFloatAsState(
                targetValue = if (headerVisible.value) 1f else 0f,
                animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
                label = "profileHeaderAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .collapsibleHeader(
                        progress = { headerProgress },
                        alpha = { headerAlpha },
                    ),
            ) {
                headerPlacements.forEach { placement ->
                    if (!placement.predicate(layoutState)) return@forEach
                    when (placement.brick) {
                        ProfileBrick.TopBar -> TopAppBar(
                            title = { Text(stringResource(R.string.screen_profile)) },
                            actions = {
                                if (selectedTab == 1) {
                                    Box {
                                        IconButton(onClick = { historyMenuOpen = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_history_menu))
                                        }
                                        DropdownMenu(expanded = historyMenuOpen, onDismissRequest = { historyMenuOpen = false }) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.profile_sort_newest)) },
                                                onClick = { setHistorySort(HistorySort.Newest) },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.profile_sort_oldest)) },
                                                onClick = { setHistorySort(HistorySort.Oldest) },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.profile_sort_title)) },
                                                onClick = { setHistorySort(HistorySort.Title) },
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.profile_clear_history)) },
                                                onClick = {
                                                    clearHistory(context)
                                                    scope.launch { reload() }
                                                    historyMenuOpen = false
                                                },
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = onSettingsClick) {
                                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                                }
                            },
                        )

                        else -> Unit
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            bodyPlacements.forEach { placement ->
                if (!placement.predicate(layoutState)) return@forEach
                when (placement.brick) {
                    ProfileBrick.Header -> ProfileHeader()

                    ProfileBrick.Tabs -> PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.profile_tab_bookmark)) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.profile_tab_history)) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.profile_tab_downloads)) })
                    }

                    ProfileBrick.Content -> when (selectedTab) {
                        0 -> BookmarkGrid(bookmarks, onComicClick)

                        2 -> DownloadsManagerContent(
                            onReadChapter = { comicSlug, src, chapterSlug ->
                                setSelectedSource(context, src)
                                onHistoryClick(comicSlug, src, chapterSlug)
                            },
                        )

                        else -> {
                            val sortedHistory = remember(history, historySort) {
                                when (historySort) {
                                    HistorySort.Newest -> history.sortedByDescending { it.readAt }
                                    HistorySort.Oldest -> history.sortedBy { it.readAt }
                                    HistorySort.Title -> history.sortedBy { it.title.lowercase(Locale.getDefault()) }
                                }
                            }
                            HistoryList(
                                history = sortedHistory,
                                listState = listState,
                                onOpen = { entry ->
                                    setSelectedSource(context, entry.source)
                                    onHistoryClick(entry.comicSlug, entry.source, entry.chapterSlug)
                                },
                                onDelete = { entry ->
                                    removeHistoryEntry(context, entry)
                                    scope.launch { reload() }
                                },
                            )
                        }
                    }

                    ProfileBrick.TopBar -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    val context = LocalContext.current
    val storageUri = DatabaseBackupStore.getCustomTreeUri(context)
    val storageLabel = storageUri?.lastPathSegment ?: stringResource(R.string.settings_storage_path_default)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.common_guest), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.profile_storage_label, storageLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BookmarkGrid(bookmarks: List<BookmarkEntry>, onComicClick: (String) -> Unit) {
    if (bookmarks.isEmpty()) {
        EmptyState(stringResource(R.string.profile_empty_bookmarks))
        return
    }
    val items = bookmarks.map {
        HomeComicItem(
            title = it.title,
            url = it.comicSlug,
            cover = it.thumbnail,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        LatestGrid(items, onComicClick, columns = 3, isHd = false)
    }
}

@Composable
private fun HistoryList(history: List<HistoryEntry>, listState: androidx.compose.foundation.lazy.LazyListState, onOpen: (HistoryEntry) -> Unit, onDelete: (HistoryEntry) -> Unit) {
    if (history.isEmpty()) {
        EmptyState(stringResource(R.string.profile_empty_history))
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = history,
            key = { "${it.source}:${it.comicSlug}:${it.chapterSlug}" },
            contentType = { "history_row" },
        ) { entry ->
            HistoryRow(entry, onOpen, onDelete)
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: (HistoryEntry) -> Unit, onDelete: (HistoryEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpen(entry) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = entry.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                entry.chapterTitle ?: entry.chapterSlug,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.profile_last_read_prefix, formatReadAt(entry.readAt)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        IconButton(onClick = { onDelete(entry) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_history))
        }
    }
}

private fun formatReadAt(epoch: Long): String {
    if (epoch <= 0) return "-"
    val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(epoch))
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
