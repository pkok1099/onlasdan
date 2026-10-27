package comic.source

import comic.R
import comic.api.ApiResponse
import comic.api.ComicDetail
import comic.api.ComicListItem
import comic.api.HomeComicItem
import comic.scraper.GenreMasterData
import comic.scraper.KomikuScraper
import comic.util.extractSlug

class KomikuSource(private val scraper: KomikuScraper = KomikuScraper()) : ComicSource {

    override val id: String = "komiku"
    override val name: String = "Komiku"
    override val iconRes: Int = R.drawable.ic_source_default

    override val capabilities: Set<Capability> = setOf(
        Capability.SEARCH,
        Capability.FILTER_TYPE,
        Capability.FILTER_GENRE,
        Capability.FILTER_STATUS,
        Capability.FILTER_SORT,
        Capability.LATEST,
        Capability.POPULAR,
    )

    // Daftar statis dari komiku.org/pustaka/ (diambil 2026-10-18).
    // Bukan fetch live: sesuai keputusan PoC filter, opsi pakai daftar statis.
    private val orderbyOptions = listOf(
        FilterOption("modified", "Chapter Terbaru"),
        FilterOption("date", "Komik Terbaru"),
        FilterOption("meta_value_num", "Populer"),
        FilterOption("rand", "Acak"),
    )
    private val tipeOptions = listOf(
        FilterOption("manga", "Manga"),
        FilterOption("manhua", "Manhua"),
        FilterOption("manhwa", "Manhwa"),
    )
    private val statusOptions = listOf(
        FilterOption("ongoing", "Ongoing"),
        FilterOption("end", "Tamat"),
    )
    private val sorttimeOptions = listOf(
        FilterOption("all", "Semua Waktu"),
        FilterOption("daily", "Harian"),
        FilterOption("weekly", "Mingguan"),
    )
    private val genreOptions = GenreMasterData.komikuMasterGenres.map { FilterOption(it.id, it.name) }

    override suspend fun allGenres(): List<Genre> = GenreMasterData.komikuMasterGenres

    /**
     * Filter Komiku sebagai data. sorttime hanya aktif bila urutan = Populer
     * (meta_value_num) — deklaratif via enabledIf, tanpa cabang di UI.
     */
    override fun availableFilters(): List<Filter> = listOf(
        Filter("orderby", "Urutan", orderbyOptions),
        Filter("tipe", "Tipe", tipeOptions),
        Filter("status", "Status", statusOptions),
        Filter("genre", "Genre", genreOptions),
        Filter(
            key = "sorttime",
            label = "Periode",
            options = sorttimeOptions,
            enabledIf = mapOf("orderby" to "meta_value_num"),
        ),
    )

    private fun mapHomeItem(item: HomeComicItem): Comic {
        val slug = extractSlug(item.url)
        return Comic(
            id = if (slug.isNotBlank()) slug else item.title,
            title = item.title,
            cover = item.cover,
            type = item.type,
            latestChapter = item.latestChapter?.title,
            sourceId = id,
            sourceName = name,
        )
    }

