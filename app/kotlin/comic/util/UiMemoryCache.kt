package comic.util

/**
 * Tiny in-memory LRU cache intended for UI navigation smoothness.
 * Not persisted across process death.
 */
object UiMemoryCache {
    private const val DEFAULT_MAX_ENTRIES = 160

    private data class Entry(val savedAtMs: Long, val ttlMs: Long, val value: Any)

    private val cache = object : LinkedHashMap<String, Entry>(DEFAULT_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > DEFAULT_MAX_ENTRIES
    }

    @Synchronized
    fun <T> peek(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return cache[key]?.value as? T
    }

    @Synchronized
    fun isFresh(key: String): Boolean {
        val entry = cache[key] ?: return false
        val age = System.currentTimeMillis() - entry.savedAtMs
        return age <= entry.ttlMs
    }

    @Synchronized
    fun put(key: String, value: Any, ttlMs: Long) {
        cache[key] = Entry(savedAtMs = System.currentTimeMillis(), ttlMs = ttlMs, value = value)
    }

    @Synchronized
    fun invalidate(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clearAll() {
        cache.clear()
    }
}
