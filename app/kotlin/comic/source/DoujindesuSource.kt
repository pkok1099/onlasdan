package comic.source
import comic.R
import comic.api.ApiResponse
import comic.api.ComicDetail
import comic.api.ComicListItem
import comic.api.HomeComicItem
import comic.scraper.DoujindesuScraper
import comic.scraper.GenreMasterData
import comic.util.extractSlug
import kotlinx.coroutines.CancellationException

class DoujindesuSource(private val scraper: DoujindesuScraper = DoujindesuScraper()) : ComicSource {

    override val id: String = "doujindesu"
    override val name: String = "DoujinDesu"
    override val iconRes: Int = R.drawable.ic_source_default

    override val capabilities: Set<Capability> = setOf(
        Capability.SEARCH,
        Capability.FILTER_TYPE,
        Capability.FILTER_GENRE,
        Capability.FILTER_STATUS,
        Capability.FILTER_SORT,
        Capability.LATEST,
        Capability.POPULAR,
        Capability.HERO_CAROUSEL,
        Capability.SECTION_BANNERS,
        Capability.GENRE_BROWSER,
    )

    override val homeLayout: List<SectionType> = listOf(SectionType.HERO, SectionType.GRID, SectionType.GRID)

    // Opsi sesuai param API /api/manga (terverifikasi 2026-10-18).
    private val sortOptions = listOf(
        FilterOption("latest_chapter", "Chapter Terbaru"),
        FilterOption("popular", "Populer"),
        FilterOption("rating", "Rating"),
        FilterOption("views", "Dilihat"),
    )
    private val typeOptions = listOf(
        FilterOption("doujinshi", "Doujinshi"),
        FilterOption("manga", "Manga"),
        FilterOption("manhwa", "Manhwa"),
    )
    private val statusOptions = listOf(
        FilterOption("publishing", "Ongoing"),
        FilterOption("completed", "Tamat"),
    )
    private val genreOptions = GenreMasterData.doujindesuMasterGenres.map { FilterOption(it.id, it.name) }

    override fun availableFilters(): List<Filter> = listOf(
        Filter("sort", "Urutan", sortOptions),
        Filter("type", "Tipe", typeOptions),
        Filter("status", "Status", statusOptions),
        Filter("genre", "Genre", genreOptions),
    )

    override val featuredGenres: List<Genre> = listOf(
        Genre(id = "big-breast", name = "Big Breast"),
        Genre(id = "nakadashi", name = "Nakadashi"),
        Genre(id = "sole-female", name = "Sole Female"),
        Genre(id = "sole-male", name = "Sole Male"),
        Genre(id = "blowjob", name = "Blowjob"),
        Genre(id = "x-ray", name = "X-Ray"),
    )

    override suspend fun allGenres(): List<Genre> = runCatching { scraper.getGenres() }
        .onFailure { if (it is CancellationException) throw it }
        .getOrNull()?.data?.takeIf { it.isNotEmpty() }
        ?: GenreMasterData.doujindesuMasterGenres

    private fun mapHomeItem(item: HomeComicItem): Comic {
        val slug = extractSlug(item.url)
        return Comic(
            id = if (slug.isNotBlank()) slug else item.title,
            title = item.title,
            cover = item.cover,
            rating = item.rating,
            type = item.type,
            latestChapter = item.latestChapter?.title,
            lastUpdated = item.lastUpdated,
            sourceId = id,
            sourceName = name,
        )
    }

    private fun mapListItem(item: ComicListItem): Comic {
        val slug = extractSlug(item.url)
        return Comic(
            id = if (slug.isNotBlank()) slug else item.title,
            title = item.title,
            cover = item.cover,
            rating = item.rating?.toString(),
            type = item.type,
            latestChapter = item.latestChapter?.title,
            lastUpdated = item.lastUpdated,
            sourceId = id,
            sourceName = name,
        )
    }