    override suspend fun home(): List<Section> {
        val resp = scraper.getHome()
        val data = resp.data
            ?: throw RuntimeException("komiku: ${resp.error ?: "respons kosong"}")
        val sections = mutableListOf<Section>()

        val heroComic = data.heroComic?.let { hero ->
            val slug = extractSlug(hero.url)
            Comic(
                id = if (slug.isNotBlank()) slug else hero.title,
                title = hero.title,
                cover = hero.cover,
                rating = hero.rating,
                type = hero.type,
                author = hero.author,
                synopsis = hero.description,
                latestChapter = hero.latestChapter?.title,
                sourceId = id,
                sourceName = name,
            )
        } ?: (data.popularComics.firstOrNull() ?: data.latestComics.firstOrNull())?.let { mapHomeItem(it) }

        heroComic?.let { hero ->
            sections.add(
                Section(
                    id = "hero",
                    title = null,
                    type = SectionType.HERO,
                    items = listOf(hero),
                ),
            )
        }

        if (data.popularComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "popular",
                    title = "Series Populer",
                    type = SectionType.ROW,
                    items = data.popularComics.map { mapHomeItem(it) }.distinctBy { it.id },
                    hasMore = true,
                ),
            )
        }

        if (data.latestComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "latest",
                    title = "Update Terbaru",
                    type = SectionType.GRID,
                    items = data.latestComics.map { mapHomeItem(it) }.distinctBy { it.id },
                    hasMore = true,
                ),
            )
        }

        return sections
    }

    override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> {
        val cleanQuery = query.trim()
        if (cleanQuery.isNotEmpty()) {
            val resp = scraper.search(cleanQuery, page)
            resp.error?.let { throw IllegalStateException(it) }
            val data = resp.data
            val comics = data?.comics.orEmpty().map { mapListItem(it) }
            val total = data?.totalPages ?: if (comics.size >= 10) page + 1 else page
            val hasNext = hasNextPage(page, data?.totalPages, comics.size, 10)
            return Paged(items = comics, page = page, totalPages = maxOf(total, page), hasNextPage = hasNext)
        } else {
            val resp = scraper.browse(
                page = page,
                orderby = filters["orderby"],
                tipe = filters["tipe"] ?: filters["type"],
                status = filters["status"],
                genre = filters["genre"],
                sorttime = filters["sorttime"],
            )
            resp.error?.let { throw IllegalStateException(it) }
            val data = resp.data
            val comics = data?.comics.orEmpty().map { mapListItem(it) }
            val total = data?.pagination?.totalPages ?: if (comics.size >= 10) page + 1 else page
            val hasNext = hasNextPage(page, data?.pagination?.totalPages, comics.size, 10)
            return Paged(items = comics, page = page, totalPages = maxOf(total, page), hasNextPage = hasNext)
        }
    }

    private companion object {
        const val DETAIL_MEMO_TTL_MS: Long = 60_000L
    }

    // Memo detail 60 detik (bug A2): sheet memanggil detail() lalu
    // chapters() untuk komik yang sama — cukup satu fetch. TTL pendek
    // menjaga daftar chapter tidak basi lama.
    private var detailMemoId: String? = null
    private var detailMemoData: ComicDetail? = null
    private var detailMemoAt: Long = 0L

    // realUrl per chapter dari fetch detail terakhir (bug A5): pages()
    // menyerahkannya ke scraper agar tidak fetch detail ulang.
    private var chapterRealUrls: Map<String, String?> = emptyMap()

    private suspend fun fetchDetail(id: String): ApiResponse<ComicDetail> {
        val memo = detailMemoData
        if (memo != null && detailMemoId == id &&
            System.currentTimeMillis() - detailMemoAt < DETAIL_MEMO_TTL_MS
        ) {
            return ApiResponse(data = memo)
        }
        val resp = scraper.getComicDetail(id)
        resp.data?.let {
            detailMemoId = id
            detailMemoData = it
            detailMemoAt = System.currentTimeMillis()
            chapterRealUrls = it.chapterList.associate { item -> (item.id ?: extractSlug(item.url)) to item.realUrl }
        }
        return resp
    }

    override suspend fun detail(id: String): Comic {
        val resp = fetchDetail(id)
        val data = resp.data ?: throw IllegalStateException(resp.error ?: "Detail not available")
        return Comic(
            id = data.id ?: id,
            title = data.title,
            cover = data.thumbnail,
            rating = data.rating,
            status = data.status,
            type = data.type,
            author = data.author,
            illustrator = data.illustrator,
            synopsis = data.synopsis,
            genres = data.genres.map { Genre(id = it, name = it) },
            latestChapter = data.chapterList.firstOrNull()?.number ?: data.chapterList.firstOrNull()?.title,
            lastUpdated = AppDateFormatter.format(data.lastUpdated),
            sourceId = this.id,
            sourceName = this.name,
        )
    }

    override suspend fun chapters(id: String): List<Chapter> {
        val resp = fetchDetail(id)
        val data = resp.data ?: return emptyList()
        return data.chapterList.map { item ->
            val chSlug = item.id ?: extractSlug(item.url)
            Chapter(
                id = "$id:$chSlug",
                number = item.number,
                title = item.title,
                url = item.url,
                lastUpdated = AppDateFormatter.format(item.lastUpdated),
            )
        }
    }

    override suspend fun pages(chapterId: String): List<Page> {
        val (slug, chSlug) = if (chapterId.contains(":")) {
            val parts = chapterId.split(":", limit = 2)
            parts[0] to parts[1]
        } else {
            "" to chapterId
        }
        val resp = scraper.getChapter(slug, chSlug, chapterRealUrls[chSlug])
        val data = resp.data ?: throw IllegalStateException(resp.error ?: "Pages not available")
        return data.images.mapIndexed { idx, url ->
            Page(index = idx, imageUrl = url)
        }
    }

    private fun mapListItem(item: ComicListItem): Comic {
        val slug = extractSlug(item.url)
        return Comic(
            id = if (slug.isNotBlank()) slug else item.title,
            title = item.title,
            cover = item.cover,
            type = item.type,
            rating = item.rating?.toString(),
            latestChapter = item.latestChapter?.title,
            sourceId = id,
            sourceName = name,
        )
    }

    override suspend fun sectionFeed(sectionId: String, tabId: String, page: Int): Paged<Comic> = when (sectionId) {
        "updates" -> {
            val data = scraper.updates(page).data
            val comics = data?.comics.orEmpty().map { mapListItem(it) }
            val total = data?.totalPages
            Paged(items = comics, page = page, totalPages = total, hasNextPage = hasNextPage(page, total, comics.size, 20))
        }

        "projects" -> {
            val data = scraper.projects(page).data
            Paged(items = data?.projects.orEmpty().map { mapListItem(it) }, page = page, totalPages = null, hasNextPage = false)
        }

        else -> Paged(emptyList(), page, null, false)
    }
}
