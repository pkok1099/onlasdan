package comic.download

import comic.db.DownloadedPageEntity
import comic.source.SOURCE_REFERERS
import comic.util.ImageIntegrityValidator
import java.io.File
import java.io.IOException
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class PageDownloadTask(
    val client: OkHttpClient,
    val dir: File,
    val images: List<String>,
    val comicSlug: String,
    val source: String,
    val chapterSlug: String,
    val chapterTitle: String,
    val isCancelled: () -> Boolean,
)

internal object PageDownloader {
    private const val MAX_RETRIES = 3

    suspend fun downloadPages(
        task: PageDownloadTask,
        pageEntities: MutableList<DownloadedPageEntity>,
        onProgress: suspend (pageIndex: Int, totalBytes: Long) -> Unit,
    ): Long? {
        var totalBytes = 0L
        val referer = SOURCE_REFERERS[task.source].orEmpty()
        for ((index, imgUrl) in task.images.withIndex()) {
            if (task.isCancelled()) return null
            val targetFile = File(task.dir, "%03d.jpg".format(index + 1))
            val pageSize = downloadPageWithRetry(task.client, imgUrl, targetFile, referer, task.isCancelled)
                ?: return null
            totalBytes += pageSize
            pageEntities.add(
                DownloadedPageEntity(task.comicSlug, task.source, task.chapterSlug, index, targetFile.absolutePath),
            )
            onProgress(index + 1, totalBytes)
        }
        return totalBytes
    }

    private suspend fun downloadPageWithRetry(
        client: OkHttpClient,
        imgUrl: String,
        targetFile: File,
        referer: String,
        isCancelled: () -> Boolean,
    ): Long? {
        var lastErr: Throwable? = null
        for (attempt in 1..MAX_RETRIES) {
            if (isCancelled()) return null
            try {
                return fetchAndValidatePage(client, imgUrl, targetFile, referer)
            } catch (e: IOException) {
                lastErr = e
                if (attempt < MAX_RETRIES) delay(500L * attempt)
            }
        }
        throw lastErr ?: IOException("Gagal mengunduh $imgUrl")
    }

    private fun fetchAndValidatePage(
        client: OkHttpClient,
        imgUrl: String,
        targetFile: File,
        referer: String,
    ): Long {
        val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        try {
            writeStreamToFile(client, imgUrl, tmpFile, referer)
            if (!ImageIntegrityValidator.isComplete(tmpFile)) {
                throw IOException("Downloaded image truncated: ${targetFile.name}")
            }
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }
            return targetFile.length()
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    private fun writeStreamToFile(
        client: OkHttpClient,
        imgUrl: String,
        tmpFile: File,
        referer: String,
    ) {
        val reqBuilder = Request.Builder()
            .url(imgUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/123.0 Mobile Safari/537.36")
        if (referer.isNotBlank()) reqBuilder.header("Referer", referer)

        client.newCall(reqBuilder.build()).execute().use { res ->
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
            val body = res.body ?: throw IOException("Empty body")
            tmpFile.outputStream().buffered().use { output -> body.byteStream().copyTo(output) }
            val expectedLen = body.contentLength()
            if (expectedLen > 0L && tmpFile.length() < expectedLen) {
                throw IOException("Incomplete download")
            }
        }
    }
}
