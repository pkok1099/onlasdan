package com.onlasdan.comic.ui.screens

import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.data.db.DownloadedChapterEntity
import com.onlasdan.comic.ui.components.AppConfirmDialog
import com.onlasdan.comic.ui.components.AppToast
import com.onlasdan.comic.ui.components.EmptyState
import com.onlasdan.comic.ui.components.NoirBottomSheet
import com.onlasdan.comic.ui.download.ChapterDownloadManager
import com.onlasdan.comic.ui.source.Chapter
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.AccentPink
import com.onlasdan.comic.ui.theme.DarkOutline
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary
import com.onlasdan.comic.ui.util.DelayedDelete
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.UiPreferences
import com.onlasdan.comic.ui.util.downloadDeleteKey
import com.onlasdan.comic.ui.util.noirClickable
import kotlinx.coroutines.launch

/**
 * Halaman kelola unduhan (backlog unduhan #2): daftar komik yang punya
 * unduhan (thumbnail, judul, total) — ketuk komik membuka sheet berisi
 * chapter terunduhnya untuk dibaca, dicoba lagi, dibatalkan, atau dihapus.
 */
// Soft-delete (Fase A2): chapter yang menunggu jendela undo disembunyikan
// dari daftar; berkasnya baru dihapus setelah jendela berakhir.
@Composable
private fun rememberVisibleDownloads(downloads: List<DownloadedChapterEntity>): List<DownloadedChapterEntity> {
    val pendingDeletes by DelayedDelete.pending.collectAsStateWithLifecycle(initialValue = emptySet())
    return remember(downloads, pendingDeletes) { downloads.filter { downloadDeleteKey(it.source, it.comicSlug, it.chapterSlug) !in pendingDeletes } }
}

@Composable
fun DownloadsManagerContent(
    onReadChapter: (comicSlug: String, source: String, chapterSlug: String) -> Unit,
    modifier: Modifier = Modifier,
    onExplore: () -> Unit = {},
) {
    val context = LocalContext.current
    val db = remember { ComicDatabase.get(context) }
    val downloads by db.downloadDao().observeAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedComic by remember { mutableStateOf<Pair<String, String>?>(null) }
    var comicToDelete by remember { mutableStateOf<DownloadedChapterEntity?>(null) }
    val visibleDownloads = rememberVisibleDownloads(downloads)
    val groups = remember(visibleDownloads) { visibleDownloads.groupBy { it.source to it.comicSlug } }

    Box(modifier = modifier.fillMaxSize()) {
        DownloadsList(visibleDownloads, groups, onOpenComic = { selectedComic = it }, onExplore = onExplore)
        SelectedComicSheet(
            groups = groups,
            selectedComic = selectedComic,
            onClose = { selectedComic = null },
            onReadChapter = onReadChapter,
            onDeleteAll = { comicToDelete = it },
        )
        DeleteAllDialogHost(
            sample = comicToDelete,
            chapters = visibleDownloads.filter { it.source == comicToDelete?.source && it.comicSlug == comicToDelete?.comicSlug },
            onConfirm = { s ->
                ChapterDownloadManager.deleteComicDownloads(context, s.comicSlug, s.source)
                comicToDelete = null
                selectedComic = null
            },
            onDismiss = { comicToDelete = null },
        )
    }
}

@Composable
private fun DeleteAllDialogHost(sample: DownloadedChapterEntity?, chapters: List<DownloadedChapterEntity>, onConfirm: (DownloadedChapterEntity) -> Unit, onDismiss: () -> Unit) {
    if (sample != null) {
        DeleteAllDownloadsDialog(chapters = chapters, onConfirm = { onConfirm(sample) }, onDismiss = onDismiss)
    }
}

