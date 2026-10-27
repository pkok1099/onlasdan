package comic.source

import comic.R
import comic.api.ApiResponse
import comic.api.ComicDetail
import comic.api.ComicListItem
import comic.api.HomeComicItem
import comic.scraper.GenreMasterData
import comic.scraper.ShinigamiApi
import comic.util.extractSlug
import kotlinx.coroutines.CancellationException

class ShinigamiSource(private val api: ShinigamiApi = ShinigamiApi()) : ComicSource {

    override val id: String = "shinigami"
    override val name: String = "Shinigami"
    override val iconRes: Int = R.drawable.ic_source_default

    override val capabilities: Set<Capability> = setOf(
        Capability.SEARCH,
        Capability.FILTER_GENRE,
        Capability.FILTER_TYPE,
        Capability.FILTER_STATUS,
        Capability.FILTER_SORT,
        Capability.RANKING,
        Capability.LATEST,
        Capability.POPULAR,
        Capability.HERO_CAROUSEL,
        Capability.MANGA_FEED_LAYOUT,
        Capability.HD_MODE,
    )

    override val homeLayout: List<SectionType> = listOf(SectionType.HERO, SectionType.ROW, SectionType.GRID, SectionType.ROW)

    private val sortOptions = listOf(
        FilterOption("latest", "Terbaru"),
        FilterOption("popularity", "Populer"),
        FilterOption("rating", "Rating"),
    )
    private val sortOrderOptions = listOf(
        FilterOption("desc", "Menurun"),
        FilterOption("asc", "Menaik"),
    )
    private val formatOptions = listOf(
        FilterOption("manga", "Manga"),
        FilterOption("manhwa", "Manhwa"),
        FilterOption("manhua", "Manhua"),
    )
    private val statusOptions = listOf(
        FilterOption("ongoing", "Ongoing"),
        FilterOption("completed", "Tamat"),
        FilterOption("hiatus", "Hiatus"),
    )
    private val genreOptions = GenreMasterData.shinigamiMasterGenres.map { FilterOption(it.id, it.name) }

    override fun availableFilters(): List<Filter> = listOf(
        Filter("sort", "Urutan", sortOptions),
        Filter("sort_order", "Arah", sortOrderOptions),
        Filter("format", "Format", formatOptions),
        Filter("status", "Status", statusOptions),
        Filter("genre", "Genre", genreOptions),
    )

