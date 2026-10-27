package comic.source

import comic.R
import comic.api.ApiResponse
import comic.api.BrowseFilterOption
import comic.api.ComicDetail
import comic.api.ComicListItem
import comic.api.HomeComicItem
import comic.scraper.GenreMasterData
import comic.scraper.KomikindoScraper
import comic.util.extractSlug

class KomikindoSource(
    private val scraper: KomikindoScraper = KomikindoScraper(),
) : ComicSource {

    override val id: String = "komikindo"
    override val name: String = "Komikindo"
    override val iconRes: Int = R.drawable.ic_source_default

    override val capabilities: Set<Capability> = setOf(
        Capability.SEARCH,
        Capability.FILTER_GENRE,
        Capability.FILTER_STATUS,
        Capability.FILTER_SORT,
        Capability.LATEST,
        Capability.POPULAR,
    )

    override val homeLayout: List<SectionType> = listOf(SectionType.HERO, SectionType.RANKED, SectionType.GRID)

    private val staticOptions by lazy { scraper.staticFilterOptions() }

    private fun singleFilter(
        key: String,
        label: String,
        options: List<BrowseFilterOption>,
    ): Filter = Filter(
        key = key,
        label = label,
        options = options.map { FilterOption(id = it.value, label = it.label) },
    )

    private fun multiFilter(
        key: String,
        label: String,
        options: List<BrowseFilterOption>,
    ): Filter = singleFilter(key, label, options).copy(multi = true)

    /**
     * Keunikan Komikindo sebagai data: 9 dimensi filter.
     * Tidak ada cabang per source di UI — semua di-render generik.
     */

    /**
     * Genre unggulan untuk section "Jelajahi Genre" — 6 genre populer,
     * id terverifikasi ada di staticFilterOptions (2026-10-18).
     */
    override val featuredGenres: List<Genre> = listOf(
        Genre(id = "action", name = "Action"),
        Genre(id = "adventure", name = "Adventure"),
        Genre(id = "fantasy", name = "Fantasy"),
        Genre(id = "romance", name = "Romance"),
        Genre(id = "comedy", name = "Comedy"),
        Genre(id = "drama", name = "Drama"),
    )

    override suspend fun allGenres(): List<Genre> = GenreMasterData.komikindoMasterGenres

    override fun availableFilters(): List<Filter> = listOf(
        singleFilter("type", "Tipe", staticOptions.type),
        singleFilter("status", "Status", staticOptions.status),
        singleFilter("format", "Format", staticOptions.format),
        singleFilter("order", "Urutan", staticOptions.orderBy),
        multiFilter("genre", "Genre", staticOptions.genres),
        multiFilter("demografis", "Demografis", staticOptions.demografis),
        multiFilter("tema", "Tema", staticOptions.tema),
        multiFilter("konten", "Konten", staticOptions.konten),
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
            ?: throw RuntimeException("komikindo: ${resp.error ?: "respons kosong"}")
        val sections = mutableListOf<Section>()

        data.heroComic?.let { hero ->
            val slug = extractSlug(hero.url)
            val heroComic = Comic(
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
            sections.add(
                Section(
                    id = "hero",
                    title = null,
                    type = SectionType.HERO,
                    items = listOf(heroComic),
                ),
            )
        }

        if (data.popularComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "popular",
                    title = "Peringkat",
                    type = SectionType.RANKED,
                    items = data.popularComics.map { mapHomeItem(it) },
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
                    items = data.latestComics.map { mapHomeItem(it) },
                    hasMore = true,
                ),
            )
        }

        return sections
    }

    override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> {
        val cleanQuery = query.trim()
        fun multi(key: String): List<String>? = filters[key]?.split(",")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
        val resp = scraper.browse(
            page = page,
            title = cleanQuery.ifEmpty { null },
            type = filters["type"],
            status = filters["status"],
            format = filters["format"],
            order = filters["order"],
            genre = multi("genre"),
            demografis = multi("demografis"),
            konten = multi("konten"),
            tema = multi("tema"),
        )
        resp.error?.let { throw IllegalStateException(it) }
        val data = resp.data
        val comics = data?.comics.orEmpty().map { mapListItem(it) }
        val total = data?.pagination?.totalPages ?: 1
        return Paged(items = comics, page = page, totalPages = total, hasNextPage = page < total)
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
