package comic.components

import androidx.compose.ui.unit.dp
import comic.theme.Spacing
import comic.theme.StrokeWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GestaltUiPrinciplesTest {

    @Test
    fun chapterFilter_hasExpectedEnumValues() {
        val filters = ChapterFilter.values()
        assertEquals(3, filters.size)
        assertNotNull(ChapterFilter.valueOf("ALL"))
        assertNotNull(ChapterFilter.valueOf("UNREAD"))
        assertNotNull(ChapterFilter.valueOf("DOWNLOADED"))
    }

    @Test
    fun spacingTokens_adhereToNoirBlushDesignSystem() {
        assertEquals(2.dp, Spacing.xxs)
        assertEquals(4.dp, Spacing.xs)
        assertEquals(8.dp, Spacing.sm)
        assertEquals(12.dp, Spacing.md)
        assertEquals(16.dp, Spacing.lg)
        assertEquals(24.dp, Spacing.xl)
        assertEquals(32.dp, Spacing.xxl)
    }

    @Test
    fun strokeWidthTokens_adhereToDesignSystem() {
        assertEquals(0.5.dp, StrokeWidth.hairline)
        assertEquals(1.dp, StrokeWidth.regular)
        assertEquals(1.5.dp, StrokeWidth.thick)
    }
}
