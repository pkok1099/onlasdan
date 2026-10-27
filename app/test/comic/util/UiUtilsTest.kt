package comic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiUtilsTest {

    @Test
    fun extractSlug_withFullUrlTrailingSlash_returnsSlug() {
        val url = "https://komikindo.ch/komik/one-piece-indo/"
        val slug = extractSlug(url)
        assertEquals("one-piece-indo", slug)
    }

    @Test
    fun extractSlug_withFullUrlNoTrailingSlash_returnsSlug() {
        val url = "https://komiku.org/manga/solo-leveling"
        val slug = extractSlug(url)
        assertEquals("solo-leveling", slug)
    }

    @Test
    fun extractSlug_withRawSlug_returnsSameSlug() {
        val slug = extractSlug("martial-peak")
        assertEquals("martial-peak", slug)
    }

    @Test
    fun extractSlug_withNullOrBlank_returnsEmptyString() {
        assertEquals("", extractSlug(null))
        assertEquals("", extractSlug(""))
        assertEquals("", extractSlug("   "))
    }

    @Test
    fun extractSlug_withMultipleSlashes_returnsLastSegment() {
        val url = "https://domain.com/path/to/nested/comic-title///"
        assertEquals("comic-title", extractSlug(url))
    }

    @Test
    fun extractChapterNumber_standardInteger_returnsDouble() {
        val title = "Chapter 1100"
        assertEquals(1100.0, extractChapterNumber(title)!!, 0.001)
    }

    @Test
    fun extractChapterNumber_decimalChapter_returnsDouble() {
        val title = "Chapter 105.5 Bahasa Indonesia"
        assertEquals(105.5, extractChapterNumber(title)!!, 0.001)
    }

    @Test
    fun extractChapterNumber_multipleNumbers_returnsLastNumber() {
        val title = "Season 2 Vol 3 Chapter 45.2"
        assertEquals(45.2, extractChapterNumber(title)!!, 0.001)
    }

    @Test
    fun extractChapterNumber_noNumbers_returnsNull() {
        val title = "Prolog Special"
        assertNull(extractChapterNumber(title))
    }

    @Test
    fun extractChapterNumber_nullOrBlank_returnsNull() {
        assertNull(extractChapterNumber(null))
        assertNull(extractChapterNumber(""))
        assertNull(extractChapterNumber("   "))
    }

    @Test
    fun extractChapterLabel_withNumber_returnsNumberString() {
        val title = "Chapter 1100"
        assertEquals("1100", extractChapterLabel(title))
    }

    @Test
    fun extractChapterLabel_withDecimal_returnsDecimalString() {
        val title = "Ch. 54.5 - End"
        assertEquals("54.5", extractChapterLabel(title))
    }

    @Test
    fun extractChapterLabel_noNumbers_returnsOriginalTitle() {
        val title = "Prologue"
        assertEquals("Prologue", extractChapterLabel(title))
    }

    @Test
    fun extractChapterLabel_nullOrBlank_returnsEmptyString() {
        assertEquals("", extractChapterLabel(null))
        assertEquals("", extractChapterLabel(""))
        assertEquals("", extractChapterLabel("  "))
    }

    @Test
    fun getImageUrl_hdMode_removesDimensionSuffix() {
        val raw = "https://cdn.example.com/cover-300x450.jpg"
        val result = getImageUrl(raw, isHd = true)
        assertEquals("https://cdn.example.com/cover.jpg", result)
    }

    @Test
    fun getImageUrl_nonHdMode_keepsOriginalUrl() {
        val raw = "https://cdn.example.com/cover-300x450.jpg"
        val result = getImageUrl(raw, isHd = false)
        assertEquals(raw, result)
    }

    @Test
    fun getImageUrl_nullOrBlank_returnsOriginal() {
        assertNull(getImageUrl(null, isHd = true))
        assertEquals("", getImageUrl("", isHd = true))
    }
}
