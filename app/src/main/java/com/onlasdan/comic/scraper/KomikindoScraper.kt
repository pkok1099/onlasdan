package com.onlasdan.comic.scraper

import com.onlasdan.comic.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class KomikindoScraper {
    private val http = ScraperHttp()
    private val base = "https://komikindo.ch"

    // Fallback filter options (matches backend hardcoded KOMIKINDO_FILTER_OPTIONS).

    /**
     * Opsi filter statis untuk dideklarasikan adapter sebagai data.
     * Tidak mengubah logika parsing — hanya membuka akses daftar fallback.
     */
    fun staticFilterOptions(): BrowseFilterOptions = BrowseFilterOptions(
        genres = KomikindoParser.fallbackGenres,
        demografis = KomikindoParser.fallbackDemografis,
        tema = KomikindoParser.fallbackTema,
        konten = KomikindoParser.fallbackKonten,
        status = KomikindoParser.fallbackStatus,
        type = KomikindoParser.fallbackTypes,
        format = KomikindoParser.fallbackFormats,
        orderBy = KomikindoParser.fallbackOrder,
    )

    suspend fun getHome(): ApiResponse<HomepageData> = coroutineScope {
        try {
            val htmlAsync = async { http.get("$base/") }

            val html = htmlAsync.await()

            val doc = Jsoup.parse(html)
            val popularComics = doc.select(".mangapopuler .animepost").mapNotNull { KomikindoParser.parseAnimePost(it) }
            val latestComics = doc.select(".chapterbaru .animepost").mapNotNull { KomikindoParser.parseAnimePost(it) }

            val hero = popularComics.firstOrNull()?.let {
                HomeHeroComic(
                    title = it.title,
                    url = it.url,
                    cover = it.cover,
                    type = it.type,
                    latestChapter = it.latestChapter,
                )
            }

            ApiResponse(
                data = HomepageData(
                    heroComic = hero,
                    latestComics = latestComics.take(20).map { HomeComicItem(it.title, it.url, it.cover, it.type, it.latestChapter) },
                    popularComics = popularComics.take(20).map { HomeComicItem(it.title, it.url, it.cover, it.type, it.latestChapter) },
                    source = "komikindo",
                ),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ApiResponse(error = e.message ?: "Failed to fetch Komikindo home")
        }
    }

    @Suppress("kotlin:S107")
    suspend fun browse(
        page: Int = 1,
        title: String? = null,
        type: String? = null,
        status: String? = null,
        format: String? = null,
        order: String? = null,
        genre: List<String>? = null,
        demografis: List<String>? = null,
        konten: List<String>? = null,
        tema: List<String>? = null,
    ): ApiResponse<BrowseData> = try {
        val url = buildString {
            append("$base/daftar-manga/")
            if (page > 1) append("page/$page/")
            val params = mutableListOf<Pair<String, String>>()
            title?.takeIf { it.isNotBlank() }?.let { params += "title" to it }
            type?.takeIf { it.isNotBlank() && it != "_all" }?.let { params += "type" to it }
            status?.takeIf { it.isNotBlank() && it != "_all" }?.let { params += "status" to it }
            format?.takeIf { it.isNotBlank() && it != "_all" }?.let { params += "format" to it }
            order?.takeIf { it.isNotBlank() && it != "_default" }?.let { params += "order" to it }
            genre?.forEach { params += "genre[]" to it }
            demografis?.forEach { params += "demografis[]" to it }
            konten?.forEach { params += "konten[]" to it }
            tema?.forEach { params += "tema[]" to it }
            if (params.isNotEmpty()) {
                append("?")
                append(
                    params.joinToString("&") { (k, v) ->
                        "$k=${java.net.URLEncoder.encode(v, "UTF-8")}"
                    },
                )
            }
        }
        val html = http.get(url)
        val doc = Jsoup.parse(html)
        val comics = KomikindoParser.parseBrowseComics(doc)
        val filters = KomikindoParser.parseFilterOptions(doc)
        val totalPages = doc.select(".pagination .page-numbers:not(.dots)")
            .mapNotNull { it.text().trim().toIntOrNull() }
            .maxOrNull()
        ApiResponse(
            data = BrowseData(
                comics = comics,
                filterOptions = filters,
                pagination = PaginationData(currentPage = page, totalPages = totalPages),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to browse Komikindo")
    }

    suspend fun search(query: String, page: Int): ApiResponse<SearchData> {
        // Komikindo search is not standardised here; fallback to browse title filter.
        val res = browse(page = page, title = query)
        return if (res.data != null) {
            ApiResponse(data = SearchData(comics = res.data.comics, totalPages = res.data.pagination?.totalPages))
        } else {
            ApiResponse(error = res.error ?: "Failed to search")
        }
    }

    suspend fun getComicDetail(slug: String): ApiResponse<ComicDetail> = try {
        val html = http.get("$base/komik/${java.net.URLEncoder.encode(slug, "UTF-8")}/")
        val doc = Jsoup.parse(html)
        val titleRaw = doc.selectFirst(".entry-title[itemprop='name']")?.text()?.trim()
            ?: doc.selectFirst("h1.entry-title")?.text()?.trim()
            ?: doc.selectFirst(".entry-title")?.text()?.trim()
            ?: slug
        val title = titleRaw.replace(Regex("^Komik\\s+", RegexOption.IGNORE_CASE), "").trim().ifBlank { slug }
        val thumb = doc.selectFirst(".thumb img")?.attr("src")
            ?.takeIf { it.isNotBlank() }?.let { KomikindoParser.absUrl(it) }
        val rating = doc.selectFirst("i[itemprop='ratingValue']")?.text()?.trim()
        val synopsis = doc.selectFirst("#sinopsis .entry-content p")?.text()?.trim()
            ?: doc.selectFirst("#sinopsis")?.text()?.trim()
            ?: doc.selectFirst(".entry-content")?.text()?.trim()

        val genres = doc.select(".genre-info a").map { it.text().trim() }.filter { it.isNotBlank() }
        val infoMap = KomikindoParser.parseInfoMap(doc)
        val status = KomikindoParser.infoFirst(infoMap, "status")
        val type = KomikindoParser.infoFirst(infoMap, "jenis komik")
        val author = KomikindoParser.infoFirst(infoMap, "pengarang")
        val illustrator = KomikindoParser.infoFirst(infoMap, "ilustrator")
        val alternativeTitles = KomikindoParser.infoList(infoMap, "judul alternatif")
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val lastUpdated =
            doc.selectFirst("meta[property='article:modified_time']")?.attr("content")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: doc.selectFirst(".infox .spe time")?.text()?.trim()?.takeIf { it.isNotBlank() }

        val officialLinks = KomikindoParser.parseExternalLinks(doc, "official")
        val infoLinks = KomikindoParser.parseExternalLinks(doc, "informasi")
        val recommendations = KomikindoParser.parseRecommendations(doc)
        val chapters = KomikindoChapterParser.parseChapters(doc, slug)

        ApiResponse(
            data = ComicDetail(
                id = slug,
                title = title,
                thumbnail = thumb,
                rating = rating,
                status = status,
                type = type,
                author = author,
                illustrator = illustrator,
                genres = genres,
                synopsis = synopsis,
                chapterList = chapters,
                alternativeTitles = alternativeTitles,
                lastUpdated = lastUpdated,
                officialLinks = officialLinks,
                infoLinks = infoLinks,
                recommendations = recommendations,
                source = "komikindo",
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komikindo detail")
    }

    suspend fun getChapter(slug: String, chapter: String): ApiResponse<ChapterData> {
        // chapter param is the chapterId in our app routes (taken from chapterList url last segment)
        return try {
            val detail = getComicDetail(slug).data
            val realUrl = detail?.chapterList?.firstOrNull { it.id == chapter }?.realUrl
            if (realUrl.isNullOrBlank()) return ApiResponse(error = "Chapter not found")
            fetchChapterPages(realUrl)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ApiResponse(error = e.message ?: "Failed to fetch Komikindo chapter")
        }
    }

    suspend fun getChapter(slug: String, chapter: String, realUrl: String?): ApiResponse<ChapterData> = if (realUrl.isNullOrBlank()) getChapter(slug, chapter) else fetchChapterPages(realUrl)

    private suspend fun fetchChapterPages(realUrl: String): ApiResponse<ChapterData> = try {
        val headers = mapOf("Referer" to "$base/")
        val html = http.get(realUrl, headers = headers)
        val doc = Jsoup.parse(html)
        ApiResponse(data = ChapterData(images = KomikindoChapterParser.extractChapterImageUrls(doc)))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch Komikindo chapter")
    }

    suspend fun genre(slug: String, page: Int): ApiResponse<GenrePageData> {
        // Minimal: use komikindo browse with genre[].
        val res = browse(page = page, genre = listOf(slug))
        return if (res.data != null) {
            ApiResponse(
                data = GenrePageData(
                    genreSlug = slug,
                    comics = res.data.comics.map { HomeComicItem(it.title, it.url, it.cover, it.type, it.latestChapter) },
                    pagination = res.data.pagination,
                ),
            )
        } else {
            ApiResponse(error = res.error ?: "Failed to fetch genre")
        }
    }

    suspend fun projects(page: Int): ApiResponse<ProjectsData> {
        // Not available directly; return empty.
        return ApiResponse(data = ProjectsData(projects = emptyList()))
    }

    suspend fun updates(page: Int): ApiResponse<UpdatesData> {
        // Komikindo updates page isn't handled; reuse browse page.
        val res = browse(page = page)
        return if (res.data != null) {
            ApiResponse(data = UpdatesData(comics = res.data.comics, totalPages = res.data.pagination?.totalPages))
        } else {
            ApiResponse(error = res.error ?: "Failed to fetch updates")
        }
    }
}
