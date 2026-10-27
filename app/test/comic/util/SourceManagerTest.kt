package comic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceManagerTest {

    @Test
    fun availableSources_containsAllFourSupportedSources() {
        assertEquals(4, SourceManager.AVAILABLE_SOURCES.size)
        assertTrue(SourceManager.AVAILABLE_SOURCES.contains("komikindo"))
        assertTrue(SourceManager.AVAILABLE_SOURCES.contains("komiku"))
        assertTrue(SourceManager.AVAILABLE_SOURCES.contains("shinigami"))
        assertTrue(SourceManager.AVAILABLE_SOURCES.contains("doujindesu"))
    }

    @Test
    fun isValidSource_exactLowercaseMatches_returnsTrue() {
        assertTrue(SourceManager.isValidSource("komikindo"))
        assertTrue(SourceManager.isValidSource("komiku"))
        assertTrue(SourceManager.isValidSource("shinigami"))
        assertTrue(SourceManager.isValidSource("doujindesu"))
    }

    @Test
    fun isValidSource_caseInsensitiveMatches_returnsTrue() {
        assertTrue(SourceManager.isValidSource("KOMIKINDO"))
        assertTrue(SourceManager.isValidSource("Komiku"))
        assertTrue(SourceManager.isValidSource("Shinigami"))
        assertTrue(SourceManager.isValidSource("DoujinDesu"))
    }

    @Test
    fun isValidSource_unknownOrEmptySources_returnsFalse() {
        assertFalse(SourceManager.isValidSource("mangadex"))
        assertFalse(SourceManager.isValidSource("unknown"))
        assertFalse(SourceManager.isValidSource(""))
        assertFalse(SourceManager.isValidSource("   "))
    }
}
