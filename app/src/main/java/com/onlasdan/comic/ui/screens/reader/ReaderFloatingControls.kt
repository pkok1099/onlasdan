package com.onlasdan.comic.ui.screens.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirAnimatedVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoxScope.ReaderScrollPill(
    visible: Boolean,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoirAnimatedVisibility(
        visible = visible,
        slideDistance = 32.dp,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 96.dp)
            .navigationBarsPadding()
            .zIndex(20f),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NoirSurface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.35f)),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                    onClick = onScrollTop,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.reader_scroll_to_top),
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.width(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp,
                )
                IconButton(
                    onClick = onScrollBottom,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.reader_scroll_to_bottom),
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

fun performReaderScrollTop(
    scope: CoroutineScope,
    listState: LazyListState,
    continuousItems: List<ReaderContinuousItem>,
    activeChapterStartIndex: Int,
) {
    scope.launch {
        runCatching {
            val currentIdx = listState.firstVisibleItemIndex
            val prevDividerIdx = continuousItems.indexOfLast {
                it is ReaderContinuousItem.ChapterDivider && continuousItems.indexOf(it) < currentIdx
            }
            if (currentIdx <= activeChapterStartIndex + 1 && prevDividerIdx >= 0) {
                val prevChapterStart = continuousItems.subList(0, prevDividerIdx)
                    .indexOfLast { it is ReaderContinuousItem.ChapterDivider }
                    .let { if (it >= 0) it + 1 else 0 }
                listState.animateScrollToItem(prevChapterStart, 0)
            } else {
                listState.animateScrollToItem(activeChapterStartIndex, 0)
            }
        }
    }
}

fun performReaderScrollBottom(
    scope: CoroutineScope,
    listState: LazyListState,
    continuousItems: List<ReaderContinuousItem>,
    currentActiveChapterSlug: String,
    activeChapterEndIndex: Int,
) {
    scope.launch {
        runCatching {
            val totalItems = continuousItems.size
            if (totalItems == 0) return@runCatching
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nextDividerIdx = continuousItems.indexOfFirst {
                it is ReaderContinuousItem.ChapterDivider && it.completedChapterSlug == currentActiveChapterSlug
            }

            if (lastVisible < activeChapterEndIndex) {
                val target = if (nextDividerIdx >= 0) nextDividerIdx else activeChapterEndIndex
                listState.animateScrollToItem(target, 100_000)
            } else if (nextDividerIdx in 0 until totalItems) {
                listState.animateScrollToItem(nextDividerIdx + 1, 0)
            } else {
                listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0), 100_000)
            }
        }
    }
}

@Composable
fun BoxScope.AutoNextCountdownBanner(
    isTrulyAtBottom: Boolean,
    autoNextEnabled: Boolean,
    seamlessChaptersEnabled: Boolean,
    nextChapterSlug: String?,
    autoNextDelaySeconds: Float,
    slug: String,
    chapterSlug: String,
    onNavigateNext: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var countdownRemainingMs by remember { mutableLongStateOf(0L) }
    var autoNextTriggered by rememberSaveable(slug, chapterSlug) { mutableStateOf(false) }
    var autoNextCancelledForThisPage by rememberSaveable(slug, chapterSlug) { mutableStateOf(false) }

    LaunchedEffect(autoNextEnabled) {
        if (autoNextEnabled) {
            autoNextTriggered = false
            autoNextCancelledForThisPage = false
        }
    }

    LaunchedEffect(
        isTrulyAtBottom,
        autoNextEnabled,
        autoNextTriggered,
        autoNextCancelledForThisPage,
        nextChapterSlug,
        autoNextDelaySeconds,
        seamlessChaptersEnabled,
    ) {
        if (seamlessChaptersEnabled || !autoNextEnabled || autoNextTriggered || autoNextCancelledForThisPage || nextChapterSlug == null) {
            countdownRemainingMs = 0L
            return@LaunchedEffect
        }

        if (isTrulyAtBottom) {
            val totalDelayMs = (autoNextDelaySeconds * 1000f).toLong().coerceIn(800L, 10_000L)
            var remaining = totalDelayMs
            while (remaining > 0L && isTrulyAtBottom && !autoNextCancelledForThisPage && !autoNextTriggered) {
                countdownRemainingMs = remaining
                delay(100L)
                remaining -= 100L
            }

            if (remaining <= 0L && isTrulyAtBottom && !autoNextCancelledForThisPage && !autoNextTriggered) {
                countdownRemainingMs = 0L
                autoNextTriggered = true
                onNavigateNext(nextChapterSlug)
            } else {
                countdownRemainingMs = 0L
            }
        } else {
            countdownRemainingMs = 0L
        }
    }

    if (countdownRemainingMs <= 0L || nextChapterSlug == null) return

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, PrimaryPink.copy(alpha = 0.6f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 84.dp)
            .navigationBarsPadding()
            .zIndex(30f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CircularProgressIndicator(
                progress = {
                    val total = (autoNextDelaySeconds * 1000f).coerceAtLeast(1000f)
                    (countdownRemainingMs.toFloat() / total).coerceIn(0f, 1f)
                },
                color = PrimaryPink,
                trackColor = DarkOutline,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            val secondsRemaining = ((countdownRemainingMs + 999L) / 1000L).toInt()
            Text(
                text = stringResource(R.string.reader_autonext_countdown, secondsRemaining),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.reader_autonext_skip),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryPink,
                modifier = Modifier
                    .clickable {
                        countdownRemainingMs = 0L
                        autoNextTriggered = true
                        onNavigateNext(nextChapterSlug)
                    }
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
            IconButton(
                onClick = {
                    countdownRemainingMs = 0L
                    autoNextCancelledForThisPage = true
                },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.reader_autonext_cancel),
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
