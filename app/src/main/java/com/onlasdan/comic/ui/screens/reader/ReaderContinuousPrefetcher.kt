package com.onlasdan.comic.ui.screens.reader

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.source.Chapter
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.util.extractSlug
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun ReaderContinuousPrefetcher(
    listState: LazyListState,
    seamlessChaptersEnabled: Boolean,
    chapterList: List<Chapter>,
    loadedChapters: SnapshotStateList<LoadedChapter>,
    isFetchingNextChapter: Boolean,
    onFetchingStatusChange: (Boolean) -> Unit,
    slug: String,
    source: String,
    context: Context,
) {
    LaunchedEffect(listState, seamlessChaptersEnabled, chapterList, loadedChapters.size) {
        if (!seamlessChaptersEnabled) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to total
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                val reachedThreshold = (total > 0 && lastVisible >= (total - 6).coerceAtLeast(0)) || !listState.canScrollForward
                if (reachedThreshold && !isFetchingNextChapter) {
                    val lastLoaded = loadedChapters.lastOrNull() ?: return@collect
                    if (chapterList.isEmpty()) return@collect

                    val nextItem = resolveNextChapterItem(chapterList, lastLoaded.slug)
                    if (nextItem != null) {
                        val nextSlug = nextItem.id.substringAfterLast(':')
                        if (loadedChapters.none { isChapterSlugMatch(nextItem, it.slug) }) {
                            onFetchingStatusChange(true)
                            try {
                                val images = withContext(Dispatchers.IO) {
                                    val db = ComicDatabase.get(context)
                                    val downloaded = db.downloadDao().getDownloadedPages(slug, source.lowercase().trim(), nextSlug)
                                    if (downloaded.isNotEmpty() && downloaded.all { File(it.localFilePath).exists() }) {
                                        downloaded.sortedBy { it.pageIndex }.map { "file://${it.localFilePath}" }
                                    } else {
                                        SourceRegistry.get(source).pages("$slug:$nextSlug").map { it.imageUrl }
                                    }
                                }
                                if (images.isNotEmpty()) {
                                    val currentIdx = chapterList.indexOfFirst { isChapterSlugMatch(it, nextSlug) }
                                    val nextNum = extractChapterNumber(nextItem, nextItem.title, currentIdx.coerceAtLeast(0), chapterList.size)
                                    loadedChapters.add(
                                        LoadedChapter(
                                            slug = nextSlug,
                                            title = nextItem.title,
                                            number = nextNum,
                                            images = images,
                                        ),
                                    )
                                }
                            } catch (_: Exception) {
                                // Abaikan error pada prefetch background
                            } finally {
                                onFetchingStatusChange(false)
                            }
                        }
                    }
                }
            }
    }
}
