package comic.source

interface ComicSource {
    val id: String
    val name: String
    val iconRes: Int
    val capabilities: Set<Capability>

    /**
     * Bentuk section Home source ini, berurutan — data untuk skeleton loading Home
     * agar blok placeholder mengikuti tata letak asli tiap source.
     */
    val homeLayout: List<SectionType>
        get() = listOf(SectionType.HERO, SectionType.ROW, SectionType.GRID)

    suspend fun home(): List<Section>
    suspend fun search(query: String, filters: Map<String, String> = emptyMap(), page: Int = 1): Paged<Comic>
    suspend fun detail(id: String): Comic
    suspend fun chapters(id: String): List<Chapter>
    suspend fun pages(chapterId: String): List<Page>

    /**
     * Memuat subfeed untuk section dan tab tertentu pada halaman Home (misal: format rekomendasi, type update).
     * Default mengembalikan Paged kosong: source tanpa subfeed tetap aman.
     */
    suspend fun sectionFeed(sectionId: String, tabId: String, page: Int = 1): Paged<Comic> = Paged(emptyList(), page, null, false)

    /**
     * Filter yang didukung source ini, dideklarasikan sebagai data.
     * UI me-render generik dari daftar ini — tanpa cabang per source.
     * Default kosong: source tanpa filter tetap jalan.
     */
    fun availableFilters(): List<Filter> = emptyList()

    /**
     * Genre unggulan untuk section "Jelajahi Genre" di home.
     * Default kosong: section disembunyikan bila source tidak mendeklarasikan.
     */
    val featuredGenres: List<Genre> get() = emptyList()

    /**
     * Seluruh genre yang didukung source ini (untuk Bottom Sheet Jelajahi Genre).
     * Default kosong: source tanpa katalog genre lengkap tetap aman.
     */
    suspend fun allGenres(): List<Genre> = emptyList()
}
