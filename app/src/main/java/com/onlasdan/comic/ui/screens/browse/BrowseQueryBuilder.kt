package com.onlasdan.comic.ui.screens.browse

import com.onlasdan.comic.ui.source.Filter

/**
 * Menerjemahkan state filter per-source di layar Browse menjadi pasangan
 * (query, peta filter) untuk ComicSource.search — kosakata kunci mengikuti
 * yang dibaca tiap adapter sumber ("type", "genre", "orderby", dst).
 */
internal fun buildBrowseFilterMap(
    source: String,
    activeQuery: String?,
    komiku: KomikuFilters,
    komikindo: KomikindoFilters,
    shinigami: ShinigamiFilters,
    doujindesu: DoujindesuFilters,
): Pair<String, Map<String, String>> {
    val query = activeQuery?.ifBlank { null } ?: ""
    val filters = when (source.lowercase()) {
        "doujindesu" -> doujindesuFilterMap(doujindesu)
        "komiku" -> komikuFilterMap(komiku)
        "komikindo" -> komikindoFilterMap(komikindo)
        "shinigami" -> shinigamiFilterMap(shinigami)
        else -> emptyMap()
    }
    val effectiveQuery = if (source.lowercase() == "komikindo" && query.isBlank()) {
        komikindo.title?.ifBlank { null } ?: ""
    } else {
        query
    }
    return effectiveQuery to filters
}

private fun doujindesuFilterMap(f: DoujindesuFilters): Map<String, String> = buildMap {
    putIfNotBlank("sort", f.sort)
    putIfNotBlank("type", f.type)
    putIfNotBlank("status", f.status)
    putIfNotBlank("genre", f.genre)
}

private fun komikuFilterMap(f: KomikuFilters): Map<String, String> = buildMap {
    putIfNotBlank("orderby", f.orderby)
    putIfNotBlank("tipe", f.tipe)
    putIfNotBlank("status", f.status)
    putIfNotBlank("genre", f.genre)
    putIfNotBlank("sorttime", f.sorttime)
}

private fun komikindoFilterMap(f: KomikindoFilters): Map<String, String> = buildMap {
    putIfNotBlank("type", f.type)
    putIfNotBlank("status", f.status)
    putIfNotBlank("format", f.format)
    putIfNotBlank("order", f.order)
    putMultiIfNotEmpty("genre", f.genres)
    putMultiIfNotEmpty("demografis", f.demografis)
    putMultiIfNotEmpty("konten", f.konten)
    putMultiIfNotEmpty("tema", f.tema)
}

private fun shinigamiFilterMap(f: ShinigamiFilters): Map<String, String> = buildMap {
    putIfNotBlank("sort", f.sort)
    putIfNotBlank("format", f.format)
    putIfNotBlank("status", f.status)
    putIfNotBlank("genre", f.genre)
}

private fun MutableMap<String, String>.putIfNotBlank(key: String, value: String) {
    if (value.isNotBlank()) put(key, value)
}

private fun MutableMap<String, String>.putMultiIfNotEmpty(key: String, values: Set<String>) {
    if (values.isNotEmpty()) put(key, values.joinToString(","))
}

/** Memetakan deklarasi filter sumber (ComicSource.availableFilters) ke opsi sheet Browse. */
internal fun List<Filter>.toBrowseFilterOptions(): BrowseFilterOptions {
    fun opts(vararg keys: String): List<BrowseFilterOption> = firstOrNull { it.key in keys }
        ?.options
        ?.map { BrowseFilterOption(label = it.label, value = it.id) }
        .orEmpty()

    return BrowseFilterOptions(
        genres = opts("genre"),
        demografis = opts("demografis"),
        tema = opts("tema"),
        konten = opts("konten"),
        status = opts("status"),
        type = opts("type", "tipe"),
        format = opts("format"),
        orderBy = opts("order", "orderby", "sort"),
        sortTime = opts("sorttime"),
    )
}
