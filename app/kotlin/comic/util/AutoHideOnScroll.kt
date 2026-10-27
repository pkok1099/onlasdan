package comic.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Binds header/bottom-bar visibility to scroll direction:
 * - scroll down  -> hide
 * - scroll up    -> show
 * - at top       -> force show
 *
 * Uses a small threshold to avoid flapping on tiny offset changes.
 */
@Suppress("ComposableNaming")
@Composable
fun bindAutoHideOnScroll(
    listState: LazyListState,
    headerVisible: MutableState<Boolean>? = null,
    bottomBarVisible: MutableState<Boolean>? = null,
    minDeltaPx: Int = 6,
) {
    LaunchedEffect(listState) {
        data class Pos(val index: Int, val offset: Int)
        var last: Pos? = null
        var accumulated = 0
        var lastDirectionDown: Boolean? = null
        snapshotFlow { Pos(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .distinctUntilChanged()
            .collect { pos ->
                val prev = last
                if (prev != null) {
                    // Pill: hanya tampil saat di posisi paling atas.
                    // Geser sedikit saja ke bawah -> sembunyi, tidak menutupi konten.
                    val atTop = pos.index == 0 && pos.offset == 0
                    if (atTop) {
                        headerVisible?.value = true
                        bottomBarVisible?.value = true
                        accumulated = 0
                        lastDirectionDown = null
                    } else {
                        val scrollingDown = when {
                            pos.index > prev.index -> true
                            pos.index < prev.index -> false
                            else -> pos.offset > prev.offset
                        }
                        // Pill dock (bottomBar): sembunyi saat scroll ke bawah,
                        // muncul lagi saat scroll ke atas (spec 2026-10-19 §3b).
                        // Di posisi paling atas selalu tampil.
                        bottomBarVisible?.value = !scrollingDown
                        val delta = if (pos.index != prev.index) minDeltaPx else abs(pos.offset - prev.offset)
                        if (delta > 0) {
                            if (lastDirectionDown == null || lastDirectionDown != scrollingDown) {
                                accumulated = 0
                                lastDirectionDown = scrollingDown
                            }
                            accumulated += delta
                            if (accumulated >= minDeltaPx) {
                                headerVisible?.value = !scrollingDown
                                accumulated = 0
                            }
                        }
                    }
                }
                last = pos
            }
    }
}

/**
 * Collapses header dynamically during scroll without triggering Composition phase recompositions
 * or onSizeChanged backwards writes.
 * Progress and alpha reads are deferred to Layout and Draw phases respectively.
 */
fun Modifier.collapsibleHeader(
    progress: () -> Float,
    alpha: () -> Float = progress,
): Modifier = this
    .clipToBounds()
    .graphicsLayer {
        this.alpha = alpha()
    }
    .layout { measurable, constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val placeable = measurable.measure(childConstraints)
        val p = progress().coerceIn(0f, 1f)
        val currentHeight = (placeable.height * p).roundToInt()
        layout(placeable.width, currentHeight) {
            placeable.placeRelative(0, currentHeight - placeable.height)
        }
    }
