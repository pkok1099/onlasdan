package com.onlasdan.comic.ui.util

import android.content.Context
import androidx.core.content.edit

fun extractSlug(url: String?): String {
    if (url.isNullOrBlank()) return ""
    val trimmed = url.trimEnd('/')
    return trimmed.substringAfterLast('/')
}

fun extractChapterNumber(title: String?): Double? {
    if (title.isNullOrBlank()) return null
    val match = Regex("(\\d+(?:\\.\\d+)?)").findAll(title).lastOrNull()
    return match?.value?.toDoubleOrNull()
}

fun extractChapterLabel(title: String?): String {
    if (title.isNullOrBlank()) return ""
    val match = Regex("(\\d+(?:\\.\\d+)?)").findAll(title).lastOrNull()
    return match?.value ?: title
}

fun getImageUrl(url: String?, isHd: Boolean): String? {
    if (url.isNullOrBlank()) return url
    return if (isHd) url.replace(Regex("-\\d+x\\d+"), "") else url
}

private fun prefs(context: Context) = context.getSharedPreferences("comic_prefs", Context.MODE_PRIVATE)

fun isBookmarked(context: Context, slug: String): Boolean = prefs(context).getBoolean("bookmark:$slug", false)

fun setBookmarked(context: Context, slug: String, value: Boolean) {
    prefs(context).edit { putBoolean("bookmark:$slug", value) }
}
