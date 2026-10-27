package comic.reader

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.components.ReaderCanvasPlaceholder
import comic.db.BookmarkEntity
import comic.db.ComicDatabase
import comic.source.SOURCE_REFERERS
import comic.source.SourceRegistry
import comic.util.BookmarkEntry
import comic.util.DbChangeNotifier
import comic.util.DbIo
import comic.util.HistoryEntry
import comic.util.extractSlug
import comic.util.upsertHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
@Suppress("kotlin:S107", "kotlin:S3776")
fun ReaderContent(
    source: String,
    slug: String,
    chapterSlug: String,
    images: List<String>,
    listState: LazyListState,
    context: Context,
    scope: CoroutineScope,
    focusManager: FocusManager,
    blueprint: ScreenBlueprint<ReaderLayoutState, ReaderBrick>,
    showControls: Boolean,
    onUpdateShowControls: (Boolean) -> Unit,
    activeExpandedTab: ReaderExpandedTab?,
    onUpdateActiveExpandedTab: (ReaderExpandedTab?) -> Unit,
    sheetProgress: Animatable<Float, *>,
    autoScrollActive: Boolean,
    onAutoScrollToggle: () -> Unit,
    autoScrollSpeed: Float,
    onAutoScrollSpeedChange: (Float) -> Unit,
    autoNextEnabled: Boolean,
    onAutoNextToggle: () -> Unit,
    autoNextDelaySeconds: Float,
    onAutoNextDelayChange: (Float) -> Unit,
    immersiveFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    keepScreenOnEnabled: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    seamlessChaptersEnabled: Boolean,
    onSeamlessChange: (Boolean) -> Unit,
    nightDimmerAlpha: Float,
    onNightDimmerChange: (Float) -> Unit,
    loadedPageSet: SnapshotStateMap<Int, Boolean>,
    onSettledImageIncrement: () -> Unit,
    onUpdateAutoScrollPausedUntil: (Long) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmarkState: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateChapter: (String) -> Unit,
) {
    var comicDetails by remember { mutableStateOf<ReaderComicDetail?>(null) }
    var hasUserScrolled by rememberSaveable(slug, chapterSlug) { mutableStateOf(false) }
    var readVersion by rememberSaveable(slug) { mutableIntStateOf(0) }

    val loadedChapters = remember(slug, chapterSlug) { mutableStateListOf<LoadedChapter>() }
    var isFetchingNextChapter by remember(slug, chapterSlug) { mutableStateOf(false) }

    val chapterList = comicDetails?.chapterList.orEmpty()
    val chapterIndex = remember(chapterList, chapterSlug) {
        val idx = chapterList.indexOfFirst { isChapterSlugMatch(it, chapterSlug) }
        if (idx >= 0) idx else 0
    }
    val chapterCount = chapterList.size
    val chapterItem = remember(chapterList, chapterIndex) {
        chapterList.getOrNull(chapterIndex)
    }
    val chapterTitle = chapterItem?.title
    val chapterNumber = remember(chapterItem, chapterTitle, chapterIndex, chapterCount) {
        extractChapterNumber(chapterItem, chapterTitle, chapterIndex, chapterCount)
    }

    // Inisialisasi bab pertama ke list
    LaunchedEffect(images, chapterSlug, chapterTitle, chapterNumber) {
        if (images.isNotEmpty() && loadedChapters.none { it.slug == chapterSlug }) {
            loadedChapters.add(
                LoadedChapter(
                    slug = chapterSlug,
                    title = chapterTitle,
                    number = chapterNumber,
                    images = images,
                ),
            )
        }
    }

    // Jika seamless dinonaktifkan, bersihkan bab tambahan
    LaunchedEffect(seamlessChaptersEnabled) {
        if (!seamlessChaptersEnabled && loadedChapters.size > 1) {
            val first = loadedChapters.firstOrNull { it.slug == chapterSlug }
            loadedChapters.clear()
            if (first != null) loadedChapters.add(first)
        }
    }

    // Bentuk item baca berkelanjutan (Continuous Reader Items)
    val continuousItems = remember(loadedChapters.toList(), seamlessChaptersEnabled, comicDetails) {
        buildContinuousItems(
            loadedChapters = loadedChapters.toList(),
            seamlessChaptersEnabled = seamlessChaptersEnabled,
            comicDetails = comicDetails,
            initialChapterSlug = chapterSlug,
            initialChapterTitle = chapterTitle,
            initialChapterNumber = chapterNumber,
            initialImages = images,
        )
    }

    val totalContinuousCount = continuousItems.size
    val activeContinuousPage by remember(continuousItems) {
        derivedStateOf {
            if (continuousItems.isEmpty()) return@derivedStateOf null
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                val idx = listState.firstVisibleItemIndex.coerceIn(0, (continuousItems.size - 1).coerceAtLeast(0))
                return@derivedStateOf continuousItems.getOrNull(idx) as? ReaderContinuousItem.Page
            }

            // 1. Jika sudah di ujung bawah scroll (tidak bisa forward), pakai halaman terakhir yang tampak
            if (!listState.canScrollForward) {
                val lastVisiblePage = visibleItems.asReversed().firstNotNullOfOrNull { itemInfo ->
                    continuousItems.getOrNull(itemInfo.index) as? ReaderContinuousItem.Page
                }
                if (lastVisiblePage != null) return@derivedStateOf lastVisiblePage
            }

            // 2. Ambil item di titik tengah viewport (fokus mata pembaca)
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItem = visibleItems.firstOrNull { itemInfo ->
                itemInfo.offset <= viewportCenter && (itemInfo.offset + itemInfo.size) >= viewportCenter
            }
            val centerPage = continuousItems.getOrNull(centerItem?.index ?: -1) as? ReaderContinuousItem.Page
            if (centerPage != null) return@derivedStateOf centerPage

            // 3. Fallback ke item pertama yang terlihat
            visibleItems.firstNotNullOfOrNull { itemInfo ->
                continuousItems.getOrNull(itemInfo.index) as? ReaderContinuousItem.Page
            } ?: (continuousItems.getOrNull(listState.firstVisibleItemIndex.coerceIn(0, (continuousItems.size - 1).coerceAtLeast(0))) as? ReaderContinuousItem.Page)
        }
    }
    val currentActiveChapterSlug = activeContinuousPage?.chapterSlug ?: chapterSlug
    val currentActiveChapterNumber = activeContinuousPage?.chapterNumber ?: chapterNumber
    val currentActiveChapterTitle = activeContinuousPage?.chapterTitle ?: chapterTitle
    val currentActivePageIndex = (activeContinuousPage?.pageIndex ?: 0) + 1
    val currentActivePageCount = activeContinuousPage?.totalPages ?: images.size

    // B1: indeks item halaman — paging tombol volume loncat presisi ke tepi
    // atas halaman berikut/sebelumnya (divider & end-of-series dilewati).
    val pageItemIndices = remember(continuousItems) {
        continuousItems.indices.filter { continuousItems[it] is ReaderContinuousItem.Page }
    }
    DisposableEffect(pageItemIndices) {
        fun volumeStepPages(forward: Boolean) {
            scope.launch {
                if (pageItemIndices.isEmpty()) return@launch
                val current = listState.firstVisibleItemIndex
                val anchorPos = pageItemIndices.binarySearch(current).let { if (it >= 0) it else -it - 1 }
                val anchorIdx = pageItemIndices.getOrNull(anchorPos)
                val target = if (forward) {
                    // Maju: halaman setelah anchor (tepi atas baru, satu tekan = satu halaman).
                    pageItemIndices.getOrNull(anchorPos + 1) ?: return@launch
                } else {
                    when {
                        anchorIdx == null -> return@launch

                        // Item teratas divider: mundur ke halaman sebelum divider.
                        anchorIdx > current -> pageItemIndices.getOrNull(anchorPos - 1) ?: anchorIdx

                        // Tengah halaman: rapikan dulu ke tepi atas halaman ini.
                        listState.firstVisibleItemScrollOffset > 48 -> anchorIdx

                        // Sudah rapi di atas: halaman sebelumnya.
                        else -> pageItemIndices.getOrNull(anchorPos - 1) ?: anchorIdx
                    }
                }
                listState.animateScrollToItem(target)
            }
        }
        ReaderVolumeKeys.onPageStep = { forward -> volumeStepPages(forward) }
        ReaderVolumeKeys.isActive = true
        onDispose {
            ReaderVolumeKeys.onPageStep = null
            ReaderVolumeKeys.isActive = false
        }
    }

    val activeChapterIndex = remember(chapterList, currentActiveChapterSlug) {
        val idx = chapterList.indexOfFirst { isChapterSlugMatch(it, currentActiveChapterSlug) }
        if (idx >= 0) idx else 0
    }

    val nextChapterItem = remember(comicDetails, currentActiveChapterSlug) {
        comicDetails?.chapterList?.let { resolveNextChapterItem(it, currentActiveChapterSlug) }
    }
    val nextChapterSlug = remember(nextChapterItem) {
        nextChapterItem?.let { it.id.substringAfterLast(':') }
    }

    val prevChapterItem = remember(comicDetails, currentActiveChapterSlug) {
        comicDetails?.chapterList?.let { resolvePrevChapterItem(it, currentActiveChapterSlug) }
    }
    val prevChapterSlug = remember(prevChapterItem) {
        prevChapterItem?.let { it.id.substringAfterLast(':') }
    }

    // 1. Prefetch Bab Berikutnya secara Reaktif
    ReaderContinuousPrefetcher(
        listState = listState,
        seamlessChaptersEnabled = seamlessChaptersEnabled,
        chapterList = chapterList,
        loadedChapters = loadedChapters,
        isFetchingNextChapter = isFetchingNextChapter,
        onFetchingStatusChange = { isFetchingNextChapter = it },
        slug = slug,
        source = source,
        context = context,
    )

    // 2. Tracking Progress & Smart Resume
    ReaderProgressTracker(
        slug = slug,
        chapterSlug = chapterSlug,
        currentActiveChapterSlug = currentActiveChapterSlug,
        source = source,
        totalContinuousCount = totalContinuousCount,
        continuousItems = continuousItems,
        listState = listState,
        context = context,
        onHasUserScrolled = { hasUserScrolled = true },
        onReadVersionIncrement = { readVersion++ },
    )

    LaunchedEffect(slug, chapterSlug, source) {
        try {
            val src = SourceRegistry.get(source)
            val detail = src.detail(slug)
            comicDetails = ReaderComicDetail(title = detail.title, cover = detail.cover, chapterList = src.chapters(slug))
            comicDetails?.let { detail ->
                val historyEntry = HistoryEntry(
                    comicSlug = slug,
                    title = detail.title,
                    thumbnail = detail.cover,
                    chapterSlug = chapterSlug,
                    chapterTitle = detail.chapterList.find { isChapterSlugMatch(it, chapterSlug) }?.title ?: chapterSlug,
                    source = source,
                    readAt = System.currentTimeMillis(),
                    lastPage = 1,
                    totalPages = 0,
                    scrollOffset = 0,
                )
                upsertHistory(context, historyEntry)
            }
        } catch (_: Exception) {
            comicDetails = null
        }
    }

    val layoutState = remember(images.size, showControls) {
        ReaderLayoutState(
            hasImages = images.isNotEmpty(),
            showControls = showControls,
            showChaptersDialog = false,
            showSettingsDialog = false,
        )
    }
    val bodyPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Body].orEmpty() }
    val overlayPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Overlay].orEmpty() }

    val nestedScrollConnection = remember(showControls, activeExpandedTab) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 4f) {
                    if (showControls && activeExpandedTab == null) {
                        onUpdateShowControls(false)
                    }
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (activeExpandedTab != null) {
                    onUpdateActiveExpandedTab(null)
                } else {
                    onUpdateShowControls(!showControls)
                }
            },
    ) {
        if (sheetProgress.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f * sheetProgress.value.coerceIn(0f, 1f)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onUpdateActiveExpandedTab(null) },
                    )
                    .zIndex(15f),
            )
        }

        val activeChapterStartIndex = remember(continuousItems, currentActiveChapterSlug) {
            val idx = continuousItems.indexOfFirst { (it as? ReaderContinuousItem.Page)?.chapterSlug == currentActiveChapterSlug }
            if (idx >= 0) idx else 0
        }
        val activeChapterEndIndex = remember(continuousItems, currentActiveChapterSlug) {
            val idx = continuousItems.indexOfLast { (it as? ReaderContinuousItem.Page)?.chapterSlug == currentActiveChapterSlug }
            if (idx >= 0) idx else (continuousItems.size - 1).coerceAtLeast(0)
        }

        val isTrulyAtBottom by remember(totalContinuousCount, seamlessChaptersEnabled) {
            derivedStateOf {
                if (totalContinuousCount <= 0) return@derivedStateOf false
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
                val isLastItemInList = lastVisible.index >= (totalContinuousCount - 1)
                val pastStart = if (totalContinuousCount > 1) listState.firstVisibleItemIndex > 0 else true
                val bottomReached = !listState.canScrollForward ||
                    (lastVisible.offset + lastVisible.size <= listState.layoutInfo.viewportEndOffset + 120)
                (!listState.canScrollForward && pastStart) || (isLastItemInList && pastStart && bottomReached)
            }
        }

        val onScrollTopAction: () -> Unit = {
            performReaderScrollTop(scope, listState, continuousItems, activeChapterStartIndex)
        }

        val onScrollBottomAction: () -> Unit = {
            performReaderScrollBottom(scope, listState, continuousItems, currentActiveChapterSlug, activeChapterEndIndex)
        }

        bodyPlacements.forEach { placement ->
            if (!placement.predicate(layoutState)) return@forEach
            when (placement.brick) {
                ReaderBrick.Loading -> ReaderCanvasPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )

                ReaderBrick.ImageList -> CompositionLocalProvider(LocalReaderPageReferer provides SOURCE_REFERERS[source]) {
                    ReaderImageList(
                        items = continuousItems,
                        isFetchingNextChapter = isFetchingNextChapter,
                        listState = listState,
                        context = context,
                        onNavigateNext = nextChapterSlug?.let { { onNavigateChapter(it) } },
                        onNavigatePrev = prevChapterSlug?.let { { onNavigateChapter(it) } },
                        onBackToDetail = onBack,
                        onUserInteraction = {
                            onUpdateAutoScrollPausedUntil(System.currentTimeMillis() + 1200)
                        },
                        onImageSettled = onSettledImageIncrement,
                        onPageLoaded = { idx -> loadedPageSet[idx] = true },
                    )
                }

                else -> Unit
            }
        }

        // Night Dimmer Black Overlay (0% - 70% opacity)
        if (nightDimmerAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = nightDimmerAlpha.coerceIn(0f, 0.70f)))
                    .zIndex(5f),
            )
        }

        overlayPlacements.forEach { placement ->
            if (!placement.predicate(layoutState)) return@forEach
            when (placement.brick) {
                ReaderBrick.OverlayControls -> ReaderOverlay(
                    state = ReaderState(
                        title = comicDetails?.title ?: slug,
                        chapterSlug = currentActiveChapterSlug,
                        chapterTitle = currentActiveChapterTitle,
                        chapterNumber = currentActiveChapterNumber,
                        chapterIndex = activeChapterIndex,
                        chapterCount = chapterCount,
                        pageIndex = currentActivePageIndex,
                        pageCount = currentActivePageCount,
                        isBookmarked = isBookmarked,
                        source = source,
                        showControls = showControls,
                        autoScrollActive = autoScrollActive,
                        autoNextEnabled = autoNextEnabled || seamlessChaptersEnabled,
                        comicDetails = comicDetails,
                    ),
                    actions = ReaderActions(
                        onBack = onBack,
                        onToggleBookmark = {
                            comicDetails?.let {
                                val nowBookmarked = !isBookmarked
                                onToggleBookmarkState(nowBookmarked)
                                val entry = BookmarkEntry(slug, it.title, it.cover, source, System.currentTimeMillis())
                                DbIo.launch(context) { ctx ->
                                    val dao = ComicDatabase.get(ctx).bookmarkDao()
                                    if (nowBookmarked) {
                                        dao.upsert(
                                            BookmarkEntity(
                                                comicSlug = entry.comicSlug,
                                                source = entry.source,
                                                title = entry.title,
                                                thumbnail = entry.thumbnail,
                                                createdAt = entry.createdAt,
                                            ),
                                        )
                                    } else {
                                        dao.delete(entry.comicSlug, entry.source)
                                    }
                                    DbChangeNotifier.onDbChanged(ctx)
                                }
                            }
                        },
                        onChapters = {
                            onUpdateActiveExpandedTab(if (activeExpandedTab == ReaderExpandedTab.Chapters) null else ReaderExpandedTab.Chapters)
                        },
                        onSettings = {
                            onUpdateActiveExpandedTab(if (activeExpandedTab == ReaderExpandedTab.Settings) null else ReaderExpandedTab.Settings)
                        },
                        onNavigateChapter = { nextSlug ->
                            onUpdateActiveExpandedTab(null)
                            if (autoScrollActive) onUpdateShowControls(false)
                            onNavigateChapter(nextSlug)
                        },
                        onAutoScroll = onAutoScrollToggle,
                        onToggleAutoNext = onAutoNextToggle,
                        onScrollTop = onScrollTopAction,
                        onScrollBottom = onScrollBottomAction,
                    ),
                    activeExpandedTab = activeExpandedTab,
                    onExpandTab = onUpdateActiveExpandedTab,
                    sheetProgress = sheetProgress.value,
                    comicDetails = comicDetails,
                    sourceId = source,
                    slug = slug,
                    readVersion = readVersion,
                    context = context,
                    autoScrollSpeed = autoScrollSpeed,
                    onSpeedChange = onAutoScrollSpeedChange,
                    autoNextDelaySeconds = autoNextDelaySeconds,
                    onAutoNextDelayChange = onAutoNextDelayChange,
                    immersiveFullscreen = immersiveFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    keepScreenOnEnabled = keepScreenOnEnabled,
                    onKeepScreenOnChange = onKeepScreenOnChange,
                    seamlessChaptersEnabled = seamlessChaptersEnabled,
                    onSeamlessChange = onSeamlessChange,
                    nightDimmerAlpha = nightDimmerAlpha,
                    onNightDimmerChange = onNightDimmerChange,
                    onClearFocus = { focusManager.clearFocus(true) },
                )

                else -> Unit
            }
        }

        // Tombol Bounded Scroll Up dan Down (Floating Pill di sisi kanan)
        ReaderScrollPill(
            visible = showControls && activeExpandedTab == null,
            onScrollTop = onScrollTopAction,
            onScrollBottom = onScrollBottomAction,
        )

        // Banner Countdown Auto-Next
        AutoNextCountdownBanner(
            isTrulyAtBottom = isTrulyAtBottom,
            autoNextEnabled = autoNextEnabled,
            seamlessChaptersEnabled = seamlessChaptersEnabled,
            nextChapterSlug = nextChapterSlug,
            autoNextDelaySeconds = autoNextDelaySeconds,
            slug = slug,
            chapterSlug = chapterSlug,
            onNavigateNext = { targetSlug ->
                if (autoScrollActive) onUpdateShowControls(false)
                onNavigateChapter(targetSlug)
            },
        )

        if (immersiveFullscreen && !showControls && activeExpandedTab == null) {
            AmbientStatusOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding(),
            )
        }
    }
}
