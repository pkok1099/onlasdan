package comic.scraper

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import comic.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * API-based source for shinigami (api.shngm.io). No HTML scraping.
 *
 * Verified endpoint/param table (recon 2026-10-06, xzx):
 * - q={query}                              → search filter      WORKS
 * - sort_by=updated_at + sort_order=desc    → latest first       WORKS
 * - format=manga | format=manhua            → type filter        WORKS
 * - page, page_size (<=500 verified)                             WORKS
 * - default list order = view_count desc ("popular")
 * - /v1/genre/list exists (51 genres) but NO content endpoint per genre
 * - genre=*, status=*, title=*, search=* params                  IGNORED by API
 * - sort_by=popular, format=comic                                HTTP 400 (invalid)
 * - Image URLs carry no expiry hints; assets.shngm.id has no hotlink protection
 */
class ShinigamiApi {

    companion object {
        const val BASE = "https://api.shngm.io"
        const val SOURCE = "shinigami"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/123.0.0.0 Mobile Safari/537.36"
        private const val REFERER = "https://11.shinigami.asia/"
        private const val PAGE_SIZE = 20
        private const val CHAPTER_PAGE_SIZE = 500
    }

    private val client = HttpClients.client

    // ---------- low level ----------

    private suspend fun httpGet(url: String): String {
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Referer", REFERER)
            .header("Accept", "application/json")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        return try {
            executeOnce(req)
        } catch (e: RateLimited) {
            delay(e.waitMs)
            executeOnce(req)
        }
    }

    private suspend fun executeOnce(req: Request): String = withContext(Dispatchers.IO) {
        client.newCall(req).execute().use { res ->
            if (res.code == 429) {
                val waitMs = (res.header("Retry-After")?.toLongOrNull()?.coerceAtMost(60) ?: 5L) * 1000
                throw RateLimited(waitMs)
            }
            if (!res.isSuccessful) throw RuntimeException("HTTP ${res.code} for ${req.url}")
            res.body?.string().orEmpty()
        }
    }

    private class RateLimited(val waitMs: Long) : RuntimeException("HTTP 429")

    /** Throws RuntimeException when retcode != 0. Visible for testing. */
    internal fun checkEnvelope(obj: JsonObject): JsonObject {
        val retcode = obj.optInt("retcode", -1)
        if (retcode != 0) {
            val msg = obj.optString("message") ?: "unknown API error"
            throw RuntimeException("shinigami API error retcode=$retcode: $msg")
        }
        return obj
    }

    private suspend fun getData(url: String): JsonObject = checkEnvelope(JsonParser.parseString(httpGet(url)).asJsonObject)

    private fun JsonObject.optString(key: String): String? = get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.optInt(key: String, default: Int): Int = get(key)?.takeIf { !it.isJsonNull }?.asInt ?: default

    private fun metaInt(obj: JsonObject, key: String): Int? = obj.getAsJsonObject("meta")?.optInt(key, -1)?.takeIf { it >= 0 }

    private fun dataArray(obj: JsonObject): List<JsonObject> = obj.getAsJsonArray("data")?.map { it.asJsonObject }.orEmpty()

    // ---------- parsing (internal = unit-testable) ----------

    internal fun parseMangaListItem(o: JsonObject): ComicListItem {
        val id = o.optString("manga_id").orEmpty()
        val formatName = taxonNames(o, "Format").firstOrNull()
        val country = o.optString("country_id")
        val effectiveType = formatName ?: when (country?.uppercase()) {
            "KR" -> "Manhwa"
            "JP" -> "Manga"
            "CN" -> "Manhua"
            else -> null
        }
        val rateVal = o.get("user_rate").asDoubleOrNull()
        val chNum = o.get("latest_chapter_number").asDoubleOrNull()?.let { fmtChapterNumber(it) }
        val chId = o.optString("latest_chapter_id")
        val latestCh = if (!chNum.isNullOrBlank()) {
            HomeLatestChapter(
                title = "Chapter $chNum",
                url = chId,
            )
        } else {
            null
        }
        val dateVal = o.optString("latest_chapter_time") ?: o.optString("updated_at")

        return ComicListItem(
            title = o.optString("title") ?: "Unknown",
            url = "/shinigami/comic/$id",
            cover = o.optString("cover_image_url") ?: o.optString("cover_portrait_url"),
            type = effectiveType,
            rating = rateVal,
            latestChapter = latestCh,
            lastUpdated = dateVal,
        )
    }

