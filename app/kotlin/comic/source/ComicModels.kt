package comic.source

enum class Capability {
    SEARCH,
    FILTER_TYPE,
    FILTER_GENRE,
    FILTER_STATUS,
    FILTER_SORT,
    RANKING,
    LATEST,
    POPULAR,
    HD_MODE,
    BOOKMARK_SYNC,
    HERO_CAROUSEL,
    SECTION_BANNERS,
    GENRE_BROWSER,
    MANGA_FEED_LAYOUT,
}

enum class SectionType {
    HERO,
    ROW,
    GRID,

    /** Daftar berperingkat bernomor (ala halaman Peringkat web). */
    RANKED,
}

data class Comic(
    val id: String,
    val title: String,
    val cover: String? = null,
    val rating: String? = null,
    val status: String? = null,
    val type: String? = null,
    val author: String? = null,
    val illustrator: String? = null,
    val synopsis: String? = null,
    val genres: List<Genre> = emptyList(),
    val latestChapter: String? = null,
    val lastUpdated: String? = null,
    val sourceId: String? = null,
    val sourceName: String? = null,
    val groups: List<String> = emptyList(),
    val series: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val views: String? = null,
)

data class Chapter(
    val id: String,
    val number: String? = null,
    val title: String,
    val url: String? = null,
    val lastUpdated: String? = null,
    val isRead: Boolean = false,
    val downloadUrl: String? = null,
)

data class Page(val index: Int, val imageUrl: String, val headers: Map<String, String> = emptyMap())

data class Genre(val id: String, val name: String, val count: Int? = null)

data class FilterOption(val id: String, val label: String)

data class Filter(
    val key: String,
    val label: String,
    val options: List<FilterOption> = emptyList(),
    /** true = boleh pilih banyak (nilai digabung koma di filters map) */
    val multi: Boolean = false,
    /**
     * Syarat tampil/aktif, deklaratif: filter ini hanya berlaku bila
     * filter lain bernilai tertentu. Contoh: mapOf("orderby" to "meta_value_num").
     */
    val enabledIf: Map<String, String>? = null,
)

data class SortOption(val id: String, val label: String)

data class Section(
    val id: String,
    val title: String? = null,
    val type: SectionType,
    val items: List<Comic> = emptyList(),
    val hasMore: Boolean = false,
    /**
     * Filter yang dipakai saat membangun section ini (mis. mapOf("type" to "manhwa")).
     * Diteruskan ke layar browse saat tombol "Lihat Semua" diklik, sehingga
     * halaman tujuan menampilkan filter yang sama persis dengan isi section.
     * Kosong = browse umum tanpa filter (perilaku lama).
     */
    val browseFilters: Map<String, String> = emptyMap(),
)

data class Paged<T>(val items: List<T>, val page: Int, val totalPages: Int? = null, val hasNextPage: Boolean = false)

/**
 * Referer per source untuk memuat gambar chapter (reader + unduhan).
 * Satu peta data-driven untuk semua pemakai — nilai shinigami menyalin
 * yang dipakai ShinigamiApi sendiri (bug B18).
 */
val SOURCE_REFERERS: Map<String, String> = mapOf(
    "komikindo" to "https://komikindo.ch/",
    "doujindesu" to "https://doujin.desu.xxx/",
    "komiku" to "https://komiku.org/",
    "shinigami" to "https://11.shinigami.asia/",
)

/**
 * Satu-satunya rumus hasNextPage di lapisan source (bug A11/C3): total resmi
 * menang; tanpa total, halaman yang penuh dianggap masih ada lanjutannya.
 */
internal fun hasNextPage(page: Int, totalPages: Int?, itemCount: Int, pageSize: Int): Boolean = totalPages?.let { page < it } ?: (itemCount >= pageSize)
