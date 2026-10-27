package comic.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import comic.util.NoirForceMotionScale
import comic.util.NoirMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State for physics-based continuous floating toolbar/dock scroll interaction.
 * Responds to scroll gestures with inertia, smooth continuous displacement,
 * velocity-aware fling, and natural settling.
 */
@Stable
class DockScrollBehavior(
    val maxOffsetPx: Float,
    val offsetAnim: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
) {
    private var settleJob: Job? = null

    /** Fraction hidden from 0f (fully visible) to 1f (fully collapsed). */
    val fraction: Float
        get() = if (maxOffsetPx > 0f) (offsetAnim.value / maxOffsetPx).coerceIn(0f, 1f) else 0f

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Dragging down (content moves down, available.y > 0) -> reveal dock
            if (available.y > 0f && offsetAnim.value > 0f) {
                settleJob?.cancel()
                val target = (offsetAnim.value - available.y).coerceIn(0f, maxOffsetPx)
                val consumedY = offsetAnim.value - target
                scope.launch { offsetAnim.snapTo(target) }
                return Offset(0f, consumedY)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            // Dragging up (content moves up, consumed.y < 0) -> hide dock
            if (consumed.y < 0f && offsetAnim.value < maxOffsetPx) {
                settleJob?.cancel()
                val dragDelta = -consumed.y
                val target = (offsetAnim.value + dragDelta).coerceIn(0f, maxOffsetPx)
                val consumedY = target - offsetAnim.value
                scope.launch { offsetAnim.snapTo(target) }
                return Offset(0f, -consumedY)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // Cancel any in-flight settling when a fling starts
            settleJob?.cancel()
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle(available.y)
            return Velocity.Zero
        }
    }

    fun settle(velocityY: Float) {
        settleJob?.cancel()
        val currentOffset = offsetAnim.value
        val targetOffset = when {
            velocityY < -350f -> maxOffsetPx // Flung upwards -> hide dock
            velocityY > 350f -> 0f            // Flung downwards -> reveal dock
            currentOffset > maxOffsetPx * 0.45f -> maxOffsetPx
            else -> 0f
        }

        settleJob = scope.launch {
            withContext(NoirForceMotionScale) {
                offsetAnim.animateTo(
                    targetValue = targetOffset,
                    animationSpec = NoirMotion.SpatialMedium.asFloat,
                    initialVelocity = -velocityY.coerceIn(-2500f, 2500f),
                )
            }
        }
    }

    fun reset() {
        settleJob?.cancel()
        settleJob = scope.launch {
            withContext(NoirForceMotionScale) {
                offsetAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = NoirMotion.SpatialFast.asFloat,
                )
            }
        }
    }
}

val LocalDockScrollBehavior = staticCompositionLocalOf<DockScrollBehavior?> { null }

@Composable
fun rememberDockScrollBehavior(
    maxOffsetPx: Float = 220f,
): DockScrollBehavior {
    val scope = rememberCoroutineScope()
    val offsetAnim = remember { Animatable(0f) }
    return remember(maxOffsetPx, scope) {
        DockScrollBehavior(
            maxOffsetPx = maxOffsetPx,
            offsetAnim = offsetAnim,
            scope = scope,
        )
    }
}