@Composable
private fun DownloadsList(
    downloads: List<DownloadedChapterEntity>,
    groups: Map<Pair<String, String>, List<DownloadedChapterEntity>>,
    onOpenComic: (Pair<String, String>) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { UiPreferences.setDownloadsSeenAt(context, System.currentTimeMillis()) }
    if (downloads.isEmpty()) {
        EmptyState(message = stringResource(R.string.downloads_empty), icon = Icons.Outlined.FileDownload, actionLabel = stringResource(R.string.common_explore_comics), onAction = onExplore, modifier = modifier.fillMaxSize())
        return
    }
    val totalSize = remember(downloads) { downloads.sumOf { it.sizeBytes } }
    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item(key = "mode") { DownloadModeToggle() }
        item(key = "summary") {
            Text(
                text = pluralStringResource(R.plurals.downloads_summary, downloads.size, downloads.size, groups.size, Formatter.formatFileSize(context, totalSize)),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        items(groups.entries.toList(), key = { "comic-${it.key.first}-${it.key.second}" }) { entry ->
            Box(Modifier.animateItem(fadeInSpec = NoirMotion.Effects.Float, fadeOutSpec = NoirMotion.Effects.Float, placementSpec = NoirMotion.Spatial.asOffset)) {
                DownloadComicCard(entry.value, onOpen = { onOpenComic(entry.key) })
            }
        }
    }
}

@Composable
private fun DownloadGroupMeta(sample: DownloadedChapterEntity, chapters: List<DownloadedChapterEntity>) {
    val context = LocalContext.current
    Text(
        pluralStringResource(
            R.plurals.downloads_group_meta,
            chapters.size,
            SourceRegistry.get(sample.source).name,
            chapters.size,
            Formatter.formatFileSize(context, chapters.sumOf { it.sizeBytes }),
        ),
        color = TextSecondary,
    )
}

@Composable
private fun DownloadCover(coverUrl: String?, sizeDp: Int) {
    AsyncImage(
        model = coverUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(6.dp)),
    )
}

@Composable
private fun DownloadComicCard(chapters: List<DownloadedChapterEntity>, onOpen: () -> Unit) {
    val context = LocalContext.current
    val sample = chapters.first()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noirClickable { onOpen() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadCover(sample.coverUrl, 48)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sample.comicTitle, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            DownloadGroupMeta(sample, chapters)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.downloads_open_chapters),
            tint = TextSecondary,
        )
    }
}

@Composable
private fun DownloadSheetHeader(chapters: List<DownloadedChapterEntity>, onDeleteAll: () -> Unit) {
    val context = LocalContext.current
    val sample = chapters.first()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadCover(sample.coverUrl, 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sample.comicTitle, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            DownloadGroupMeta(sample, chapters)
        }
        IconButton(onClick = onDeleteAll, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.downloads_delete_all), tint = TextSecondary)
        }
    }
}

@Composable
private fun DownloadComicSheet(
    chapters: List<DownloadedChapterEntity>,
    onReadChapter: (String, String, String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DownloadSheetHeader(chapters, onDeleteAll)
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(chapters, key = { "dl-${it.source}-${it.comicSlug}-${it.chapterSlug}" }) { entity ->
                Box(
                    Modifier.animateItem(
                        fadeInSpec = NoirMotion.Effects.Float,
                        fadeOutSpec = NoirMotion.Effects.Float,
                        placementSpec = NoirMotion.Spatial.asOffset,
                    ),
                ) {
                    DownloadChapterRow(entity, onRead = { onReadChapter(entity.comicSlug, entity.source, entity.chapterSlug) })
                }
            }
        }
    }
}

@Composable
private fun DownloadChapterRow(entity: DownloadedChapterEntity, onRead: () -> Unit) {
    val completed = entity.status == ChapterDownloadManager.STATUS_COMPLETED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp, end = 4.dp)
            .then(if (completed) Modifier.noirClickable { onRead() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(entity.chapterTitle, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            DownloadStatusLine(entity)
        }
        DownloadRowActions(entity)
    }
}

@Composable
private fun DownloadStatusLine(entity: DownloadedChapterEntity) {
    val context = LocalContext.current
    val text = when (entity.status) {
        ChapterDownloadManager.STATUS_QUEUED -> stringResource(R.string.downloads_status_queued)

        ChapterDownloadManager.STATUS_DOWNLOADING ->
            pluralStringResource(R.plurals.downloads_status_downloading, entity.totalPages, entity.downloadedPages, entity.totalPages)

        ChapterDownloadManager.STATUS_COMPLETED ->
            pluralStringResource(R.plurals.downloads_status_completed, entity.totalPages, entity.totalPages, Formatter.formatFileSize(context, entity.sizeBytes))

        else -> stringResource(R.string.downloads_status_failed)
    }
    Text(text, color = if (entity.status == ChapterDownloadManager.STATUS_FAILED) AccentPink else TextSecondary)
}

