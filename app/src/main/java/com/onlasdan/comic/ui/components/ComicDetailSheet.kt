package com.onlasdan.comic.ui.components

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.BookmarkEntity
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.data.db.DownloadedChapterEntity
import com.onlasdan.comic.data.db.HistoryEntity
import com.onlasdan.comic.ui.download.ChapterDownloadManager
import com.onlasdan.comic.ui.source.*
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.DbChangeNotifier
import com.onlasdan.comic.ui.util.DelayedDelete
import com.onlasdan.comic.ui.util.chapterSlugOf
import com.onlasdan.comic.ui.util.downloadDeleteKey
import com.onlasdan.comic.ui.util.markChaptersRead
import com.onlasdan.comic.ui.util.pressScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ComicDetailSheet(
    comicId: String,
    sourceId: String,
    onDismiss: () -> Unit,
    onReadChapter: (comicId: String, chapterSlug: String) -> Unit,
    onGenreClick: ((Genre) -> Unit)? = null,
) {
    val context = LocalContext.current
    val shareComicText = stringResource(R.string.detail_share_comic)
    val shareTextFormat = stringResource(R.string.detail_share_text)
    val chapterDeletedFormat = stringResource(R.string.downloads_chapter_deleted)
    val chapterDeletedFreedFormat = stringResource(R.string.downloads_chapter_deleted_freed)
    val bookmarkRemovedFormat = stringResource(R.string.detail_bookmark_removed)
    val undoText = stringResource(R.string.common_undo)
    val view = LocalView.current
    val downloadQueuedText = stringResource(R.string.detail_download_queued)
    val downloadCancelledText = stringResource(R.string.detail_download_cancelled)
    val deletedAllDownloadsText = stringResource(R.string.detail_deleted_all_downloads)
    val freedDownloadFormat = stringResource(R.string.downloads_deleted_freed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val source = remember(sourceId) { SourceRegistry.get(sourceId) }
    val db = remember { ComicDatabase.get(context) }

    val readChapterList by db.readDao().observeReadChapters(comicId, source.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val readChapters = remember(readChapterList) { readChapterList.toSet() }
    val historyEntity by db.historyDao().observeHistory(comicId, source.id).collectAsStateWithLifecycle(initialValue = null)
    val downloads by db.downloadDao().observeDownloads(comicId, source.id).collectAsState(initial = emptyList<DownloadedChapterEntity>())
    val pendingDeletes by DelayedDelete.pending.collectAsState(initial = emptySet())
    val visibleDownloads = remember(downloads, pendingDeletes) { downloads.filter { downloadDeleteKey(it.source, it.comicSlug, it.chapterSlug) !in pendingDeletes } }
    // BUKAN derivedStateOf: visibleDownloads adalah List biasa (bukan snapshot
    // state), jadi derivedStateOf tidak pernah menghitung ulang dan peta status
    // unduhan membeku di nilai saat sheet dibuka (bug: indikator tidak berubah
    // sampai sheet ditutup-dibuka). remember ber-key menghitung ulang tepat
    // saat daftar unduhan berubah.
    val downloadMap = remember(visibleDownloads) { visibleDownloads.associateBy { it.chapterSlug } }

    var comicDetail by remember { mutableStateOf<Comic?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var synopsisExpanded by remember { mutableStateOf(false) }

    val genreFilterSupported = source.capabilities.contains(Capability.FILTER_GENRE)
    var sortAscending by remember { mutableStateOf(false) }
    var chapterQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ChapterFilter.ALL) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedChapters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMarkPreviousDialog by remember { mutableStateOf<Chapter?>(null) }
    var showDeleteDownloadsDialog by remember { mutableStateOf(false) }

    // Job muatan terakhir dibatalkan sebelum muat ulang (bug BUG-7); CE dilempar ulang, bukan ditelan.
    var loadJob by remember { mutableStateOf<Job?>(null) }
    fun loadData() {
        loadJob?.cancel()
        loadJob = scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val bookmarkedCount = withContext(Dispatchers.IO) { db.bookmarkDao().exists(comicId, source.id) }
                isBookmarked = bookmarkedCount > 0

                val detail = withContext(Dispatchers.IO) { source.detail(comicId) }
                val chs = withContext(Dispatchers.IO) { source.chapters(comicId) }
                comicDetail = detail
                chapters = chs
                isLoading = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = AppErrorHandler.toUserMessage(e)
                isLoading = false
            }
        }
    }

    LaunchedEffect(comicId, sourceId) { loadData() }

    fun bookmarkEntity(detail: Comic) = BookmarkEntity(comicSlug = comicId, source = source.id, title = detail.title, thumbnail = detail.cover, createdAt = System.currentTimeMillis())

    fun toggleBookmark() {
        val detail = comicDetail ?: return
        val wasBookmarked = isBookmarked
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        scope.launch {
            withContext(Dispatchers.IO) {
                if (wasBookmarked) db.bookmarkDao().delete(comicId, source.id) else db.bookmarkDao().upsert(bookmarkEntity(detail))
            }
            isBookmarked = !wasBookmarked
            DbChangeNotifier.onDbChanged(context)
            if (wasBookmarked) {
                AppToast.action(bookmarkRemovedFormat.format(detail.title), undoText) {
                    scope.launch {
                        withContext(Dispatchers.IO) { db.bookmarkDao().upsert(bookmarkEntity(detail)) }
                        isBookmarked = true
                        DbChangeNotifier.onDbChanged(context)
                    }
                }
            }
        }
    }

    fun shareComic() {
        val detail = comicDetail ?: return
        val shareText = shareTextFormat.format(detail.title, source.name)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, detail.title)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, shareComicText))
    }

    val unreadChapterCount = remember(chapters, readChapters) {
        chapters.count { ch ->
            val slug = chapterSlugOf(ch.id)
            slug !in readChapters
        }
    }
    val downloadedChapterCount = remember(visibleDownloads) {
        visibleDownloads.count { it.status == ChapterDownloadManager.STATUS_COMPLETED }
    }
    val totalStorageBytes = remember(visibleDownloads) {
        visibleDownloads.filter { it.status == ChapterDownloadManager.STATUS_COMPLETED }.sumOf { it.sizeBytes }
    }

    val displayedChapters = remember(chapters, sortAscending, readChapters, chapterQuery, activeFilter, downloadMap) {
        val list = if (sortAscending) chapters.reversed() else chapters
        val q = chapterQuery.trim()
        val filteredByQuery = if (q.isEmpty()) {
            list
        } else {
            list.filter { ch ->
                ch.number?.contains(q, ignoreCase = true) == true ||
                    ch.title.contains(q, ignoreCase = true)
            }
        }
        val filteredByTab = when (activeFilter) {
            ChapterFilter.ALL -> filteredByQuery

            ChapterFilter.UNREAD -> filteredByQuery.filter { ch ->
                val slug = chapterSlugOf(ch.id)
                slug !in readChapters
            }

            ChapterFilter.DOWNLOADED -> filteredByQuery.filter { ch ->
                val slug = chapterSlugOf(ch.id)
                downloadMap[slug]?.status == ChapterDownloadManager.STATUS_COMPLETED
            }
        }
        filteredByTab.map { ch ->
            val slug = chapterSlugOf(ch.id)
            ch.copy(isRead = slug in readChapters)
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused && chapters.isNotEmpty()) {
            listState.animateScrollToItem(index = 1, scrollOffset = 0)
        }
    }

    val dynamicBottomPadding: Dp = if (isSearchFocused || selectionMode) 120.dp else 48.dp

    NoirBottomSheet(
        onDismiss = onDismiss,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Kontainer utama dengan navigationBarsPadding dan imePadding agar mengecil di atas keyboard.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPink,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                val detail = comicDetail
                when {
                    isLoading -> {
                        DetailHeaderPlaceholder()
                        Column(modifier = Modifier.weight(1f)) {
                            repeat(5) { ChapterRowPlaceholder() }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ErrorState(
                                message = errorMessage ?: "Gagal memuat detail komik",
                                onRetry = { loadData() },
                            )
                        }
                    }

                    detail != null -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = dynamicBottomPadding),
                        ) {
                            item(key = "comic_header_info") {
                                DetailInfoSection(
                                    comic = detail,
                                    chapters = chapters,
                                    history = historyEntity,
                                    isBookmarked = isBookmarked,
                                    totalStorageBytes = totalStorageBytes,
                                    onDeleteAllDownloads = { showDeleteDownloadsDialog = true },
                                    synopsisExpanded = synopsisExpanded,
                                    onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                                    onReadChapter = { slug -> onReadChapter(comicId, slug) },
                                    onToggleBookmark = { toggleBookmark() },
                                    onShareComic = { shareComic() },
                                    onGenreClick = onGenreClick,
                                    genreFilterSupported = genreFilterSupported,
                                )
                            }

                            if (chapters.isNotEmpty()) {
                                stickyHeader(key = "chapter_sticky_controls") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSurface),
                                    ) {
                                        ChapterHeaderControls(
                                            totalChapterCount = chapters.size,
                                            unreadChapterCount = unreadChapterCount,
                                            downloadedChapterCount = downloadedChapterCount,
                                            activeFilter = activeFilter,
                                            onFilterChange = { activeFilter = it },
                                            selectionMode = selectionMode,
                                            onToggleSelectionMode = {
                                                selectionMode = !selectionMode
                                                if (!selectionMode) selectedChapters = emptySet()
                                            },
                                            onReadFirstChapter = {
                                                val ch1 = chapters.lastOrNull()
                                                val slug = chapterSlugOf(ch1?.id.orEmpty())
                                                if (slug.isNotBlank()) onReadChapter(comicId, slug)
                                            },
                                            onReadLatestChapter = {
                                                val chNewest = chapters.firstOrNull()
                                                val slug = chapterSlugOf(chNewest?.id.orEmpty())
                                                if (slug.isNotBlank()) onReadChapter(comicId, slug)
                                            },
                                            onToggleSort = { sortAscending = !sortAscending },
                                        )
                                        ChapterSearchBar(
                                            chapterQuery = chapterQuery,
                                            onChapterQueryChange = { chapterQuery = it },
                                            onFocusChange = { focused ->
                                                isSearchFocused = focused
                                            },
                                        )
                                    }
                                }
                            }

                            if (displayedChapters.isEmpty()) {
                                item(key = "empty_chapters") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.xl),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (chapterQuery.isNotBlank()) {
                                                stringResource(R.string.detail_empty_chapters_search)
                                            } else {
                                                stringResource(R.string.detail_empty_chapters)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = displayedChapters,
                                    key = { it.id },
                                    contentType = { "detail_chapter_row" },
                                ) { ch ->
                                    val slug = chapterSlugOf(ch.id)
                                    val isRead = slug in readChapters
                                    val dl = downloadMap[slug]
                                    val dlProgress = dl?.let { if (it.totalPages > 0) it.downloadedPages.toFloat() / it.totalPages else 0f }

                                    ChapterRowItem(
                                        chapter = ch,
                                        isRead = isRead,
                                        downloadStatus = dl?.status,
                                        downloadProgress = dlProgress,
                                        selectionMode = selectionMode,
                                        isSelected = slug in selectedChapters,
                                        onToggleSelect = {
                                            selectedChapters = if (slug in selectedChapters) selectedChapters - slug else selectedChapters + slug
                                        },
                                        onLongClick = {
                                            showMarkPreviousDialog = ch
                                        },
                                        onClick = { onReadChapter(comicId, slug) },
                                        onDownloadClick = {
                                            when (dl?.status) {
                                                ChapterDownloadManager.STATUS_COMPLETED -> {
                                                    val key = downloadDeleteKey(source.id, comicId, slug)
                                                    DelayedDelete.schedule(key) { ChapterDownloadManager.deleteDownload(context, comicId, source.id, slug) }
                                                    val size = dl?.sizeBytes ?: 0L
                                                    val message = if (size > 0L) chapterDeletedFreedFormat.format(ch.title, Formatter.formatFileSize(context, size)) else chapterDeletedFormat.format(ch.title)
                                                    AppToast.action(message, undoText) { DelayedDelete.cancel(key) }
                                                }

                                                ChapterDownloadManager.STATUS_QUEUED, ChapterDownloadManager.STATUS_DOWNLOADING -> {
                                                    ChapterDownloadManager.cancelDownload(context, comicId, source.id, slug)
                                                    AppToast.show(downloadCancelledText)
                                                }

                                                else -> comicDetail?.let {
                                                    ChapterDownloadManager.enqueue(context, it, ch, source.id)
                                                    AppToast.success(downloadQueuedText)
                                                }
                                            }
                                        },
                                        onDownloadPdf = { chapterToDownload ->
                                            val pdfUrl = chapterToDownload.downloadUrl
                                            if (!pdfUrl.isNullOrBlank()) {
                                                try {
                                                    val uri = pdfUrl.toUri()
                                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    AppToast.error("Gagal membuka link download")
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Multi-Select Bottom Bar
            if (selectionMode) {
                Surface(
                    color = DarkSurfaceElevated,
                    border = BorderStroke(StrokeWidth.regular, DarkOutline),
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.detail_selected_count, selectedChapters.size, selectedChapters.size),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryPink,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                TextButton(
                                    onClick = {
                                        val unreadSlugs = displayedChapters.filter { !it.isRead }.map {
                                            chapterSlugOf(it.id)
                                        }.toSet()
                                        selectedChapters = unreadSlugs
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_select_unread),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentPink,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        selectedChapters = displayedChapters.map {
                                            chapterSlugOf(it.id)
                                        }.toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_select_all),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentPink,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        selectionMode = false
                                        selectedChapters = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_cancel_select),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = BorderStroke(StrokeWidth.regular, DarkOutline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = selectedChapters.isNotEmpty()) {
                                        scope.launch {
                                            markChaptersRead(context, comicId, selectedChapters.toList(), source.id)
                                            selectionMode = false
                                            selectedChapters = emptySet()
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (selectedChapters.isNotEmpty()) PrimaryPink else TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.detail_mark_read_selected),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedChapters.isNotEmpty()) TextPrimary else TextSecondary,
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedChapters.isNotEmpty()) PrimaryPink else DarkSurface,
                                border = BorderStroke(
                                    StrokeWidth.regular,
                                    if (selectedChapters.isNotEmpty()) PrimaryPink else DarkOutline,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = selectedChapters.isNotEmpty()) {
                                        val currentDetail = comicDetail
                                        if (currentDetail != null) {
                                            val chsToDownload = chapters.filter {
                                                val slug = chapterSlugOf(it.id)
                                                slug in selectedChapters
                                            }
                                            chsToDownload.forEach { ChapterDownloadManager.enqueue(context, currentDetail, it, source.id) }
                                            AppToast.success(downloadQueuedText)
                                        }
                                        selectionMode = false
                                        selectedChapters = emptySet()
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = if (selectedChapters.isNotEmpty()) Color.White else TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.detail_download_selected, selectedChapters.size),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedChapters.isNotEmpty()) Color.White else TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long press dialog: "Tandai chapter sebelumnya sudah dibaca"
    showMarkPreviousDialog?.let { targetChapter ->
        MarkPreviousReadDialog(
            chapterTitle = targetChapter.title,
            onDismiss = { showMarkPreviousDialog = null },
            onConfirm = {
                val targetIdx = chapters.indexOfFirst { it.id == targetChapter.id }
                if (targetIdx >= 0) {
                    val slugsToMark = chapters.subList(targetIdx, chapters.size).map { chapterSlugOf(it.id) }
                    scope.launch { markChaptersRead(context, comicId, slugsToMark, source.id) }
                }
                showMarkPreviousDialog = null
            },
        )
    }

    // Dialog: Delete all downloads for this comic (dengan hitungan chapter + ukuran)
    if (showDeleteDownloadsDialog) {
        DetailDeleteAllDialog(
            chapterCount = downloadedChapterCount,
            sizeText = Formatter.formatFileSize(context, totalStorageBytes),
            onDismiss = { showDeleteDownloadsDialog = false },
            onConfirm = {
                ChapterDownloadManager.deleteComicDownloads(context, comicId, source.id)
                if (totalStorageBytes > 0L) AppToast.success(freedDownloadFormat.format(Formatter.formatFileSize(context, totalStorageBytes))) else AppToast.show(deletedAllDownloadsText)
                showDeleteDownloadsDialog = false
            },
        )
    }
}

/**
 * Header info komik: cover, metadata, tombol baca/bookmark, genre, sinopsis.
 */
@Composable
private fun DetailInfoSection(
    comic: Comic,
    chapters: List<Chapter>,
    history: HistoryEntity?,
    isBookmarked: Boolean,
    totalStorageBytes: Long,
    onDeleteAllDownloads: () -> Unit,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onReadChapter: (chapterSlug: String) -> Unit,
    onToggleBookmark: () -> Unit,
    onShareComic: () -> Unit,
    onGenreClick: ((Genre) -> Unit)?,
    modifier: Modifier = Modifier,
    genreFilterSupported: Boolean = true,
) {
    val context = LocalContext.current

    val readResumeText = stringResource(R.string.card_action_read_resume)
    val readStartText = stringResource(R.string.card_action_read_start)
    val readButtonLabel = remember(history, chapters, readResumeText, readStartText) {
        if (history != null) readResumeText else readStartText
    }
    val targetChapterSlug = remember(history, chapters) {
        if (history != null) {
            history.chapterSlug
        } else {
            val first = chapters.lastOrNull() ?: chapters.firstOrNull()
            val rawId = first?.id.orEmpty()
            chapterSlugOf(rawId)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Comic Info Header (Gestalt: Common Region & Proximity)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .clip(CardShape)
                    .background(DarkSurfaceElevated)
                    .border(BorderStroke(StrokeWidth.hairline, DarkOutline), CardShape),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comic.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!comic.author.isNullOrBlank()) {
                    Text(
                        text = comic.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                // Badges: type, status, rating
                Row(
                    modifier = Modifier.padding(top = Spacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!comic.type.isNullOrBlank()) {
                        CategoryBadge(type = comic.type)
                    }
                    if (!comic.status.isNullOrBlank()) {
                        StatusBadge(status = comic.status)
                    }
                    if (!comic.rating.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(StrokeWidth.hairline, DarkOutline),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentPink,
                                    modifier = Modifier.size(10.dp),
                                )
                                Text(
                                    text = comic.rating,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                    ),
                                    color = AccentPink,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!comic.rating.isNullOrBlank()) {
            Box(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                RatingCard(rating = comic.rating)
            }
        }

        // Indicator: Terakhir dibaca
        if (history != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrimaryPink.copy(alpha = 0.12f),
                border = BorderStroke(StrokeWidth.hairline, PrimaryPink.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clickable { onReadChapter(history.chapterSlug) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(18.dp),
                    )
                    val text = if (history.lastPage > 0 && history.totalPages > 0) {
                        val percent = (history.lastPage * 100) / history.totalPages
                        stringResource(
                            R.string.detail_last_read_page,
                            history.chapterTitle ?: history.chapterSlug,
                            history.lastPage,
                            history.totalPages,
                            percent,
                        )
                    } else {
                        stringResource(
                            R.string.detail_last_read_label,
                            history.chapterTitle ?: history.chapterSlug,
                        )
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = PrimaryPink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        // Progres baca (Fase D — Goal Gradient): kartu momentum di bawah
        // chip terakhir dibaca, dari data lokal yang sudah ada di layar ini.
        if (history != null) {
            DetailReadProgressCard(chapters = chapters, historyChapterSlug = history.chapterSlug)
        }

        // Indicator: Offline storage used
        if (totalStorageBytes > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(StrokeWidth.hairline, DarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = AccentPink,
                            modifier = Modifier.size(16.dp),
                        )
                        val formattedSize = Formatter.formatFileSize(context, totalStorageBytes)
                        Text(
                            text = stringResource(R.string.detail_storage_used, formattedSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onDeleteAllDownloads,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.detail_delete_all_downloads),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Action buttons: Read button & Bookmark button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                text = readButtonLabel,
                icon = Icons.Default.PlayArrow,
                onClick = {
                    if (targetChapterSlug.isNotBlank()) {
                        onReadChapter(targetChapterSlug)
                    }
                },
                modifier = Modifier.weight(1f),
            )

            // Bookmark Circle Button
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(BorderStroke(StrokeWidth.hairline, if (isBookmarked) PrimaryPink.copy(alpha = 0.6f) else DarkOutline), CircleShape),
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(if (isBookmarked) R.string.detail_bookmark_remove else R.string.detail_bookmark_add),
                    tint = if (isBookmarked) PrimaryPink else TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Share Circle Button
            IconButton(
                onClick = onShareComic,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(BorderStroke(StrokeWidth.hairline, DarkOutline), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.detail_share_comic),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Series Information Panel (authors, groups, series, characters, genres pink pills, views)
        Box(modifier = Modifier.padding(horizontal = Spacing.lg)) {
            SeriesInformationPanel(
                comic = comic,
                onGenreClick = onGenreClick,
                genreFilterSupported = genreFilterSupported,
            )
        }

        // Synopsis
        if (!comic.synopsis.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            ) {
                Text(
                    text = comic.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comic.synopsis.length > 120) {
                    Text(
                        text = stringResource(if (synopsisExpanded) R.string.detail_synopsis_less else R.string.detail_synopsis_more),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryPink,
                        modifier = Modifier
                            .clickable { onToggleSynopsis() }
                            .padding(vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}

/**
 * Kartu progres baca (Fase D — Goal Gradient): posisi bab terakhir yang
 * dibaca terhadap total bab dari daftar lokal detail. Bar tidak pernah
 * tampak 0% (minimum visual 6% begitu ada bab terbaca); teks momentum
 * berubah menjadi hitungan sisa saat bab hampir habis. Tidak dirender
 * bila bab terakhir tidak ditemukan di daftar (data basi) — chip
 * "terakhir dibaca" tetap menjadi fallback yang bersih. Daftar bab
 * terurut terbaru-dulu: indeks bab riwayat di daftar = jumlah bab yang
 * lebih baru darinya = sisa bab sesudah posisi baca terakhir.
 */
@Composable
private fun DetailReadProgressCard(chapters: List<Chapter>, historyChapterSlug: String) {
    val total = chapters.size
    val remaining = chapters.indexOfFirst { chapterSlugOf(it.id) == historyChapterSlug }
    if (total == 0 || remaining < 0) return
    val current = total - remaining
    val momentumText = when {
        remaining == 0 -> stringResource(R.string.detail_progress_finished)
        remaining <= 5 -> pluralStringResource(R.plurals.detail_progress_remaining, remaining, remaining)
        else -> pluralStringResource(R.plurals.detail_progress_chapters, total, current, total, current * 100 / total)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceElevated,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = momentumText, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            LinearProgressIndicator(
                progress = { (current.toFloat() / total).coerceIn(0.06f, 1f) },
                color = PrimaryPink,
                trackColor = DarkOutline,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

/**
 * Header kontrol daftar chapter: tombol quick jump, sortir, filter chips, dan mode multi-select.
 */
