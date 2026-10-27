package comic.download

import android.content.Context
import android.util.Log
import comic.db.ComicDatabase
import comic.db.DownloadDao
import comic.db.DownloadedChapterEntity
import comic.db.DownloadedPageEntity
import comic.scraper.HttpClients
import comic.source.Chapter
import comic.source.Comic
import comic.source.SourceRegistry
import comic.util.UiPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import okhttp3.Request

object ChapterDownloadManager {
    const val STATUS_QUEUED = 0
    const val STATUS_DOWNLOADING = 1
    const val STATUS_COMPLETED = 2
    const val STATUS_FAILED = 3

    private const val MAX_PARALLEL_DOWNLOADS = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Hanya satu drainer boleh berjalan dalam satu proses (beta.23): job
    // UIDT dan jalur cadangan in-process memperebutkan kunci ini; klaim DB
    // atomik tetap menjadi penjaga terakhir antar-proses.
    private val drainMutex = Mutex()

    private val httpClient = HttpClients.downloadClient

    // Penanda batal dari deleteDownload/deleteComicDownloads (bug B15):
    // dipasang sinkron saat hapus dipanggil, diperiksa processTask di awal
    // putaran halaman dan sebelum insertPages — tanpa penanda ini, insert
    // yang mendarat setelah delete menyisakan baris halaman yatim.
    private val cancelledChapters = ConcurrentHashMap.newKeySet<String>()
    private val cancelledComics = ConcurrentHashMap.newKeySet<String>()

    private fun chapterKey(source: String, comicSlug: String, chapterSlug: String) = "$source|$comicSlug|$chapterSlug"

    private fun DownloadTask.isCancelled(): Boolean = cancelledChapters.contains(chapterKey(source, comicSlug, chapterSlug)) ||
        cancelledComics.contains("$source|$comicSlug")

    private data class DownloadTask(
        val context: Context,
        val comicSlug: String,
        val comicTitle: String,
        val coverUrl: String?,
        val chapterSlug: String,
        val chapterTitle: String,
        val source: String,
    )

    // Dipanggil sekali dari ComicApp.onCreate: baris DOWNLOADING yang
    // ditinggal proses sebelumnya dikembalikan ke QUEUED, lalu job UIDT
    // dijadwalkan untuk melanjutkan sisa antrean (auto-resume beta.21).
    // Baris FAILED tidak disentuh — kegagalan nyata menunggu dicoba lagi.
    fun reconcileStuckDownloads(context: Context) {
        val appCtx = context.applicationContext
        scope.launch {
            val dao = ComicDatabase.get(appCtx).downloadDao()
            dao.requeueStuckDownloads(STATUS_DOWNLOADING, STATUS_QUEUED)
            if (dao.nextQueued(STATUS_QUEUED) != null) {
                DownloadJobService.schedule(appCtx)
                drainQueue(appCtx)
            }
        }
    }

    // Mesin antrean persisten (beta.21): menggerakkan antrean sampai DB
    // kosong; baris QUEUED diklaim dari DB (klaim atomik), jadi proses yang
    // mati di tengah tidak menghilangkan sisa antrean — pemicu berikutnya
    // melanjutkan dari DB. Mode paralel: beberapa chapter bersamaan;
    // halaman dalam satu chapter tetap berurutan. Mengembalikan false bila
    // drainer lain dalam proses ini sedang aktif — penelepon (job UIDT)
    // harus MENAHAN diri, bukan selesai: selama job aktif proses memegang
    // hak eksekusi latar (pelajaran beta.24: job yang selesai dalam 54ms
    // melepas perlindungan itu dan unduhan latar mati jaringan). Error
    // dicatat ke Logcat, tidak ditelan: antrean persisten di DB membuat
    // kegagalan di sini pulih pada pemicu berikutnya.
    @Suppress("TooGenericExceptionCaught")
    suspend fun drainQueue(context: Context): Boolean {
        val appCtx = context.applicationContext
        if (!drainMutex.tryLock()) return false
        try {
            val dao = ComicDatabase.get(appCtx).downloadDao()
            val workers = if (UiPreferences.downloadParallel(appCtx).first()) MAX_PARALLEL_DOWNLOADS else 1
            coroutineScope { repeat(workers) { launch { drainWorker(dao, appCtx) } } }
        } catch (e: Exception) {
            Log.e("ChapterDownloadManager", "drain antrean unduhan gagal", e)
            if (e is CancellationException) throw e
        } finally {
            drainMutex.unlock()
        }
        return true
    }