    override suspend fun home(): List<Section> {
        val resp = scraper.getHome()
        // Jangan telan error jadi daftar kosong: lempar agar HomeScreen menampilkan
        // pesan error aslinya (diagnostik) via AppErrorHandler, bukan EmptyState buta.
        if (resp.error != null) {
            throw RuntimeException("doujindesu: ${resp.error}")
        }
        val data = resp.data ?: throw RuntimeException("doujindesu: respons kosong")
        val sections = mutableListOf<Section>()

        // 1. Hero Carousel: top 8 popular items
        val heroItems = data.popularComics.take(8).map { mapHomeItem(it) }
        if (heroItems.isNotEmpty()) {
            sections.add(
                Section(
                    id = "hero",
                    title = null,
                    type = SectionType.HERO,
                    items = heroItems,
                ),
            )
        }

        // 2. Section Doujinshi & Manga (isinya type=doujinshi, sesuai getHome())
        if (data.latestComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "doujinshi_manga",
                    title = "Doujinshi & Manga",
                    type = SectionType.GRID,
                    items = data.latestComics.map { mapHomeItem(it) },
                    hasMore = true,
                    browseFilters = mapOf("type" to "doujinshi"),
                ),
            )
        }

        // 3. Section Manhwa
        val manhwaResp = scraper.browse(
            page = 1,
            orderby = "latest_chapter",
            tipe = "manhwa",
            status = null,
            genre = null,
            sorttime = null,
        )
        val manhwaItems = manhwaResp.data?.comics.orEmpty().map { mapListItem(it) }
        if (manhwaItems.isNotEmpty()) {
            sections.add(
                Section(
                    id = "manhwa",
                    title = "Manhwa",
                    type = SectionType.GRID,
                    items = manhwaItems,
                    hasMore = true,
                    browseFilters = mapOf("type" to "manhwa"),
                ),
            )
        }

        return sections
    }

    override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> {
        val cleanQuery = query.trim().ifBlank {
            filters["search"]?.trim()?.ifBlank { null } ?: filters["q"]?.trim()?.ifBlank { null }
        }
        val resp = scraper.browse(
            page = page,
            orderby = filters["sort"] ?: filters["sort_by"],
            tipe = filters["type"] ?: filters["format"],
            status = filters["status"],
            genre = filters["genre"],
            sorttime = null,
            search = cleanQuery,
        )
        if (resp.error != null) {
            throw RuntimeException("doujindesu: ${resp.error}")
        }
        val data = resp.data ?: throw RuntimeException("doujindesu: respons kosong")
        val comics = data.comics.map { mapListItem(it) }
        val total = data.pagination?.totalPages
        val hasNext = hasNextPage(page, total, comics.size, 20)
        return Paged(items = comics, page = page, totalPages = total, hasNextPage = hasNext)
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
            genres = data.genres.map { Genre(id = it.trim().lowercase().replace(" ", "-"), name = it.trim()) },
            latestChapter = data.chapterList.firstOrNull()?.number ?: data.chapterList.firstOrNull()?.title,
            lastUpdated = AppDateFormatter.format(data.lastUpdated),
            sourceId = this.id,
            sourceName = this.name,
            groups = data.groups,
            series = data.series,
            characters = data.characters,
            views = data.views,
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
                downloadUrl = item.downloadUrl,
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
        val resp = scraper.getChapter(slug, chSlug)
        val data = resp.data ?: throw IllegalStateException(resp.error ?: "Pages not available")
        return data.images.mapIndexed { idx, url ->
            Page(index = idx, imageUrl = url)
        }
    }

    override suspend fun sectionFeed(sectionId: String, tabId: String, page: Int): Paged<Comic> = when (sectionId) {
        "updates" -> {
            val resp = scraper.updates(page)
            if (resp.error != null) throw RuntimeException("doujindesu: ${resp.error}")
            val data = resp.data ?: throw RuntimeException("doujindesu: respons kosong")
            val comics = data.comics.map { mapListItem(it) }
            val total = data.totalPages
            Paged(items = comics, page = page, totalPages = total, hasNextPage = hasNextPage(page, total, comics.size, 20))
        }

        "projects" -> {
            val resp = scraper.projects(page)
            if (resp.error != null) throw RuntimeException("doujindesu: ${resp.error}")
            val data = resp.data ?: throw RuntimeException("doujindesu: respons kosong")
            Paged(items = data.projects.map { mapListItem(it) }, page = page, totalPages = null, hasNextPage = false)
        }

        else -> Paged(emptyList(), page, null, false)
    }
}
