package com.onlasdan.comic.ui.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object SourceRegistry {
    private val _sources = mutableListOf<ComicSource>(
        ShinigamiSource(),
        KomikindoSource(),
        KomikuSource(),
        DoujindesuSource(),
    )

    val sources: List<ComicSource>
        get() = _sources.toList()

    val defaultSource: ComicSource
        get() = _sources.first()

    fun get(id: String): ComicSource = _sources.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: defaultSource

    fun contains(id: String): Boolean = _sources.any { it.id.equals(id, ignoreCase = true) }

    fun register(source: ComicSource) {
        if (_sources.none { it.id.equals(source.id, ignoreCase = true) }) {
            _sources.add(source)
        }
    }

    fun unregister(sourceId: String) {
        _sources.removeAll { it.id.equals(sourceId, ignoreCase = true) }
    }

    /**
     * Cari gabungan semua source. [CombinedSearchResult.TotalFailure] hanya bila
     * SEMUA source gagal — daftar kosong dari [CombinedSearchResult.Success]
     * selalu berarti memang tidak ada hasil, bukan kegagalan yang tersamar.
     */
    suspend fun combinedSearch(query: String, page: Int = 1): CombinedSearchResult = coroutineScope {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@coroutineScope CombinedSearchResult.Success(emptyList())

        val searchable = sources.filter { Capability.SEARCH in it.capabilities }
        val deferreds = searchable.map { src -> async { searchOrNull(src, cleanQuery, page) } }
        val results = deferreds.awaitAll()
        if (searchable.isNotEmpty() && results.all { it == null }) {
            CombinedSearchResult.TotalFailure
        } else {
            CombinedSearchResult.Success(results.filterNotNull().flatten())
        }
    }

    /** Hasil search satu source; null bila source itu gagal (bukan "tidak ada hasil"). */
    private suspend fun searchOrNull(src: ComicSource, query: String, page: Int): List<Comic>? = try {
        src.search(query = query, filters = emptyMap(), page = page).items
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

/** Hasil pencarian gabungan: sukses (boleh kosong) atau gagal total di semua source. */
sealed interface CombinedSearchResult {
    data class Success(val items: List<Comic>) : CombinedSearchResult

    data object TotalFailure : CombinedSearchResult
}