    // Satu pekerja drain: klaim baris QUEUED tertua satu per satu (klaim
    // atomik) dan proses sampai antrean tak lagi bisa diklaim.
    private suspend fun drainWorker(dao: DownloadDao, appCtx: Context) {
        while (true) {
            val next = dao.nextQueued(STATUS_QUEUED) ?: break
            val claimed = dao.claimChapter(next.comicSlug, next.source, next.chapterSlug, STATUS_QUEUED, STATUS_DOWNLOADING)
            if (claimed == 0) continue
            processTask(DownloadTask(appCtx, next.comicSlug, next.comicTitle, next.coverUrl, next.chapterSlug, next.chapterTitle, next.source))
        }
    }

    fun enqueue(
        context: Context,
        comic: Comic,
        chapter: Chapter,
        source: String,
    ) {
        val chSlug = if (chapter.id.contains(":")) chapter.id.substringAfterLast(':') else chapter.id
        val appCtx = context.applicationContext
        val normalizedSource = source.lowercase().trim()
        val comicSlug = comic.id
        // Enqueue baru membatalkan penanda hapus lama untuk target yang sama.
        cancelledChapters.remove(chapterKey(normalizedSource, comicSlug, chSlug))
        cancelledComics.remove("$normalizedSource|$comicSlug")

        scope.launch {
            val db = ComicDatabase.get(appCtx)
            val existing = db.downloadDao().getChapterDownload(comicSlug, normalizedSource, chSlug)
            if (existing != null && existing.status == STATUS_COMPLETED) return@launch

            val localDir = getChapterDir(appCtx, normalizedSource, comicSlug, chSlug).absolutePath
            val entity = DownloadedChapterEntity(
                comicSlug = comicSlug,
                source = normalizedSource,
                chapterSlug = chSlug,
                chapterTitle = chapter.title,
                comicTitle = comic.title,
                coverUrl = comic.cover,
                totalPages = 0,
                downloadedPages = 0,
                status = STATUS_QUEUED,
                localDir = localDir,
                sizeBytes = 0L,
                downloadedAt = System.currentTimeMillis(),
            )
            db.downloadDao().upsertChapter(entity)
            DownloadJobService.schedule(appCtx)
            scope.launch { drainQueue(appCtx) }
        }
    }

    // Penghapusan total satu chapter: baris chapter + baris halaman + folder.
    // Dipakai deleteDownload (hapus dari pustaka) dan cancelDownload
    // (batalkan yang sedang antre/jalan) — perilakunya sama, beda label.
    private suspend fun purgeChapter(context: Context, comicSlug: String, source: String, chapterSlug: String) {
        val db = ComicDatabase.get(context)
        db.downloadDao().deleteChapter(comicSlug, source, chapterSlug)
        db.downloadDao().deletePages(comicSlug, source, chapterSlug)
        getChapterDir(context, source, comicSlug, chapterSlug).deleteRecursively()
    }

    fun deleteDownload(
        context: Context,
        comicSlug: String,
        source: String,
        chapterSlug: String,
    ) {
        val appCtx = context.applicationContext
        val normalizedSource = source.lowercase().trim()
        cancelledChapters.add(chapterKey(normalizedSource, comicSlug, chapterSlug))
        scope.launch { purgeChapter(appCtx, comicSlug, normalizedSource, chapterSlug) }
    }

    // Membatalkan unduhan yang sedang antre/berjalan (backlog unduhan #1):
    // penanda batal menghentikan putaran halaman, antrean yang belum mulai
    // gugur di gerbang processTask, sisa file dibersihkan, user diberi tahu.
    fun cancelDownload(
        context: Context,
        comicSlug: String,
        source: String,
        chapterSlug: String,
    ) {
        val appCtx = context.applicationContext
        val normalizedSource = source.lowercase().trim()
        cancelledChapters.add(chapterKey(normalizedSource, comicSlug, chapterSlug))
        scope.launch {
            val entity = ComicDatabase.get(appCtx).downloadDao()
                .getChapterDownload(comicSlug, normalizedSource, chapterSlug)
            purgeChapter(appCtx, comicSlug, normalizedSource, chapterSlug)
            if (entity != null) {
                DownloadNotifier.notifyCancelled(
                    appCtx,
                    DownloadTaskInfo(normalizedSource, comicSlug, chapterSlug, entity.comicTitle, entity.chapterTitle),
                )
            }
        }
    }

