package com.onlasdan.comic.ui.components.pulltorefresh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicSfxCatalogTest {

    @Test
    fun `catalog contains exactly 24 authentic variants across 3 origins`() {
        assertEquals(8, ComicSfxCatalog.MANGA_VARIANTS.size)
        assertEquals(8, ComicSfxCatalog.MANHWA_VARIANTS.size)
        assertEquals(8, ComicSfxCatalog.MANHUA_VARIANTS.size)
        assertEquals(24, ComicSfxCatalog.ALL.size)
    }

    @Test
    fun `all variants have valid 3 chars, 4 stamps, and metadata`() {
        ComicSfxCatalog.ALL.forEach { sfx ->
            assertTrue(sfx.id.isNotBlank())
            assertEquals(3, sfx.chars.size)
            assertEquals(4, sfx.stamps.size)
            assertTrue(sfx.origin.isNotBlank())
            assertTrue(sfx.sublabel.isNotBlank())
            assertTrue(sfx.burstTitle.isNotBlank())
            assertTrue(sfx.burstMeaning.isNotBlank())
        }
    }

    @Test
    fun `pickNext returns a different variant than currentId`() {
        val first = ComicSfxCatalog.ALL.first()
        repeat(10) {
            val next = ComicSfxCatalog.pickNext(first.id)
            assertNotNull(next)
            assertNotEquals(first.id, next.id)
        }
    }

    @Test
    fun `spring physics steps toward target and triggers at threshold`() {
        val state = ComicPullState(densityScale = 1f)
        state.accumulateDragPx(120f)
        assertTrue(state.isDragging)
        assertTrue(state.currentPullDp >= TriggerThresholdDp)

        var refreshed = false
        state.onRelease { refreshed = true }
        assertTrue(refreshed)
        assertTrue(state.isRefreshingVisual)
        assertEquals(18, state.sparks.size)

        repeat(30) { state.stepPhysics(0.016f) }
        assertTrue(state.springPosDp > 40f)
        assertTrue(state.burstPos > 0.5f)
    }
}