    override suspend fun allGenres(): List<Genre> = GenreMasterData.shinigamiMasterGenres

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
            type = item.type,
            rating = item.rating?.toString(),
            latestChapter = item.latestChapter?.title,
            lastUpdated = item.lastUpdated,
            sourceId = id,
            sourceName = name,
        )
    }

    override suspend fun home(): List<Section> {
        val resp = api.getHome()
        val data = resp.data
            ?: throw RuntimeException("shinigami: ${resp.error ?: "respons kosong"}")
        val sections = mutableListOf<Section>()

        // Hanya varian format hero yang difetch terpisah (bug A3); section
        // lain memakai hasil getHome() yang sudah di tangan.
        val recommendedResp = runCatching { api.getRecommended(pageSize = 10, format = "manhwa") }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
        val recommendedItems = recommendedResp?.data.orEmpty().map { mapListItem(it) }

        val heroComics = (recommendedItems.ifEmpty { data.popularComics.map { mapHomeItem(it) } }).take(6)
        if (heroComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "hero",
                    title = null,
                    type = SectionType.HERO,
                    items = heroComics,
                ),
            )
        }

        if (recommendedItems.isNotEmpty()) {
            sections.add(
                Section(
                    id = "recommendations",
                    title = "Rekomendasi",
                    type = SectionType.ROW,
                    items = recommendedItems,
                    hasMore = true,
                    browseFilters = mapOf("sort" to "rating"),
                ),
            )
        }

        val updateItems = data.latestComics.map { mapHomeItem(it) }

        if (updateItems.isNotEmpty()) {
            sections.add(
                Section(
                    id = "latest",
                    title = "Update",
                    type = SectionType.GRID,
                    items = updateItems,
                    hasMore = true,
                    browseFilters = mapOf("sort" to "latest"),
                ),
            )
        }

        if (data.popularComics.isNotEmpty()) {
            sections.add(
                Section(
                    id = "popular",
                    title = "Series Populer",
                    type = SectionType.ROW,
                    items = data.popularComics.map { mapHomeItem(it) },
                    hasMore = true,
                    browseFilters = mapOf("sort" to "popularity"),
                ),
            )
        }

        return sections
    }

    override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> {
        val cleanQuery = query.trim().ifBlank {
            filters["q"]?.trim()?.ifBlank { null } ?: filters["search"]?.trim()?.ifBlank { null }
        }
        val resp = api.browse(
            page = page,
            sort = filters["sort"] ?: filters["sort_by"],
            sortOrder = filters["sort_order"],
            status = filters["status"],
            format = filters["format"] ?: filters["type"],
            genreInclude = filters["genre"] ?: filters["genre_include"],
            search = cleanQuery,
        )
        if (resp.error != null) {
            throw RuntimeException("shinigami: ${resp.error}")
        }
        val data = resp.data ?: throw RuntimeException("shinigami: respons kosong")
        val comics = data.comics.map { item ->
            val slug = extractSlug(item.url)
            Comic(
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
        val resp = api.getComicDetail(id)
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
        val resp = api.getChapter(slug, chSlug)
        val data = resp.data ?: throw IllegalStateException(resp.error ?: "Pages not available")
        return data.images.mapIndexed { idx, url ->
            Page(index = idx, imageUrl = url)
        }
    }

    override suspend fun sectionFeed(sectionId: String, tabId: String, page: Int): Paged<Comic> = when (sectionId) {
        "recommendations" -> {
            val format = tabId.lowercase().trim()
            val cacheKey = "$id:recs:$format:p$page"
            val typeToken = com.google.gson.reflect.TypeToken.getParameterized(Paged::class.java, Comic::class.java).type
            SourceCache.getOrFetch(cacheKey, typeToken, ttlMs = 15 * 60 * 1000L) {
                val resp = api.getRecommended(page = page, pageSize = 10, format = format)
                val list = resp.data?.map { mapListItem(it) }
                    ?: throw RuntimeException(resp.error ?: "Gagal memuat rekomendasi shinigami")
                Paged(items = list, page = page, totalPages = 1, hasNextPage = false)
            }
        }

        "latest", "update" -> {
            val type = tabId.lowercase().trim()
            val cacheKey = "$id:updates:$type:p$page"
            val typeToken = com.google.gson.reflect.TypeToken.getParameterized(Paged::class.java, Comic::class.java).type
            SourceCache.getOrFetch(cacheKey, typeToken, ttlMs = 5 * 60 * 1000L) {
                val resp = api.getUpdates(page = page, pageSize = 24, type = type)
                val data = resp.data ?: throw RuntimeException(resp.error ?: "Gagal memuat update shinigami")
                val comics = data.comics.map { mapListItem(it) }
                val totalPages = data.pagination?.totalPages
                val hasNext = hasNextPage(page, totalPages, comics.size, 24)
                Paged(items = comics, page = page, totalPages = totalPages, hasNextPage = hasNext)
            }
        }

        "updates" -> updatesFeed(page)

        "projects" -> projectsFeed(page)

        else -> Paged(emptyList(), page, null, false)
    }

    private suspend fun updatesFeed(page: Int): Paged<Comic> {
        val resp = api.updates(page)
        if (resp.error != null) throw RuntimeException("shinigami: ${resp.error}")
        val data = resp.data ?: throw RuntimeException("shinigami: respons kosong")
        val comics = data.comics.map { mapListItem(it) }
        val total = data.totalPages
        return Paged(items = comics, page = page, totalPages = total, hasNextPage = hasNextPage(page, total, comics.size, 20))
    }

    private suspend fun projectsFeed(page: Int): Paged<Comic> {
        val resp = api.projects(page)
        if (resp.error != null) throw RuntimeException("shinigami: ${resp.error}")
        val data = resp.data ?: throw RuntimeException("shinigami: respons kosong")
        return Paged(items = data.projects.map { mapListItem(it) }, page = page, totalPages = null, hasNextPage = false)
    }
}
