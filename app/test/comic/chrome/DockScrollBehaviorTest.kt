package comic.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DockScrollBehaviorTest {

    @Test
    fun fraction_calculatesCorrectly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = Animatable(0f)
        val behavior = DockScrollBehavior(
            maxOffsetPx = 200f,
            offsetAnim = anim,
            scope = scope,
        )

        assertEquals(0f, behavior.fraction, 0.001f)

        anim.snapTo(100f)
        assertEquals(0.5f, behavior.fraction, 0.001f)

        anim.snapTo(200f)
        assertEquals(1f, behavior.fraction, 0.001f)

        anim.snapTo(250f)
        assertEquals(1f, behavior.fraction, 0.001f)
    }

    @Test
    fun onPostScroll_dragsDockTowardsHiddenWhenScrollingContentUp() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = Animatable(0f)
        val behavior = DockScrollBehavior(
            maxOffsetPx = 200f,
            offsetAnim = anim,
            scope = scope,
        )

        // Scrolling content up (consumed.y is negative in Compose nested scroll)
        val consumed = behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -60f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        // Dock consumes the vertical movement and translates down
        assertEquals(-60f, consumed.y, 0.001f)
        scope.advanceUntilIdle()
        assertEquals(60f, anim.value, 0.001f)
    }

    @Test
    fun onPreScroll_dragsDockTowardsVisibleWhenScrollingContentDown() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = Animatable(150f)
        val behavior = DockScrollBehavior(
            maxOffsetPx = 200f,
            offsetAnim = anim,
            scope = scope,
        )

        // Scrolling content down (available.y is positive)
        val consumed = behavior.nestedScrollConnection.onPreScroll(
            available = Offset(0f, 50f),
            source = NestedScrollSource.UserInput,
        )

        // Dock pre-consumes the scroll and translates up (reveals)
        assertEquals(50f, consumed.y, 0.001f)
        scope.advanceUntilIdle()
        assertEquals(100f, anim.value, 0.001f)
    }

    @Test
    fun settle_onPostFling_animatesToCorrectTarget() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = Animatable(100f)
        val behavior = DockScrollBehavior(
            maxOffsetPx = 200f,
            offsetAnim = anim,
            scope = scope,
        )

        // Flung downwards (available.y positive in post fling) -> settle to visible (0f)
        behavior.nestedScrollConnection.onPostFling(
            consumed = Velocity.Zero,
            available = Velocity(0f, 600f),
        )
        scope.advanceUntilIdle()
        assertEquals(0f, anim.value, 0.001f)

        // Flung upwards (available.y negative) -> settle to hidden (200f)
        behavior.nestedScrollConnection.onPostFling(
            consumed = Velocity.Zero,
            available = Velocity(0f, -600f),
        )
        scope.advanceUntilIdle()
        assertEquals(200f, anim.value, 0.001f)
    }
}
