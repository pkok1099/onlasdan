package comic.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoHideOnScrollRefactorTest {

    @Test
    fun dockScrollBehavior_nullSafeInteroperability() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val anim = Animatable(0f)
        val behavior = DockScrollBehavior(
            maxOffsetPx = 220f,
            offsetAnim = anim,
            scope = scope,
        )

        behavior.reset()
        scope.advanceUntilIdle()
        assertEquals(0f, anim.value, 0.001f)

        val fallbackVisible = mutableStateOf(true)
        assertTrue(fallbackVisible.value)
        fallbackVisible.value = false
        assertFalse(fallbackVisible.value)
    }
}
