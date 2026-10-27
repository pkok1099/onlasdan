package comic.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderPrefetchTest {

    @Test
    fun `calculatePrefetchRange returns expected range from start`() {
        val range = calculatePrefetchRange(lastVisible = 0, totalItems = 20, prefetchCount = 8)
        assertEquals(1..8, range)
    }

    @Test
    fun `calculatePrefetchRange clamps to total items near end`() {
        val range = calculatePrefetchRange(lastVisible = 15, totalItems = 20, prefetchCount = 8)
        assertEquals(16..19, range)
    }

    @Test
    fun `calculatePrefetchRange returns null when already at the end`() {
        val range = calculatePrefetchRange(lastVisible = 19, totalItems = 20, prefetchCount = 8)
        assertNull(range)
    }

    @Test
    fun `calculatePrefetchRange returns null for invalid lastVisible or empty items`() {
        assertNull(calculatePrefetchRange(lastVisible = -1, totalItems = 20, prefetchCount = 8))
        assertNull(calculatePrefetchRange(lastVisible = 0, totalItems = 0, prefetchCount = 8))
    }
}
