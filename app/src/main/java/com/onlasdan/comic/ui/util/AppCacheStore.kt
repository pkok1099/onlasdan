package com.onlasdan.comic.ui.util

import android.content.Context
import coil.Coil
import com.onlasdan.comic.ui.source.SourceCache
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppCacheStore {

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kilo = 1024.0
        val mega = kilo * 1024.0
        val giga = mega * 1024.0
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / kilo)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / mega)
            else -> String.format(Locale.US, "%.2f GB", bytes / giga)
        }
    }

    private fun getDirectorySizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return try {
            dir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (_: Exception) {
            0L
        }
    }

    internal fun calculateTotalCacheBytes(context: Context): Long {
        val internal = getDirectorySizeBytes(context.cacheDir)
        val code = getDirectorySizeBytes(context.codeCacheDir)
        val external = getDirectorySizeBytes(context.externalCacheDir)
        return internal + code + external
    }

    suspend fun getCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        calculateTotalCacheBytes(context)
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearCache(context: Context): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val initialSize = calculateTotalCacheBytes(context)

            // 1. Bersihkan memory & disk cache Coil
            runCatching {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            }

            // 2. Bersihkan in-memory caches & source caches
            runCatching { SourceCache.clear() }
            runCatching { UiMemoryCache.clearAll() }

            // 3. Bersihkan konten file cache tanpa menghapus folder root sistem
            context.cacheDir?.listFiles()?.forEach { file ->
                runCatching { file.deleteRecursively() }
            }
            context.codeCacheDir?.listFiles()?.forEach { file ->
                runCatching { file.deleteRecursively() }
            }
            context.externalCacheDir?.listFiles()?.forEach { file ->
                runCatching { file.deleteRecursively() }
            }

            val remainingSize = calculateTotalCacheBytes(context)
            val freed = (initialSize - remainingSize).coerceAtLeast(0L)
            if (freed == 0L && initialSize > 0L) initialSize else freed
        }
    }
}