    fun deleteComicDownloads(
        context: Context,
        comicSlug: String,
        source: String,
    ) {
        val appCtx = context.applicationContext
        val normalizedSource = source.lowercase().trim()
        cancelledComics.add("$normalizedSource|$comicSlug")
        scope.launch {
            val db = ComicDatabase.get(appCtx)
            db.downloadDao().deleteComicDownloads(comicSlug, normalizedSource)
            db.downloadDao().deleteComicPages(comicSlug, normalizedSource)
            val comicDir = File(appCtx.filesDir, "downloads/$normalizedSource/$comicSlug")
            comicDir.deleteRecursively()
        }
    }

    private suspend fun processTask(task: DownloadTask) {
        DownloadJobService.notifyProgress(task.chapterTitle, null, null)
        val db = ComicDatabase.get(task.context)
        val dir = getChapterDir(task.context, task.source, task.comicSlug, task.chapterSlug)
        dir.mkdirs()

        try {
            db.downloadDao().updateProgress(
                comicSlug = task.comicSlug,
                source = task.source,
                chapterSlug = task.chapterSlug,
                downloadedPages = 0,
                status = STATUS_DOWNLOADING,
                sizeBytes = 0L,
            )

            val images = try {
                SourceRegistry.get(task.source).pages("${task.comicSlug}:${task.chapterSlug}").map { it.imageUrl }
            } catch (_: Exception) {
                emptyList()
            }
            if (images.isEmpty()) {
                failTask(db, task, dir)
                return
            }

            val initialEntity = db.downloadDao().getChapterDownload(task.comicSlug, task.source, task.chapterSlug)
            if (task.isCancelled() || initialEntity == null) {
                // Dihapus saat antre/fetch berjalan (bug B15): jangan
                // menghidupkan lagi barisnya lewat upsert di bawah.
                dir.deleteRecursively()
                return
            }
            db.downloadDao().upsertChapter(initialEntity.copy(totalPages = images.size))

            val pageEntities = mutableListOf<DownloadedPageEntity>()
            val downloadTask = PageDownloadTask(
                client = httpClient,
                dir = dir,
                images = images,
                comicSlug = task.comicSlug,
                source = task.source,
                chapterSlug = task.chapterSlug,
                chapterTitle = task.chapterTitle,
                isCancelled = { task.isCancelled() },
            )
            val totalBytes = PageDownloader.downloadPages(
                task = downloadTask,
                pageEntities = pageEntities,
                onProgress = { downloadedCount, bytesSoFar ->
                    db.downloadDao().updateProgress(
                        task.comicSlug,
                        task.source,
                        task.chapterSlug,
                        downloadedCount,
                        STATUS_DOWNLOADING,
                        bytesSoFar,
                    )
                    DownloadJobService.notifyProgress(task.chapterTitle, downloadedCount, images.size)
                },
            )
            if (totalBytes == null) {
                dir.deleteRecursively()
                return
            }

            // Pemeriksaan terakhir sebelum satu-satunya titik lahirnya baris
            // halaman: penanda batal ATAU baris chapter yang sudah terhapus.
            if (task.isCancelled() ||
                db.downloadDao().getChapterDownload(task.comicSlug, task.source, task.chapterSlug) == null
            ) {
                dir.deleteRecursively()
                return
            }
            db.downloadDao().insertPages(pageEntities)
            db.downloadDao().updateProgress(
                comicSlug = task.comicSlug,
                source = task.source,
                chapterSlug = task.chapterSlug,
                downloadedPages = images.size,
                status = STATUS_COMPLETED,
                sizeBytes = totalBytes,
            )
            DownloadNotifier.notifyCompleted(
                task.context,
                DownloadTaskInfo(task.source, task.comicSlug, task.chapterSlug, task.comicTitle, task.chapterTitle),
            )
        } catch (_: Exception) {
            failTask(db, task, dir)
        }
    }

    // Kegagalan unduhan tidak meninggalkan file parsial (bug B14): folder
    // chapter dihapus agar statistik penyimpanan jujur dan unduh ulang
    // mulai dari bersih.
    private suspend fun failTask(db: ComicDatabase, task: DownloadTask, dir: File) {
        DownloadNotifier.notifyFailed(
            task.context,
            DownloadTaskInfo(task.source, task.comicSlug, task.chapterSlug, task.comicTitle, task.chapterTitle),
        )
        db.downloadDao().updateProgress(
            comicSlug = task.comicSlug,
            source = task.source,
            chapterSlug = task.chapterSlug,
            downloadedPages = 0,
            status = STATUS_FAILED,
            sizeBytes = 0L,
        )
        dir.deleteRecursively()
    }

    fun getChapterDir(context: Context, source: String, comicSlug: String, chapterSlug: String): File = File(context.filesDir, "downloads/${source.lowercase()}/$comicSlug/$chapterSlug")
}
