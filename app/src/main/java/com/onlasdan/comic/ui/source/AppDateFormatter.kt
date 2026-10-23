package com.onlasdan.comic.ui.source

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

// Berbasis java.time (bug A6/A7): DateTimeFormatter immutable & thread-safe,
// menggantikan SimpleDateFormat bersama yang tidak thread-safe. Akhiran 'Z'
// kini dibaca sebagai zona UTC sungguhan lewat Instant.parse, bukan pola
// literal yang mengabaikan zona.
object AppDateFormatter {
    private val localeId = Locale.forLanguageTag("id-ID")
    private val displayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)

    // Pola tanpa penanda zona: ditafsirkan sebagai waktu lokal perangkat.
    private val localDateTimePatterns = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
    )
    private val localDatePatterns = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("d MMMM yyyy", localeId),
        DateTimeFormatter.ofPattern("d MMM yyyy", localeId),
    )

    fun formatRelativeOrDate(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs

        if (diffMs < 0L) {
            return formatDate(timestampMs)
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes menit lalu"
            hours < 24 -> "$hours jam lalu"
            hours < 48 -> "Kemarin"
            days < 7 -> "$days hari lalu"
            else -> formatDate(timestampMs)
        }
    }

    private fun formatDate(timestampMs: Long): String = displayFormatter.format(Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()))

    fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()

        // Handle common English/Indonesian relative phrases
        val lower = trimmed.lowercase()
        when {
            lower == "just now" || lower == "baru saja" -> return "Baru saja"
            lower == "yesterday" || lower == "kemarin" -> return "Kemarin"
        }

        val hoursMatch = Regex("(\\d+)\\s*(?:hours?|jam)\\s*(?:ago|lalu)?").find(lower)
        if (hoursMatch != null) {
            val h = hoursMatch.groupValues[1]
            return "$h jam lalu"
        }

        val minutesMatch = Regex("(\\d+)\\s*(?:mins?|minutes?|menit)\\s*(?:ago|lalu)?").find(lower)
        if (minutesMatch != null) {
            val m = minutesMatch.groupValues[1]
            return "$m menit lalu"
        }

        val daysMatch = Regex("(\\d+)\\s*(?:days?|hari)\\s*(?:ago|lalu)?").find(lower)
        if (daysMatch != null) {
            val d = daysMatch.groupValues[1].toIntOrNull() ?: 1
            return if (d == 1) "Kemarin" else "$d hari lalu"
        }

        val parsedMs = parseToEpochMs(trimmed)
        return if (parsedMs != null) formatRelativeOrDate(parsedMs) else trimmed
    }

    private fun parseToEpochMs(trimmed: String): Long? {
        // ISO-8601 dengan zona eksplisit ('Z' atau offset, fraksi opsional).
        try {
            return Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {
            // Lanjut ke pola lokal di bawah.
        }
        val zone = ZoneId.systemDefault()
        for (pattern in localDateTimePatterns) {
            try {
                return LocalDateTime.parse(trimmed, pattern).atZone(zone).toInstant().toEpochMilli()
            } catch (_: Exception) {
                // Coba pola berikutnya.
            }
        }
        for (pattern in localDatePatterns) {
            try {
                return LocalDate.parse(trimmed, pattern).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) {
                // Coba pola berikutnya.
            }
        }
        return null
    }
}
