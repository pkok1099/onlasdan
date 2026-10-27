package comic.chrome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockScrollPhysicsTest {

    @Test
    fun dockScrollOffset_clampsBetweenZeroAndMax() {
        val maxOffset = 220f
        var currentOffset = 0f

        // Scroll down 100px
        currentOffset = (currentOffset + 100f).coerceIn(0f, maxOffset)
        assertEquals(100f, currentOffset, 0.001f)

        // Scroll down another 200px -> clamps to maxOffset
        currentOffset = (currentOffset + 200f).coerceIn(0f, maxOffset)
        assertEquals(220f, currentOffset, 0.001f)

        // Scroll up 50px -> offset decreases
        currentOffset = (currentOffset - 50f).coerceIn(0f, maxOffset)
        assertEquals(170f, currentOffset, 0.001f)

        // Scroll up 300px -> clamps to 0f
        currentOffset = (currentOffset - 300f).coerceIn(0f, maxOffset)
        assertEquals(0f, currentOffset, 0.001f)
    }

    @Test
    fun settleTarget_respectsVelocityAndMidpoint() {
        fun computeSettleTarget(offset: Float, maxOffset: Float, velocityY: Float): Float {
            return when {
                velocityY > 300f -> maxOffset // Fling down -> hide
                velocityY < -300f -> 0f       // Fling up -> show
                offset > maxOffset / 2f -> maxOffset // More than half hidden -> hide
                else -> 0f                          // Less than half hidden -> show
            }
        }

        val max = 200f
        // Strong fling down from near top
        assertEquals(200f, computeSettleTarget(20f, max, 800f), 0.001f)
        // Strong fling up from near bottom
        assertEquals(0f, computeSettleTarget(180f, max, -800f), 0.001f)
        // Gentle release past midpoint
        assertEquals(200f, computeSettleTarget(120f, max, 50f), 0.001f)
        // Gentle release before midpoint
        assertEquals(0f, computeSettleTarget(80f, max, 0f), 0.001f)
    }
}
