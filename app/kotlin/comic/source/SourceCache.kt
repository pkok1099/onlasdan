package comic.source

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import comic.ComicApp
import java.io.File
import java.lang.reflect.Type
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object SourceCache {
    private val gson = Gson()
    private val mutex = Mutex()
    private const val MAX_MEMORY_ENTRIES = 120

    // Bump when the serialized model shape changes so old payloads are never read.
    private const val DISK_CACHE_VERSION = 2

    private data class MemoryEntry(val timestamp: Long, val ttlMs: Long, val data: Any)

    private val memoryCache = object : LinkedHashMap<String, MemoryEntry>(MAX_MEMORY_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryEntry>?): Boolean = size > MAX_MEMORY_ENTRIES
    }

    private data class DiskEnvelope(val timestamp: Long, val ttlMs: Long, val payload: String)

    private fun getCacheDir(): File? = try {
        val base = ComicApp.instance.cacheDir
        File(base, "source_cache").apply { if (!exists()) mkdirs() }
    } catch (_: Exception) {
        null
    }

    private fun hashKey(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(key.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun cacheFile(dir: File, key: String): File = File(dir, "${hashKey(key)}_v$DISK_CACHE_VERSION.json")

    suspend fun <T : Any> getOrFetch(
        key: String,
        type: Type,
        ttlMs: Long = 10 * 60 * 1000L, // 10 minutes default
        fetch: suspend () -> T,
    ): T {
        val now = System.currentTimeMillis()

        // 1. Check memory cache
        mutex.withLock {
            val mem = memoryCache[key]
            if (mem != null && (now - mem.timestamp <= mem.ttlMs)) {
                @Suppress("UNCHECKED_CAST")
                return mem.data as T
            }
        }

        // 2. Check disk cache
        var diskTimestamp = now
        val diskResult = withContext(Dispatchers.IO) {
            try {
                val dir = getCacheDir() ?: return@withContext null
                val file = cacheFile(dir, key)
                if (file.exists()) {
                    val content = file.readText(Charsets.UTF_8)
                    val envelope = gson.fromJson(content, DiskEnvelope::class.java)
                    if (envelope != null && (now - envelope.timestamp <= envelope.ttlMs)) {
                        val parsed = gson.fromJson<T>(envelope.payload, type)
                        // Abaikan cache kosong peninggalan versi lama (lihat guard saat write).
                        val isEmptyResult = (parsed as? Collection<*>)?.isEmpty() == true
                        if (parsed != null && !isEmptyResult) {
                            diskTimestamp = envelope.timestamp
                            return@withContext parsed
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore disk read error and fallback to fetch
            }
            null
        }

        if (diskResult != null) {
            mutex.withLock {
                memoryCache[key] = MemoryEntry(diskTimestamp, ttlMs, diskResult)
            }
            return diskResult
        }

        // 3. Fetch fresh data
        val fresh = fetch()
        // Timestamp diambil SETELAH fetch selesai: entri tidak boleh lahir
        // sudah menua sebesar durasi fetch (bug A14).
        val fetchedAt = System.currentTimeMillis()

        // Jangan cache hasil kosong: kegagalan sesaat (network/timeout) tidak boleh
        // membeku jadi layar kosong selama TTL. Pemanggil berikutnya akan fetch ulang.
        val isEmptyResult = (fresh as? Collection<*>)?.isEmpty() == true
        if (!isEmptyResult) {
            // 4. Save to memory
            mutex.withLock {
                memoryCache[key] = MemoryEntry(fetchedAt, ttlMs, fresh)
            }

            // 5. Save to disk in background
            withContext(Dispatchers.IO) {
                try {
                    val dir = getCacheDir() ?: return@withContext
                    val file = cacheFile(dir, key)
                    val jsonPayload = gson.toJson(fresh)
                    val envelope = DiskEnvelope(fetchedAt, ttlMs, jsonPayload)
                    file.writeText(gson.toJson(envelope), Charsets.UTF_8)
                } catch (_: Exception) {
                    // Ignore disk write failure
                }
            }
        }

        return fresh
    }

    suspend fun invalidate(key: String) {
        mutex.withLock {
            memoryCache.remove(key)
        }
        withContext(Dispatchers.IO) {
            try {
                val dir = getCacheDir() ?: return@withContext
                val file = cacheFile(dir, key)
                if (file.exists()) file.delete()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            memoryCache.clear()
        }
        withContext(Dispatchers.IO) {
            try {
                getCacheDir()?.deleteRecursively()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
