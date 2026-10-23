package com.onlasdan.comic.scraper
import com.onlasdan.comic.api.*
import kotlinx.coroutines.CancellationException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class KomikuScraper {
    private val http = ScraperHttp()
    private val base = "https://komiku.org"
    private val apiBase = "https://api.komiku.org"
    private var cachedGenres: List<BrowseFilterOption>? = null

    private companion object {
        const val ATTR_DATA_SRC = "data-src"
        const val SEL_LS2V_IMG = ".ls2v img"
        const val SEL_LS4V_IMG = ".ls4v img"
    }

    private suspend fun getGenreOptions(): List<BrowseFilterOption> {
        val cached = cachedGenres
        if (!cached.isNullOrEmpty()) return cached
        val fallback = GenreMasterData.komikuMasterGenres.map { BrowseFilterOption(it.name, it.id) }
        return try {
            val rows = KomikuParser.parseKomikuGenreOptions(http.get("$base/pustaka/"))
            // Hanya hasil asli yang boleh masuk cache: kegagalan sesaat atau
            // parse kosong tidak boleh membekukan fallback seumur proses (A10).
            if (rows.isEmpty()) {
                fallback
            } else {
                cachedGenres = rows
                rows
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fallback
        }
    }

    private suspend fun mangaApi(page: Int, params: Map<String, String>): List<ComicListItem> {
        val safePage = page.coerceAtLeast(1)
        val baseUrl = if (safePage <= 1) {
            "$apiBase/manga/?"
        } else {
            // Komiku API uses a dedicated path for pagination.
            "$apiBase/manga/page/$safePage/?page=$safePage&"
        }
        val url = buildString {
            append(baseUrl)
            append(params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" })
        }
        val html = http.get(url)
        return KomikuParser.parseBgeCards(html)
    }

    suspend fun getHome(): ApiResponse<HomepageData> {
        return try {
            // Match backend + frontend: parse komiku.org homepage sections (ranking/hot/latest/genre/stats).
            val html = http.get(base)
            val doc = Jsoup.parse(html)

            val popularArticles = doc.select("#Rekomendasi_Komik article, #rank-harian article, #rank-mingguan article")
            val popular = popularArticles.mapNotNull(KomikuParser::toHomeComicItem)

            val latestArticles = doc.select("#Terbaru article, .ls2-wrap article, #Baru_Ditambahkan article")
            val latest = latestArticles.mapNotNull(KomikuParser::toHomeComicItem)

            val hotManga = doc.select("#Komik_Hot_Manga article").mapNotNull(KomikuParser::toHomeComicItem)
            val hotManhwa = doc.select("#Komik_Hot_Manhwa article").mapNotNull(KomikuParser::toHomeComicItem)
            val hotManhua = doc.select("#Komik_Hot_Manhua article").mapNotNull(KomikuParser::toHomeComicItem)
            val genreCards = doc.select("#Genre .ls3").mapNotNull { card ->
                val name = card.selectFirst("h4")?.text()?.trim().orEmpty()
                val url = card.selectFirst("a")?.attr("href")?.trim().orEmpty()
                val image = card.selectFirst("img")?.attr("src")?.trim().orEmpty()
                if (name.isBlank() || url.isBlank()) return@mapNotNull null
                HomeGenreCard(name = name, url = KomikuParser.absUrl(url), image = KomikuParser.normalizeImageUrl(image))
            }
            val stats = KomikuParser.extractStats(doc)

            val resolvedPopular = popular.ifEmpty { latest.take(10) }
            val resolvedLatest = latest.ifEmpty { popular }

            val hero = resolvedPopular.firstOrNull()?.let {
                HomeHeroComic(
                    title = it.title,
                    url = it.url,
                    cover = it.cover,
                    rating = null,
                    author = null,
                    type = it.type,
                    description = null,
                    latestChapter = it.latestChapter,
                )
            }
            ApiResponse(data = HomepageData(heroComic = hero, latestComics = resolvedLatest, popularComics = resolvedPopular, hotManga = hotManga, hotManhwa = hotManhwa, hotManhua = hotManhua, genreCards = genreCards, stats = stats, source = "komiku"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ApiResponse(error = e.message ?: "Failed to fetch Komiku home")
        }
    }

    suspend fun browse(
        page: Int,
        orderby: String?,
        tipe: String?,
        status: String?,
        genre: String?,
        sorttime: String?,
    ): ApiResponse<BrowseData> = try {
        val params = mutableMapOf(
            "orderby" to (orderby ?: "modified"),
        )
        if (!tipe.isNullOrBlank()) params["tipe"] = tipe
        if (!status.isNullOrBlank()) params["status"] = status
        if (!genre.isNullOrBlank()) params["genre"] = genre
        if (!sorttime.isNullOrBlank()) params["sorttime"] = sorttime
        // Backend logic: when orderby=meta_value_num, Komiku expects sorttime (commonly "all").
        if (params["orderby"] == "meta_value_num" && params["sorttime"].isNullOrBlank()) {
            params["sorttime"] = "all"
        }
        val comics = mangaApi(page, params)
        val filterOptions = BrowseFilterOptions(
            genres = getGenreOptions().ifEmpty {
                GenreMasterData.komikuMasterGenres.map { BrowseFilterOption(it.name, it.id) }
            },
            status = listOf(BrowseFilterOption("All", ""), BrowseFilterOption("Ongoing", "ongoing"), BrowseFilterOption("End", "end")),
            type = listOf(BrowseFilterOption("All", ""), BrowseFilterOption("Manga", "manga"), BrowseFilterOption("Manhua", "manhua"), BrowseFilterOption("Manhwa", "manhwa")),
            orderBy = listOf(BrowseFilterOption("Modified", "modified"), BrowseFilterOption("Date", "date"), BrowseFilterOption("Popular", "meta_value_num"), BrowseFilterOption("Random", "rand")),
            sortTime = listOf(BrowseFilterOption("All", "all"), BrowseFilterOption("Daily", "daily"), BrowseFilterOption("Weekly", "weekly")),
        )
        val totalPages = if (comics.size >= 10) page + 1 else page
        ApiResponse(data = BrowseData(comics = comics, pagination = PaginationData(currentPage = page, totalPages = totalPages), filterOptions = filterOptions))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to browse Komiku")
    }

    suspend fun search(query: String, page: Int): ApiResponse<SearchData> = try {
        val html = http.get("$apiBase/?post_type=manga&s=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page")
        val comics = KomikuParser.parseBgeCards(html)
        val totalPages = if (comics.size >= 10) page + 1 else page
        ApiResponse(data = SearchData(comics = comics, totalPages = totalPages))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to search Komiku")
    }

    suspend fun getComicDetail(slug: String): ApiResponse<ComicDetail> {
        return try {
            val html = http.get("$base/manga/${java.net.URLEncoder.encode(slug, "UTF-8")}/")
            val doc = Jsoup.parse(html)
            val title = doc.selectFirst("#Judul h1 span")?.text()?.replace(Regex("^Komik\\s+"), "")?.trim()
                ?: slug
            val imgEl = doc.selectFirst("#Informasi .ims img")
            val thumbRaw = imgEl?.attr(ATTR_DATA_SRC)?.ifBlank {
                imgEl.attr("data-original").ifBlank {
                    imgEl.attr("src")
                }
            } ?: imgEl?.attr("src").orEmpty()
            val thumb = thumbRaw.takeIf { it.isNotBlank() && !it.contains("lazy.jpg", ignoreCase = true) }?.let { KomikuParser.normalizeImageUrl(it) }
            val genres = doc.select("#Informasi ul.genre li.genre a span").map { it.text().trim() }.filter { it.isNotBlank() }
            val synopsis = doc.selectFirst("[itemprop='description']")?.text()?.trim()
                ?: doc.selectFirst("#Informasi p.desc")?.text()?.trim()

            // Align with backend: parse info table fields.
            val labelMap = mutableMapOf<String, String>()
            doc.select("#Informasi .inftable tr").forEach { row ->
                val cols = row.select("td")
                if (cols.size >= 2) {
                    val key = cols[0].text().trim().lowercase()
                    val value = cols[1].text().trim()
                    if (key.isNotBlank()) labelMap[key] = value
                }
            }

            fun field(vararg keys: String): String {
                for (k in keys) {
                    val v = labelMap[k.lowercase()]
                    if (!v.isNullOrBlank()) return v
                }
                return ""
            }

            val type = field("jenis komik")
            val status = field("status")
            val author = field("pengarang")
            val illustrator = field("ilustrator")
            val altTitle = field("judul indonesia", "judul alternatif", "judul lain")
            val lastUpdatedFallback = field("modified", "diperbarui", "update", "update terakhir")
            val chapters = doc.select("#Daftar_Chapter #daftarChapter tr[itemprop='itemListElement'], #Daftar_Chapter tr[itemprop='itemListElement']").mapNotNull { row ->
                val link = row.selectFirst("td.judulseries a") ?: return@mapNotNull null
                val chTitle = link.selectFirst("span[itemprop='name']")?.text()?.trim().orEmpty().ifBlank {
                    link.text().trim()
                }
                val chUrl = link.attr("href").takeIf { it.isNotBlank() }?.let { KomikuParser.absUrl(it) } ?: return@mapNotNull null
                val lastUpdated = row.selectFirst("td.tanggalseries")?.text()?.trim()
                val chapterId = chUrl.trimEnd('/').split("/").last()
                ChapterItem(
                    id = chapterId,
                    number = null,
                    title = chTitle,
                    url = "/komiku/read/$slug/$chapterId",
                    realUrl = chUrl,
                    lastUpdated = lastUpdated,
                )
            }

            val recommendations = doc.select("#Terbaru .ls8").mapNotNull { el ->
                val anchor = el.selectFirst(".ls8j h3 a") ?: return@mapNotNull null
                val recTitle = anchor.text().trim()
                val recHref = anchor.attr("href").trim()
                if (recTitle.isBlank() || recHref.isBlank()) return@mapNotNull null
                val recThumbRaw = el.selectFirst(".ls8v img")?.attr("src")?.ifBlank {
                    el.selectFirst(".ls8v img")?.attr(ATTR_DATA_SRC).orEmpty()
                }.orEmpty()
                HomeComicItem(
                    title = recTitle,
                    url = KomikuParser.internalizeComicUrl(recHref),
                    cover = recThumbRaw.takeIf { it.isNotBlank() }?.let { KomikuParser.normalizeImageUrl(it) },
                    type = null,
                    latestChapter = null,
                )
            }

            val lastUpdated = chapters.firstOrNull()?.lastUpdated
                ?: lastUpdatedFallback.ifBlank { null }
            ApiResponse(
                data = ComicDetail(
                    id = slug,
                    title = title,
                    thumbnail = thumb,
                    status = status.ifBlank { null },
                    type = type.ifBlank { null },
                    author = author.ifBlank { null },
                    illustrator = illustrator.ifBlank { null },
                    genres = genres,
                    synopsis = synopsis,
                    chapterList = chapters,
                    alternativeTitles = altTitle.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
                    lastUpdated = lastUpdated,
                    officialLinks = emptyList(),
                    infoLinks = emptyList(),
                    recommendations = recommendations,
                    source = "komiku",
                ),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ApiResponse(error = e.message ?: "Failed to fetch Komiku detail")
        }
    }

    // chapter param is the chapterId in our app routes (taken from chapterList url last segment)
    suspend fun getChapter(slug: String, chapter: String): ApiResponse<ChapterData> = try {
        fetchChapterPages(resolveChapterRealUrl(slug, chapter))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komiku chapter")
    }

    private suspend fun resolveChapterRealUrl(slug: String, chapter: String): String {
        if (slug.isBlank()) return KomikuParser.absUrl("/$chapter/")
        val detail = try {
            getComicDetail(slug).data
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
        return KomikuChapterParser.findChapterRealUrl(detail, chapter) ?: KomikuParser.absUrl("/$chapter/")
    }

    // Varian bug A5: realUrl sudah diketahui pemanggil dari daftar chapter —
    // lewati fetch detail di getChapter.
    suspend fun getChapter(slug: String, chapter: String, realUrl: String?): ApiResponse<ChapterData> = if (realUrl.isNullOrBlank()) getChapter(slug, chapter) else fetchChapterPages(realUrl)

    private suspend fun fetchChapterPages(realUrl: String): ApiResponse<ChapterData> = try {
        val html = http.get(realUrl)
        val doc: Document = Jsoup.parse(html)
        val selectors = listOf(
            "#Baca_Komik img.klazy",
            "#Baca_Komik img.ww",
            "#Baca_Komik img",
            ".baca-koleksi img",
            ".reader-area img",
        )
        val imgs = KomikuChapterParser.extractImagesBySelectors(doc, selectors)
            .ifEmpty { KomikuChapterParser.extractImagesFromChapterDataScript(doc) }
            .ifEmpty { KomikuChapterParser.extractImagesFromAllImages(doc) }

        if (imgs.isEmpty()) {
            ApiResponse(error = "Tidak ada gambar ditemukan di chapter ini")
        } else {
            ApiResponse(data = ChapterData(images = imgs))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komiku chapter")
    }

    suspend fun updates(page: Int): ApiResponse<UpdatesData> = try {
        val comics = mangaApi(page, mapOf("orderby" to "modified"))
        ApiResponse(data = UpdatesData(comics = comics, totalPages = null))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komiku updates")
    }

    suspend fun projects(page: Int): ApiResponse<ProjectsData> {
        // Komiku doesn't have "projects" in the same way; reuse browse.
        return try {
            val comics = mangaApi(page, mapOf("orderby" to "date"))
            ApiResponse(data = ProjectsData(projects = comics))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ApiResponse(error = e.message ?: "Failed to fetch Komiku projects")
        }
    }

    suspend fun genre(slug: String, page: Int): ApiResponse<GenrePageData> = try {
        val html = http.get("$apiBase/genre/${java.net.URLEncoder.encode(slug, "UTF-8")}/?page=$page")
        val comics = KomikuParser.parseBgeCards(html).map { HomeComicItem(it.title, it.url, it.cover, it.type, it.latestChapter) }
        ApiResponse(data = GenrePageData(genreSlug = slug, comics = comics, pagination = PaginationData(currentPage = page)))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komiku genre")
    }
}
