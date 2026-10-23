package com.onlasdan.comic.ui.components

import com.onlasdan.comic.ui.util.formatChapterShort
import com.onlasdan.comic.ui.util.formatTimeCompact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShinigamiHomeComponentsTest {

    @Test
    fun formatChapterShort_formatsVariousChapterStrings() {
        assertEquals("Ch.281", formatChapterShort("Chapter 281"))
        assertEquals("Ch.222", formatChapterShort("Ch. 222"))
        assertEquals("Ch.183", formatChapterShort("Ch.183"))
        assertEquals("Ch.3680", formatChapterShort("3680"))
        assertEquals("Ch.12.5", formatChapterShort("Chapter 12.5"))
        assertEquals("", formatChapterShort(null))
        assertEquals("", formatChapterShort(""))
    }

    @Test
    fun formatTimeCompact_formatsVariousTimeStrings() {
        assertEquals("30d", formatTimeCompact("30 detik lalu"))
        assertEquals("15m", formatTimeCompact("15 menit lalu"))
        assertEquals("12j", formatTimeCompact("12 jam lalu"))
        assertEquals("5h", formatTimeCompact("5 hari lalu"))
        assertEquals("1h", formatTimeCompact("kemarin"))
        assertEquals("1h", formatTimeCompact("yesterday"))
        assertEquals("1d", formatTimeCompact("baru saja"))
        assertEquals("1d", formatTimeCompact("just now"))
        assertEquals("5h", formatTimeCompact("5h"))
        assertEquals("12j", formatTimeCompact("12j"))
        assertEquals("", formatTimeCompact(null))
        assertEquals("", formatTimeCompact(""))
    }

    @Test
    fun formatTimeCompact_parsesFullDatesToDaysSuffix() {
        // Tanggal lampau (tahun 2024) harus menghasilkan angka dengan suffix 'h'
        val formatted = formatTimeCompact("2024-11-27T09:42:17Z")
        assertTrue("Expected to end with 'h', got: $formatted", formatted.endsWith("h"))
    }
}
