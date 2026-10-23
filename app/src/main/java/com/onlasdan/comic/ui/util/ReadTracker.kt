package com.onlasdan.comic.ui.util

import android.content.Context
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.data.db.ReadChapterEntity
import com.onlasdan.comic.ui.source.SourcePreferences
import kotlinx.coroutines.flow.first

fun markChapterRead(
    context: Context,
    comicSlug: String,
    chapterSlug: String,
    source: String? = null,
    lastPage: Int = 0,
    totalPages: Int = 0,
    scrollOffset: Int = 0,
) {
    if (comicSlug.isBlank() || chapterSlug.isBlank()) return

    DbIo.launch(context) { ctx ->
        // Source aktif dibaca dari DataStore (sumber kebenaran tunggal, bug
        // B12) — dulu dari SharedPreferences basi dengan default hardcoded.
        val resolvedSource = source?.ifBlank { null } ?: SourcePreferences.selectedSourceId(ctx).first()
        val normalizedSource = resolvedSource.lowercase().trim()
        val inserted = ComicDatabase.get(ctx).readDao().insertIfAbsent(
            ReadChapterEntity(
                comicSlug = comicSlug,
                source = normalizedSource,
                chapterSlug = chapterSlug,
                readAt = System.currentTimeMillis(),
                lastPage = lastPage,
                totalPages = totalPages,
                scrollOffset = scrollOffset,
            ),
        )
        if (inserted != -1L) {
            DbChangeNotifier.onDbChanged(ctx)
        }
    }
}

fun markChaptersRead(
    context: Context,
    comicSlug: String,
    chapterSlugs: List<String>,
    source: String,
) {
    if (comicSlug.isBlank() || chapterSlugs.isEmpty() || source.isBlank()) return
    val normalizedSource = source.lowercase().trim()
    val now = System.currentTimeMillis()
    val entities = chapterSlugs.map { chSlug ->
        ReadChapterEntity(
            comicSlug = comicSlug,
            source = normalizedSource,
            chapterSlug = chSlug,
            readAt = now,
        )
    }
    DbIo.launch(context) { ctx ->
        ComicDatabase.get(ctx).readDao().insertAllIfAbsent(entities)
        DbChangeNotifier.onDbChanged(ctx)
    }
}
