package comic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPreferencesTest {

    @Test
    fun animationMode_containsExpectedModes() {
        val modes = AnimationMode.values().map { it.name }
        assertTrue(modes.contains("FULL"))
        assertTrue(modes.contains("MINIMAL"))
        assertEquals(2, AnimationMode.values().size)
    }

    @Test
    fun animationMode_parseFromPreferencesString_matchesFallbackRule() {
        fun parseMode(raw: String?): AnimationMode = when (raw) {
            AnimationMode.MINIMAL.name -> AnimationMode.MINIMAL
            else -> AnimationMode.FULL
        }

        assertEquals(AnimationMode.FULL, parseMode("FULL"))
        assertEquals(AnimationMode.MINIMAL, parseMode("MINIMAL"))
        assertEquals(AnimationMode.FULL, parseMode(null))
        assertEquals(AnimationMode.FULL, parseMode("UNKNOWN_VALUE"))
        assertEquals(AnimationMode.FULL, parseMode(""))
    }
}
