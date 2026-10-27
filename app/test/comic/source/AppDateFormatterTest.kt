package comic.source

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDateFormatterTest {

    @Test
    fun format_nullOrBlank_returnsEmptyString() {
        assertEquals("", AppDateFormatter.format(null))
        assertEquals("", AppDateFormatter.format(""))
        assertEquals("", AppDateFormatter.format("   "))
    }

    @Test
    fun format_relativePhrases_mappedToIndonesian() {
        assertEquals("Baru saja", AppDateFormatter.format("just now"))
        assertEquals("Baru saja", AppDateFormatter.format("Baru saja"))
        assertEquals("Kemarin", AppDateFormatter.format("yesterday"))
        assertEquals("Kemarin", AppDateFormatter.format("kemarin"))
        assertEquals("5 jam lalu", AppDateFormatter.format("5 hours ago"))
        assertEquals("2 jam lalu", AppDateFormatter.format("2 jam lalu"))
        assertEquals("30 menit lalu", AppDateFormatter.format("30 mins ago"))
        assertEquals("15 menit lalu", AppDateFormatter.format("15 minutes ago"))
        assertEquals("Kemarin", AppDateFormatter.format("1 day ago"))
        assertEquals("4 hari lalu", AppDateFormatter.format("4 days ago"))
    }

    @Test
    fun formatRelativeOrDate_negativeOrZero_returnsEmptyString() {
        assertEquals("", AppDateFormatter.formatRelativeOrDate(0L))
        assertEquals("", AppDateFormatter.formatRelativeOrDate(-1000L))
    }

    @Test
    fun formatRelativeOrDate_justNow_returnsBaruSaja() {
        val now = System.currentTimeMillis()
        assertEquals("Baru saja", AppDateFormatter.formatRelativeOrDate(now - 1000L)) // 1 second ago
    }

    @Test
    fun formatRelativeOrDate_minutesAgo_returnsCorrectMinutes() {
        val now = System.currentTimeMillis()
        val fifteenMinsAgo = now - TimeUnit.MINUTES.toMillis(15)
        assertEquals("15 menit lalu", AppDateFormatter.formatRelativeOrDate(fifteenMinsAgo))
    }

    @Test
    fun formatRelativeOrDate_hoursAgo_returnsCorrectHours() {
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - TimeUnit.HOURS.toMillis(3)
        assertEquals("3 jam lalu", AppDateFormatter.formatRelativeOrDate(threeHoursAgo))
    }

    @Test
    fun formatRelativeOrDate_yesterday_returnsKemarin() {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.HOURS.toMillis(30)
        assertEquals("Kemarin", AppDateFormatter.formatRelativeOrDate(yesterday))
    }

    @Test
    fun formatRelativeOrDate_daysAgo_returnsCorrectDays() {
        val now = System.currentTimeMillis()
        val fourDaysAgo = now - TimeUnit.DAYS.toMillis(4)
        assertEquals("4 hari lalu", AppDateFormatter.formatRelativeOrDate(fourDaysAgo))
    }

    @Test
    fun formatRelativeOrDate_olderThanWeek_formatsDate() {
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)
        val formatted = AppDateFormatter.formatRelativeOrDate(tenDaysAgo)
        assertTrue(formatted.isNotBlank())
        // Should contain year or month
        assertTrue(formatted.any { it.isDigit() })
    }
}
