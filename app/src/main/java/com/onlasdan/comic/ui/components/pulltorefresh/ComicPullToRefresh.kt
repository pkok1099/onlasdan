package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ImpactShakeDurationSec = 0.36f

@Composable
fun ComicPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: ComicPullState = rememberComicPullState(isRefreshing = isRefreshing),
    content: @Composable BoxScope.() -> Unit,
) {
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val nestedConnection = remember(state) {
        createComicNestedScrollConnection(state = state, onRefresh = { currentOnRefresh() })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .comicImpactShake(state.impactElapsed)
            .nestedScroll(nestedConnection)
            .trackPullPointer(state = state, onRelease = { currentOnRefresh() }),
    ) {
        CompositionLocalProvider(LocalComicPullState provides state) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullFeedTransform(state.springPosDp),
                content = content,
            )
        }
        ComicPullHeaderContainer(state = state)
    }
}

@Composable
private fun BoxScope.ComicPullHeaderContainer(state: ComicPullState) {
    if (state.springPosDp <= 0.5f && !state.isRefreshingVisual) return
    val containerHeight = (state.springPosDp + 18f).coerceAtLeast(0f).dp
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(containerHeight)
            .clipToBounds(),
    ) {
        ComicPullBackdrop(state = state)
        ComicPullForeground(state = state)
    }
}

private fun Modifier.pullFeedTransform(offsetDp: Float): Modifier = graphicsLayer {
    val feedScale = (1f - (offsetDp / 1800f)).coerceAtLeast(0.97f)
    translationY = offsetDp.dp.toPx()
    scaleX = feedScale
    scaleY = feedScale
}

private fun Modifier.comicImpactShake(impactElapsed: Float): Modifier = graphicsLayer {
    if (impactElapsed >= ImpactShakeDurationSec) return@graphicsLayer
    val progress = (impactElapsed / ImpactShakeDurationSec).coerceIn(0f, 1f)
    val decay = 1f - progress
    val phase = (progress * 2.5f * 2.0 * PI).toFloat()
    translationX = (-sin(phase) * 4f * decay).dp.toPx()
    translationY = (cos(phase) * 5f * decay).dp.toPx()
    rotationZ = -sin(phase) * 0.8f * decay
}

private fun Modifier.trackPullPointer(
    state: ComicPullState,
    onRelease: () -> Unit,
): Modifier = pointerInput(state) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val activeChange = event.changes.firstOrNull { it.pressed }
            if (activeChange != null) {
                state.updatePointerX(activeChange.position.x, size.width.toFloat())
            } else {
                state.onRelease(onRelease)
                break
            }
        }
    }
}
