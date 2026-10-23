package com.onlasdan.comic.ui.util

/** Slug chapter dari id gabungan "source:slug" — seluruh string bila tanpa pemisah. */
fun chapterSlugOf(chapterId: String): String = chapterId.substringAfterLast(':')

fun getCountryFlagEmoji(type: String?): String? = when (type?.trim()?.lowercase()) {
    "kr", "manhwa" -> "🇰🇷"
    "jp", "manga" -> "🇯🇵"
    "cn", "manhua" -> "🇨🇳"
    else -> null
}

/**
 * Helper untuk memformat judul chapter menjadi format ringkas "Ch.xx"
 * Contoh: "Chapter 281" -> "Ch.281", "Ch. 222" -> "Ch.222", "3680" -> "Ch.3680"
 */
fun formatChapterShort(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    val match = Regex("""(?:chapter|ch\.?|ch)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE).find(trimmed)
    if (match != null) {
        return "Ch.${match.groupValues[1]}"
    }
    val fallbackNum = Regex("""([0-9]+(?:\.[0-9]+)?)""").find(trimmed)
    return if (fallbackNum != null) {
        "Ch.${fallbackNum.groupValues[1]}"
    } else {
        trimmed
    }
}

/**
 * Helper untuk memformat waktu rilis menjadi sangat ringkas: 1d|j|h
 * Contoh: detik -> "30d", jam -> "12j", hari/kemarin -> "5h"/"1h", tanggal lengkap -> "${days}h"
 */
fun formatTimeCompact(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim().lowercase()

    if (Regex("""^\d+[djhm]$""").matches(trimmed)) return trimmed
    if (trimmed == "just now" || trimmed == "baru saja") return "1d"
    if (trimmed == "yesterday" || trimmed == "kemarin") return "1h"

    val secMatch = Regex("""(\d+)\s*(?:detik|sec|second)""").find(trimmed)
    if (secMatch != null) return "${secMatch.groupValues[1]}d"

    val minMatch = Regex("""(\d+)\s*(?:menit|min|mins?|minute|minutes?)""").find(trimmed)
    if (minMatch != null) return "${minMatch.groupValues[1]}m"

    val hourMatch = Regex("""(\d+)\s*(?:jam|hour|hours?|hr|hrs?)""").find(trimmed)
    if (hourMatch != null) return "${hourMatch.groupValues[1]}j"

    val dayMatch = Regex("""(\d+)\s*(?:hari|day|days?)""").find(trimmed)
    if (dayMatch != null) return "${dayMatch.groupValues[1]}h"

    val parseFormats = listOf(
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US),
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US),
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US),
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US),
        java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.forLanguageTag("id-ID")),
        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.forLanguageTag("id-ID")),
    )
    for (fmt in parseFormats) {
        try {
            val date = fmt.parse(raw.trim())
            if (date != null) {
                val now = System.currentTimeMillis()
                val diffMs = now - date.time
                if (diffMs <= 0L) return "1d"
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs)
                if (days >= 1L) return "${days}h"
                val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMs)
                if (hours >= 1L) return "${hours}j"
                val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs)
                if (minutes >= 1L) return "${minutes}m"
                val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diffMs)
                return "${seconds.coerceAtLeast(1L)}d"
            }
        } catch (_: Exception) {
        }
    }

    return trimmed
}