@Composable
private fun DeleteDownloadButton(entity: DownloadedChapterEntity) {
    val context = LocalContext.current
    val view = LocalView.current
    val deletedFormat = stringResource(R.string.downloads_chapter_deleted)
    val deletedFreedFormat = stringResource(R.string.downloads_chapter_deleted_freed)
    val undoText = stringResource(R.string.common_undo)
    IconButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val key = downloadDeleteKey(entity.source, entity.comicSlug, entity.chapterSlug)
            DelayedDelete.schedule(key) { ChapterDownloadManager.deleteDownload(context, entity.comicSlug, entity.source, entity.chapterSlug) }
            val message = if (entity.sizeBytes > 0L) deletedFreedFormat.format(entity.chapterTitle, Formatter.formatFileSize(context, entity.sizeBytes)) else deletedFormat.format(entity.chapterTitle)
            AppToast.action(message, undoText) { DelayedDelete.cancel(key) }
        },
        modifier = Modifier.size(48.dp),
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.downloads_delete_chapter), tint = TextSecondary)
    }
}

@Composable
private fun DownloadRowActions(entity: DownloadedChapterEntity) {
    val context = LocalContext.current
    when (entity.status) {
        ChapterDownloadManager.STATUS_COMPLETED -> DeleteDownloadButton(entity)

        ChapterDownloadManager.STATUS_FAILED -> Row {
            IconButton(
                onClick = {
                    ChapterDownloadManager.enqueue(
                        context,
                        Comic(id = entity.comicSlug, title = entity.comicTitle, cover = entity.coverUrl),
                        Chapter(id = entity.chapterSlug, title = entity.chapterTitle),
                        entity.source,
                    )
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.downloads_retry), tint = PrimaryPink)
            }
            DeleteDownloadButton(entity)
        }

        else -> DownloadCancelButton(entity)
    }
}

// Tombol batal di dalam lingkaran progres (permintaan user beta.19):
// cincin menunjukkan progres chapter, (x) di tengahnya membatalkan.
@Composable
private fun DownloadCancelButton(entity: DownloadedChapterEntity) {
    val context = LocalContext.current
    IconButton(
        onClick = { ChapterDownloadManager.cancelDownload(context, entity.comicSlug, entity.source, entity.chapterSlug) },
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (entity.status == ChapterDownloadManager.STATUS_DOWNLOADING && entity.totalPages > 0) {
                CircularProgressIndicator(
                    progress = { entity.downloadedPages.toFloat() / entity.totalPages },
                    color = PrimaryPink,
                    trackColor = DarkOutline,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                CircularProgressIndicator(color = AccentPink, trackColor = DarkOutline, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.detail_cancel_download), tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DeleteAllDownloadsDialog(
    chapters: List<DownloadedChapterEntity>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val freedFormat = stringResource(R.string.downloads_deleted_freed)
    val deletedText = stringResource(R.string.downloads_deleted)
    val totalBytes = chapters.sumOf { it.sizeBytes }
    AppConfirmDialog(
        title = stringResource(R.string.downloads_delete_all),
        message = pluralStringResource(R.plurals.downloads_delete_all_confirm, chapters.size, chapters.first().comicTitle, chapters.size, Formatter.formatFileSize(context, totalBytes)),
        confirmLabel = stringResource(R.string.detail_confirm),
        onConfirm = {
            onConfirm()
            if (totalBytes > 0L) AppToast.success(freedFormat.format(Formatter.formatFileSize(context, totalBytes))) else AppToast.show(deletedText)
        },
        onDismiss = onDismiss,
        destructive = true,
    )
}

@Composable
private fun SelectedComicSheet(
    groups: Map<Pair<String, String>, List<DownloadedChapterEntity>>,
    selectedComic: Pair<String, String>?,
    onClose: () -> Unit,
    onReadChapter: (String, String, String) -> Unit,
    onDeleteAll: (DownloadedChapterEntity) -> Unit,
) {
    val chapters = selectedComic?.let { groups[it] }.orEmpty()
    if (selectedComic != null && chapters.isNotEmpty()) {
        NoirBottomSheet(onDismiss = onClose) {
            DownloadComicSheet(
                chapters = chapters,
                onReadChapter = { slug, src, chapter ->
                    onClose()
                    onReadChapter(slug, src, chapter)
                },
                onDeleteAll = { onDeleteAll(chapters.first()) },
            )
        }
    }
}

// Opsi mode unduhan (beta.21): paralel = hingga 3 chapter bersamaan,
// nonaktif = sekuensial satu per satu. Berlaku sejak antrean berikutnya
// dijalankan job; tersimpan di UiPreferences.
@Composable
private fun DownloadModeToggle() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parallel by UiPreferences.downloadParallel(context).collectAsStateWithLifecycle(initialValue = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.downloads_parallel_title), color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.downloads_parallel_desc), color = TextSecondary)
        }
        Switch(checked = parallel, onCheckedChange = { scope.launch { UiPreferences.setDownloadParallel(context, it) } })
    }
}
