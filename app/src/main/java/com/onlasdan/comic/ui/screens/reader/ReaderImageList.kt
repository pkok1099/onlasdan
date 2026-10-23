package com.onlasdan.comic.ui.screens.reader

import android.content.Context
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.onlasdan.comic.ui.components.ReaderAutoRetryMaxAttempts
import com.onlasdan.comic.ui.components.ReaderPageSlot
import com.onlasdan.comic.ui.components.ReaderPageSlotState
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@Suppress("kotlin:S107", "kotlin:S3776")
fun ReaderImageList(
    items: List<ReaderContinuousItem>,
    isFetchingNextChapter: Boolean,
    listState: LazyListState,
    context: Context,
    onNavigateNext: (() -> Unit)? = null,
    onNavigatePrev: (() -> Unit)? = null,
    onBackToDetail: () -> Unit = {},
    onUserInteraction: () -> Unit,
    onImageSettled: () -> Unit,
    onPageLoaded: (Int) -> Unit,
) {
    val pageReferer = LocalReaderPageReferer.current
    val imageLoader = remember(context) { Coil.imageLoader(context) }
    val displayWidthPx = remember(context) { context.resources.displayMetrics.widthPixels }

    // Prefetch based on scroll position (single coroutine), not per-item effects.
    val prefetched = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(items, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible < 0 || items.isEmpty() || lastVisible < items.size - 6) return@collect
                val candidateItems = listOfNotNull(
                    items.getOrNull(lastVisible + 1),
                    items.getOrNull(lastVisible + 2),
                )
                candidateItems.filterIsInstance<ReaderContinuousItem.Page>().forEach { nextItem ->
                    if (prefetched.putIfAbsent(nextItem.imageUrl, true) == null) {
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .apply { pageReferer?.let { addHeader("Referer", it) } }
                                .data(nextItem.imageUrl)
                                .size(displayWidthPx)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build(),
                        )
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onUserInteraction()
                    waitForUpOrCancellation()
                    onUserInteraction()
                }
            },
        state = listState,
        contentPadding = PaddingValues(0.dp),
    ) {
        itemsIndexed(
            items = items,
            key = { _, itm ->
                when (itm) {
                    is ReaderContinuousItem.Page -> "${itm.chapterSlug}_${itm.pageIndex}_${itm.imageUrl}"
                    is ReaderContinuousItem.ChapterDivider -> "divider_${itm.completedChapterSlug}_${itm.nextChapterSlug}"
                    is ReaderContinuousItem.EndOfSeries -> "end_of_series"
                }
            },
            contentType = { _, itm ->
                when (itm) {
                    is ReaderContinuousItem.Page -> "reader_page"
                    is ReaderContinuousItem.ChapterDivider -> "reader_divider"
                    is ReaderContinuousItem.EndOfSeries -> "reader_end_of_series"
                }
            },
        ) { idx, item ->
            when (item) {
                is ReaderContinuousItem.Page -> {
                    var slotState by remember { mutableStateOf<ReaderPageSlotState>(ReaderPageSlotState.Loading) }
                    var retryAttempt by remember { mutableIntStateOf(0) }
                    var reloadKey by remember { mutableIntStateOf(0) }
                    val request = remember(item.imageUrl, reloadKey, pageReferer) {
                        ImageRequest.Builder(context)
                            .apply { pageReferer?.let { addHeader("Referer", it) } }
                            .data(item.imageUrl)
                            .size(displayWidthPx)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }

                    LaunchedEffect(slotState) {
                        if (slotState is ReaderPageSlotState.Retrying) {
                            delay(1500)
                            reloadKey++
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        var isLoaded by remember { mutableStateOf(false) }
                        var settledReported by remember { mutableStateOf(false) }
                        val imageAlpha by animateNoirFloatAsState(
                            targetValue = if (isLoaded) 1f else 0f,
                            animationSpec = NoirMotion.effectsFastSpring(),
                            label = "readerImageAlpha",
                        )
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = imageAlpha },
                            contentScale = ContentScale.FillWidth,
                            onState = { state ->
                                isLoaded = state is AsyncImagePainter.State.Success
                                when (state) {
                                    is AsyncImagePainter.State.Error -> {
                                        if (!settledReported) {
                                            settledReported = true
                                            onImageSettled()
                                        }
                                        val nextAttempt = retryAttempt + 1
                                        if (nextAttempt <= ReaderAutoRetryMaxAttempts) {
                                            retryAttempt = nextAttempt
                                            slotState = ReaderPageSlotState.Retrying(
                                                nextAttempt,
                                                ReaderAutoRetryMaxAttempts,
                                            )
                                        } else {
                                            slotState = ReaderPageSlotState.Error
                                            onPageLoaded(idx)
                                        }
                                    }

                                    is AsyncImagePainter.State.Success -> {
                                        if (!settledReported) {
                                            settledReported = true
                                            onImageSettled()
                                        }
                                        onPageLoaded(idx)
                                    }

                                    else -> Unit
                                }
                            },
                        )
                        if (!isLoaded) {
                            ReaderPageSlot(
                                pageNumber = item.pageIndex + 1,
                                state = slotState,
                                onRetry = {
                                    retryAttempt = 0
                                    settledReported = false
                                    slotState = ReaderPageSlotState.Loading
                                    reloadKey++
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                is ReaderContinuousItem.ChapterDivider -> {
                    SideEffect {
                        onPageLoaded(idx)
                    }
                    ChapterDividerCard(
                        completedChapterNumber = item.completedChapterNumber,
                        nextChapterNumber = item.nextChapterNumber,
                    )
                }

                is ReaderContinuousItem.EndOfSeries -> {
                    SideEffect {
                        onPageLoaded(idx)
                    }
                    EndOfChapterCard(
                        nextChapterTitle = item.nextChapterTitle,
                        prevChapterTitle = item.prevChapterTitle,
                        onNavigateNext = onNavigateNext,
                        onNavigatePrev = onNavigatePrev,
                        onBackToDetail = onBackToDetail,
                    )
                }
            }
        }

        if (isFetchingNextChapter) {
            item(key = "reader_connecting_next_chapter", contentType = "reader_connecting") {
                SideEffect {
                    onPageLoaded(items.size)
                }
                ConnectingNextChapterCard()
            }
        }
    }
}
