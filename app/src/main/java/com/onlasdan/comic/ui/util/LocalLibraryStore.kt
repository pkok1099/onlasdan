package com.onlasdan.comic.ui.util

import android.content.Context
import androidx.core.content.edit
import com.onlasdan.comic.data.db.BookmarkEntity
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.data.db.HistoryEntity
import com.onlasdan.comic.data.db.ReadChapterEntity
import com.onlasdan.comic.ui.source.SourceRegistry
import org.json.JSONArray

data class BookmarkEntry(
    val comicSlug: String,
    val title: String,
    val thumbnail: String?,
    val source: String,
    val createdAt: Long,
)

data class HistoryEntry(
    val comicSlug: String,
    val title: String,
    val thumbnail: String?,
    val chapterSlug: String,
    val chapterTitle: String?,
    val source: String,
    val readAt: Long,
    val lastPage: Int = 0,
    val totalPages: Int = 0,
    val scrollOffset: Int = 0,
)

private const val BOOKMARKS_KEY = "bookmarks_list" // legacy
private const val HISTORY_KEY = "history_list" // legacy
private const val MIGRATION_KEY = "sqlite_migrated_v1"
private const val MIGRATION_IN_PROGRESS_KEY = "sqlite_migrating_v1"

private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

@Suppress("kotlin:S3776")
private fun ensureMigrated(context: Context) {
    val p = prefs(context)
    if (p.getBoolean(MIGRATION_KEY, false)) return
    if (p.getBoolean(MIGRATION_IN_PROGRESS_KEY, false)) return

    p.edit { putBoolean(MIGRATION_IN_PROGRESS_KEY, true) }
    DbIo.launch(context) { ctx ->
        val db = ComicDatabase.get(ctx)

        // Legacy bookmarks/history from SharedPreferences JSON
        val rawBookmarks = p.getString(BOOKMARKS_KEY, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(rawBookmarks)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val entity = BookmarkEntity(
                    comicSlug = obj.optString("comicSlug"),
                    source = obj.optString("source", "komikindo"),
                    title = obj.optString("title"),
                    thumbnail = obj.optString("thumbnail").ifBlank { null },
                    createdAt = obj.optLong("createdAt"),
                )
                if (entity.comicSlug.isNotBlank()) db.bookmarkDao().upsert(entity)
            }
        }

        val rawHistory = p.getString(HISTORY_KEY, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(rawHistory)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val entity = HistoryEntity(
                    comicSlug = obj.optString("comicSlug"),
                    source = obj.optString("source", "komikindo"),
                    title = obj.optString("title"),
                    thumbnail = obj.optString("thumbnail").ifBlank { null },
                    chapterSlug = obj.optString("chapterSlug"),
                    chapterTitle = obj.optString("chapterTitle").ifBlank { null },
                    readAt = obj.optLong("readAt"),
                )
                if (entity.comicSlug.isNotBlank()) db.historyDao().upsert(entity)
            }
        }

        // Legacy read map stored as StringSet under "read:<comicSlug>".
        // Atribusi source: catatan legacy di SharedPreferences adalah rekaman
        // historis yang benar untuk data warisan; hanya bila kuncinya tidak
        // ada, jatuh ke default aplikasi — bukan hardcoded source (bug B12).
        val selectedSource = p.getString("selected_source", null) ?: SourceRegistry.defaultSource.id
        p.all.forEach { (key, value) ->
            if (!key.startsWith("read:") || value !is Set<*>) return@forEach
            val comicSlug = key.removePrefix("read:")
            val chapters = value.filterIsInstance<String>()
            chapters.forEach { chapter ->
                if (comicSlug.isBlank() || chapter.isBlank()) return@forEach
                db.readDao().insert(
                    ReadChapterEntity(
                        comicSlug = comicSlug,
                        source = selectedSource,
                        chapterSlug = chapter,
                        readAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        p.edit {
            putBoolean(MIGRATION_KEY, true)
            putBoolean(MIGRATION_IN_PROGRESS_KEY, false)
        }

        DbChangeNotifier.onDbChanged(ctx)
    }
}

fun upsertHistory(context: Context, entry: HistoryEntry) {
    ensureMigrated(context)
    val updated = entry.copy(readAt = System.currentTimeMillis())
    DbIo.launch(context) { ctx ->
        ComicDatabase.get(ctx).historyDao().upsertPreservingProgress(
            HistoryEntity(
                comicSlug = updated.comicSlug,
                source = updated.source.lowercase().trim(),
                title = updated.title,
                thumbnail = updated.thumbnail,
                chapterSlug = updated.chapterSlug,
                chapterTitle = updated.chapterTitle,
                readAt = updated.readAt,
                lastPage = updated.lastPage,
                totalPages = updated.totalPages,
                scrollOffset = updated.scrollOffset,
            ),
        )
        DbChangeNotifier.onDbChanged(ctx)
    }
}

fun removeHistoryEntry(context: Context, entry: HistoryEntry) {
    ensureMigrated(context)
    DbIo.launch(context) { ctx ->
        ComicDatabase.get(ctx).historyDao().delete(entry.comicSlug, entry.source)
        DbChangeNotifier.onDbChanged(ctx)
    }
}

fun clearHistory(context: Context) {
    ensureMigrated(context)
    DbIo.launch(context) { ctx ->
        ComicDatabase.get(ctx).historyDao().clear()
        DbChangeNotifier.onDbChanged(ctx)
    }
}
