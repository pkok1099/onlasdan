package com.onlasdan.comic.ui.screens.reader

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.util.markChapterRead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun ReaderProgressTracker(
    slug: String,
    chapterSlug: String,
    currentActiveChapterSlug: String,
    source: String,
    totalContinuousCount: Int,
    continuousItems: List<ReaderContinuousItem>,
    listState: LazyListState,
    context: Context,
    onHasUserScrolled: () -> Unit,
    onReadVersionIncrement: () -> Unit,
) {
    // Smart Resume: Mengembalikan exact page dan pixel scrollOffset dari riwayat tersimpan
    LaunchedEffect(slug, chapterSlug, source) {
        val db = ComicDatabase.get(context)
        val hist = withContext(Dispatchers.IO) {
            val existing = db.historyDao().getHistory(slug, source)
            if (existing != null && existing.chapterSlug == chapterSlug) existing else null
        }
        val targetPage = hist?.lastPage ?: 1
        val targetOffset = hist?.scrollOffset ?: 0
        // Resume ditunda sampai item target benar-benar terpasang (bug B16):
        // scrollToItem yang ditembak sebelum daftar selesai dibangun akan
        // ter-clamp/tertelan. Jalur resume kini hanya tracker ini — listState
        // reader tidak lagi memakai Saver (pemulih kedua).
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it >= targetPage || (it > 0 && it >= totalContinuousCount) }
        if (targetPage > 1 || targetOffset > 0) {
            runCatching { listState.scrollToItem((targetPage - 1).coerceAtLeast(0), targetOffset) }
        } else {
            runCatching { listState.scrollToItem(0, 0) }
        }
    }

    // Tandai bab sebagai sudah dibaca ketika menjadi bab aktif
    LaunchedEffect(slug, currentActiveChapterSlug, source) {
        markChapterRead(context, slug, currentActiveChapterSlug, source)
        onReadVersionIncrement()
    }

    // Deteksi interaksi scroll pengguna
    LaunchedEffect(slug, chapterSlug) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (idx, off) ->
                if (idx > 0 || off > 0) onHasUserScrolled()
            }
    }

    // Simpan progress baca & scrollOffset secara dinamis saat user membaca melintasi chapter.
    // Menggunakan debounce(300ms) untuk mencegah I/O contention berlebih selama gesture scroll aktif.
    LaunchedEffect(slug, source) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .debounce(300)
            .collect { (idx, off) ->
                val page = continuousItems.getOrNull(idx) as? ReaderContinuousItem.Page
                if (page != null) {
                    val pageNum = page.pageIndex + 1
                    val chSlug = page.chapterSlug
                    withContext(Dispatchers.IO) {
                        val db = ComicDatabase.get(context)
                        val existing = db.historyDao().getHistory(slug, source)
                        if (existing != null) {
                            db.historyDao().upsert(
                                existing.copy(
                                    chapterSlug = chSlug,
                                    chapterTitle = page.chapterTitle ?: existing.chapterTitle,
                                    lastPage = pageNum,
                                    totalPages = page.totalPages,
                                    scrollOffset = off,
                                    readAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                        markChapterRead(context, slug, chSlug, source)
                    }
                }
            }
    }
}