    private fun taxonNames(o: JsonObject, key: String): List<String> = o.getAsJsonObject("taxonomy")?.getAsJsonArray(key)
        ?.mapNotNull { it.asJsonObject.optString("name") }.orEmpty()

    internal fun parseMangaDetail(o: JsonObject): ComicDetail {
        val alt = o.optString("alternative_title")?.takeIf { it.isNotBlank() }
        return ComicDetail(
            id = o.optString("manga_id"),
            title = o.optString("title") ?: "Unknown",
            thumbnail = o.optString("cover_image_url"),
            rating = o.optString("user_rate"),
            // Only status=1 (ongoing) observed during recon; anything else left unknown.
            status = if (o.optInt("status", -1) == 1) "Ongoing" else null,
            type = taxonNames(o, "Format").firstOrNull(),
            author = taxonNames(o, "Author").joinToString(", ").ifBlank { null },
            illustrator = taxonNames(o, "Artist").joinToString(", ").ifBlank { null },
            genres = taxonNames(o, "Genre"),
            synopsis = o.optString("description"),
            alternativeTitles = alt?.let { listOf(it) }.orEmpty(),
            lastUpdated = o.optString("updated_at"),
            source = SOURCE,
        )
    }

    internal fun fmtChapterNumber(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()

    internal fun parseChapterListItem(o: JsonObject, mangaId: String): ChapterItem {
        val cid = o.optString("chapter_id").orEmpty()
        val num = fmtChapterNumber(o.get("chapter_number").asDoubleOrNull() ?: 0.0)
        val rawTitle = o.optString("chapter_title").orEmpty()
        return ChapterItem(
            id = cid,
            number = num,
            title = rawTitle.ifBlank { "Chapter $num" },
            url = "/shinigami/read/$mangaId/$cid",
            lastUpdated = o.optString("release_date"),
        )
    }

    // Latest-first, matching komiku/komikindo convention — DetailScreen
    // (newestSlug/oldestSlug) and ReaderScreen (prev/next via idx±1) rely on it.
    internal fun parseChapterList(arr: JsonArray, mangaId: String): List<ChapterItem> = arr.map { parseChapterListItem(it.asJsonObject, mangaId) }
        .sortedByDescending { it.number?.toDoubleOrNull() ?: 0.0 }

    internal fun parseChapterImages(o: JsonObject): List<String> {
        val base = o.optString("base_url").orEmpty().trimEnd('/')
        val body = o.getAsJsonObject("chapter")
        val path = body?.optString("path").orEmpty()
        return body?.getAsJsonArray("data")?.map { "$base$path${it.asString}" }.orEmpty()
    }

    private fun ComicListItem.toHomeItem() = HomeComicItem(
        title = title,
        url = url,
        cover = cover,
        type = type,
        latestChapter = latestChapter,
        rating = rating?.toString(),
        lastUpdated = lastUpdated,
    )

    // ---------- public API (mirrors KomikuScraper signatures) ----------

    suspend fun getHome(): ApiResponse<HomepageData> = try {
        val latest = getData("$BASE/v1/manga/list?page=1&page_size=24&type=project&sort=latest&sort_order=desc")
        val popular = getData("$BASE/v1/manga/list?page=1&page_size=12&sort=popularity")
        val recommended = runCatching {
            getData("$BASE/v1/manga/list?page=1&page_size=12&is_recommended=true&sort=latest&sort_order=desc")
        }.onFailure { if (it is CancellationException) throw it }.getOrNull()

        val latestItems = dataArray(latest).map { parseMangaListItem(it).toHomeItem() }
        val popularItems = dataArray(popular).map { parseMangaListItem(it).toHomeItem() }
        val recommendedItems = recommended?.let { dataArray(it).map { parseMangaListItem(it).toHomeItem() } }.orEmpty()

        val heroSource = if (recommendedItems.isNotEmpty()) recommendedItems else popularItems
        val hero = heroSource.firstOrNull()?.let {
            HomeHeroComic(
                title = it.title,
                url = it.url,
                cover = it.cover,
                rating = it.rating,
                type = it.type,
            )
        }
        ApiResponse(
            data = HomepageData(
                heroComic = hero,
                latestComics = latestItems,
                popularComics = popularItems,
                source = SOURCE,
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami home")
    }

    suspend fun getRecommended(page: Int = 1, pageSize: Int = 20, format: String? = null): ApiResponse<List<ComicListItem>> = try {
        val formatParam = if (!format.isNullOrBlank()) "&format=${format.lowercase().trim()}" else ""
        val obj = getData("$BASE/v1/manga/list?page=$page&page_size=$pageSize&is_recommended=true&sort=latest&sort_order=desc$formatParam")
        ApiResponse(data = dataArray(obj).map { parseMangaListItem(it) })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami recommendations")
    }

    suspend fun getUpdates(page: Int = 1, pageSize: Int = 24, type: String = "project"): ApiResponse<BrowseData> = try {
        val cleanType = type.lowercase().trim()
        val obj = getData("$BASE/v1/manga/list?page=$page&page_size=$pageSize&type=$cleanType&sort=latest&sort_order=desc")
        ApiResponse(
            data = BrowseData(
                comics = dataArray(obj).map { parseMangaListItem(it) },
                filterOptions = shinigamiFilterOptions(),
                pagination = PaginationData(
                    currentPage = page,
                    totalPages = metaInt(obj, "total_page"),
                ),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami updates")
    }

    suspend fun browse(
        page: Int,
        sort: String? = null,
        sortOrder: String? = null,
        status: String? = null,
        format: String? = null,
        type: String? = null,
        genreInclude: String? = null,
        genreIncludeMode: String? = null,
        search: String? = null,
    ): ApiResponse<BrowseData> = try {
        val params = mutableListOf("page=$page", "page_size=$PAGE_SIZE")
        val effectiveSort = when (sort?.lowercase()?.trim()) {
            "popular" -> "popularity"
            "updated_at" -> "latest"
            else -> sort?.trim()
        }
        if (!effectiveSort.isNullOrBlank()) params += "sort=$effectiveSort"
        if (!sortOrder.isNullOrBlank()) params += "sort_order=${sortOrder.lowercase().trim()}"
        if (!status.isNullOrBlank()) params += "status=${status.lowercase().trim()}"
        val cleanFormat = format?.lowercase()?.trim()
        if (!cleanFormat.isNullOrBlank()) params += "format=$cleanFormat"
        val cleanType = type?.lowercase()?.trim()
        if (!cleanType.isNullOrBlank()) params += "type=$cleanType"
        val effectiveGenre = genreInclude?.trim()
        if (!effectiveGenre.isNullOrBlank()) {
            val g = java.net.URLEncoder.encode(effectiveGenre, "UTF-8")
            params += "genre_include=$g"
        }
        if (!genreIncludeMode.isNullOrBlank()) params += "genre_include_mode=$genreIncludeMode"
        val cleanSearch = search?.trim()
        if (!cleanSearch.isNullOrBlank()) {
            val q = java.net.URLEncoder.encode(cleanSearch, "UTF-8")
            params += "q=$q"
        }
        val obj = getData("$BASE/v1/manga/list?" + params.joinToString("&"))
        ApiResponse(
            data = BrowseData(
                comics = dataArray(obj).map { parseMangaListItem(it) },
                filterOptions = shinigamiFilterOptions(),
                pagination = PaginationData(
                    currentPage = page,
                    totalPages = metaInt(obj, "total_page"),
                ),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to browse shinigami")
    }

    private fun shinigamiFilterOptions() = BrowseFilterOptions(
        genres = GenreMasterData.shinigamiMasterGenres.map { BrowseFilterOption(it.name, it.id) },
        status = listOf(
            BrowseFilterOption("All", ""),
            BrowseFilterOption("Ongoing", "ongoing"),
            BrowseFilterOption("Completed", "completed"),
            BrowseFilterOption("Hiatus", "hiatus"),
        ),
        type = listOf(
            BrowseFilterOption("All", ""),
            BrowseFilterOption("Manga", "manga"),
            BrowseFilterOption("Manhwa", "manhwa"),
            BrowseFilterOption("Manhua", "manhua"),
        ),
        orderBy = listOf(
            BrowseFilterOption("Latest Updates", "latest"),
            BrowseFilterOption("Most Popular", "popularity"),
            BrowseFilterOption("Top Rating", "rating"),
        ),
        sortTime = emptyList(),
    )

    suspend fun search(query: String, page: Int): ApiResponse<SearchData> = try {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val obj = getData("$BASE/v1/manga/list?page=$page&page_size=$PAGE_SIZE&q=$q")
        ApiResponse(
            data = SearchData(
                comics = dataArray(obj).map { parseMangaListItem(it) },
                totalPages = metaInt(obj, "total_page"),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to search shinigami")
    }

    suspend fun getComicDetail(slug: String): ApiResponse<ComicDetail> = try {
        val detailObj = getData("$BASE/v1/manga/detail/$slug")
        val detail = parseMangaDetail(detailObj.getAsJsonObject("data"))
        // Chapter list is paginated; page_size=500 verified (recon: 920 chapters → 2 pages).
        val chapters = mutableListOf<ChapterItem>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages) {
            val listObj = getData(
                "$BASE/v1/chapter/$slug/list?page=$page&page_size=$CHAPTER_PAGE_SIZE" +
                    "&sort_by=chapter_number&sort_order=desc",
            )
            chapters += parseChapterList(listObj.getAsJsonArray("data"), slug)
            totalPages = metaInt(listObj, "total_page") ?: 1
            page++
        }
        ApiResponse(data = detail.copy(chapterList = chapters))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami detail")
    }

    suspend fun getChapter(slug: String, chapter: String): ApiResponse<ChapterData> = try {
        val obj = getData("$BASE/v1/chapter/detail/$chapter")
        ApiResponse(data = ChapterData(images = parseChapterImages(obj.getAsJsonObject("data"))))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami chapter")
    }

    suspend fun updates(page: Int): ApiResponse<UpdatesData> = try {
        val obj = getData(
            "$BASE/v1/manga/list?page=$page&page_size=$PAGE_SIZE&sort_by=updated_at&sort_order=desc",
        )
        ApiResponse(
            data = UpdatesData(
                comics = dataArray(obj).map { parseMangaListItem(it) },
                totalPages = metaInt(obj, "total_page"),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami updates")
    }

    suspend fun projects(page: Int): ApiResponse<ProjectsData> = try {
        // No "projects" concept on shinigami; reuse the popular list (same pragmatic
        // reuse as KomikuScraper.projects).
        val obj = getData("$BASE/v1/manga/list?page=$page&page_size=$PAGE_SIZE")
        ApiResponse(data = ProjectsData(projects = dataArray(obj).map { parseMangaListItem(it) }))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami projects")
    }

    suspend fun genre(slug: String, page: Int): ApiResponse<GenrePageData> = try {
        val s = java.net.URLEncoder.encode(slug, "UTF-8")
        val obj = getData("$BASE/v1/manga/list?page=$page&page_size=$PAGE_SIZE&genre_include=$s")
        val comics = dataArray(obj).map { parseMangaListItem(it).toHomeItem() }
        val total = metaInt(obj, "total_page")
        val name = GenreMasterData.shinigamiMasterGenres.find { it.id.equals(slug, ignoreCase = true) }?.name ?: slug
        ApiResponse(
            data = GenrePageData(
                genreName = name,
                genreSlug = slug,
                comics = comics,
                pagination = PaginationData(currentPage = page, totalPages = total),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch shinigami genre")
    }
}
