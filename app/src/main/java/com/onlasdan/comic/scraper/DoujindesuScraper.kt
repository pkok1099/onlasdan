package com.onlasdan.comic.scraper

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.onlasdan.comic.api.*
import com.onlasdan.comic.ui.source.Genre
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Scraper/API client for DoujinDesu (doujin.desu.xxx).
 * Handles XOR-encrypted responses (_enc_resp_) using client-side time-slotted key derivation.
 */
class DoujindesuScraper {

    companion object {
        const val BASE = "https://doujin.desu.xxx"
        const val SOURCE = "doujindesu"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/123.0.0.0 Mobile Safari/537.36"
        private const val PAGE_SIZE = 20
        private const val SALT = "doujindesu-scrapers-cannot-read-this-super-secret-salt-2026-v2"
        private const val SLOT_MS = 3600000L
    }

    private val client = HttpClients.client

    // ---------- Crypto / Decryption ----------

    internal fun deriveKey(slot: Long): String {
        val s = "${SALT}_$slot"
        var n = 0
        for (i in 0 until s.length) {
            n = (n shl 5) - n + s[i].code
        }
        var c = if (n == Int.MIN_VALUE) 2147483648L else kotlin.math.abs(n).toLong()
        if (c == 0L) c = 123456789L
        val out = StringBuilder(32)
        for (i in 0 until 32) {
            c = (c * 1664525L + 1013904223L) % 4294967296L
            out.append((33 + (c % 93L).toInt()).toChar())
        }
        return out.toString()
    }

    internal fun xorStream(hex: String, key: String): String {
        val len = hex.length
        val raw = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            raw[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        val out = StringBuilder(raw.size)
        var l = 42
        val kl = key.length
        for (x in raw.indices) {
            val y = raw[x].toInt() and 0xFF
            val k = key[x % kl].code
            val dec = (y xor k xor (x * 13) xor l) and 0xFF
            out.append(dec.toChar())
            l = (l + y) % 256
        }
        return out.toString()
    }

    internal fun decrypt(hex: String): String {
        val slot = System.currentTimeMillis() / SLOT_MS
        val slots = longArrayOf(slot, slot - 1, slot + 1)
        for (s in slots) {
            try {
                val key = deriveKey(s)
                val stream = xorStream(hex, key)
                val decoded = URLDecoder.decode(stream, StandardCharsets.UTF_8.name())
                if (decoded.startsWith("{") || decoded.startsWith("[")) {
                    return decoded
                }
            } catch (_: Exception) {
            }
        }
        throw IllegalStateException("Failed to decrypt doujindesu _enc_resp_")
    }

    // ---------- HTTP & Parsing ----------

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw RuntimeException("HTTP ${res.code} for ${req.url}")
            res.body?.string().orEmpty()
        }
    }

    private suspend fun apiGet(path: String): JsonElement {
        val raw = httpGet(BASE + path)
        val elem = JsonParser.parseString(raw)
        if (elem.isJsonObject) {
            val obj = elem.asJsonObject
            val enc = obj.get("_enc_resp_")?.takeIf { !it.isJsonNull }?.asString
            if (enc != null) {
                val decrypted = decrypt(enc)
                return JsonParser.parseString(decrypted)
            }
        }
        return elem
    }

    private fun JsonObject.optString(key: String): String? = get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun unescapeHtml(text: String): String = text.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&hellip;", "…")
        .replace("&ldquo;", "“")
        .replace("&rdquo;", "”")
        .replace("&amp;", "&")

    internal fun cleanHtml(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val unescaped = unescapeHtml(raw)
        val text = unescaped
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n")
            .trim()
        return text.ifBlank { null }
    }

    internal fun fmtChapterNumber(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()

    internal fun parseMangaListItem(o: JsonObject): ComicListItem {
        val slug = o.optString("slug") ?: o.optString("id").orEmpty()
        val chArr = o.getAsJsonArray("chapters")
        val latestCh = chArr?.firstOrNull()?.asJsonObject?.let { chObj ->
            val num = fmtChapterNumber(chObj.get("chapter_number").asDoubleOrNull() ?: 0.0)
            val title = chObj.optString("title")
            HomeLatestChapter(
                title = if (title.isNullOrBlank()) "Ch. $num" else title,
                url = chObj.optString("id"),
            )
        }
        val ratingVal = o.get("rating").asDoubleOrNull()
        val dateVal = o.optString("updated_at") ?: chArr?.firstOrNull()?.asJsonObject?.optString("created_at")
        return ComicListItem(
            title = o.optString("title") ?: "Unknown",
            url = "/doujindesu/manga/$slug",
            cover = o.optString("cover_url"),
            type = o.optString("type"),
            rating = ratingVal,
            latestChapter = latestCh,
            lastUpdated = dateVal,
        )
    }

    internal fun parseMangaDetail(o: JsonObject): ComicDetail {
        val slug = o.optString("slug") ?: o.optString("id").orEmpty()
        val alt = o.optString("alt_titles")?.takeIf { it.isNotBlank() }
        val genres = mutableListOf<String>()
        val groups = mutableListOf<String>()
        val characters = mutableListOf<String>()
        val seriesList = mutableListOf<String>()
        val authors = mutableListOf<String>()

        val rawTermList = o.optString("term_list")
        if (!rawTermList.isNullOrBlank()) {
            val terms = rawTermList.split("|")
            for (term in terms) {
                val parts = term.split(":")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val kind = parts[1].trim().lowercase()
                    if (name.isNotBlank() && !name.equals("N/A", ignoreCase = true)) {
                        when (kind) {
                            "genre" -> if (!genres.contains(name)) genres.add(name)
                            "group" -> if (!groups.contains(name)) groups.add(name)
                            "character" -> if (!characters.contains(name)) characters.add(name)
                            "series" -> if (!seriesList.contains(name)) seriesList.add(name)
                            "author" -> if (!authors.contains(name)) authors.add(name)
                        }
                    }
                }
            }
        }
        if (genres.isEmpty()) {
            o.getAsJsonArray("manga_genres")?.mapNotNull {
                it.asJsonObject.getAsJsonObject("genres")?.optString("name")
            }?.let { genres.addAll(it) }
        }
        val authorField = o.optString("author")
        if (!authorField.isNullOrBlank() && !authors.contains(authorField)) {
            authors.add(0, authorField)
        }
        val artistField = o.optString("artist")
        val viewsVal = o.get("views")?.takeIf { !it.isJsonNull }?.let {
            if (it.isJsonPrimitive) it.asString else null
        }

        return ComicDetail(
            id = slug,
            title = o.optString("title") ?: "Unknown",
            thumbnail = o.optString("cover_url"),
            rating = o.optString("rating"),
            status = when (o.optString("status")?.lowercase()) {
                "publishing" -> "Ongoing"
                "completed" -> "Completed"
                else -> o.optString("status")
            },
            type = o.optString("type")?.replaceFirstChar { it.uppercase() },
            author = if (authors.isNotEmpty()) authors.joinToString(", ") else authorField,
            illustrator = artistField,
            genres = genres,
            synopsis = cleanHtml(o.optString("description")),
            alternativeTitles = alt?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
            lastUpdated = o.optString("updated_at"),
            source = SOURCE,
            groups = groups,
            series = seriesList,
            characters = characters,
            views = viewsVal,
        )
    }

    internal fun parseChapterListItem(o: JsonObject, mangaSlug: String): ChapterItem {
        val cid = o.optString("id").orEmpty()
        val num = fmtChapterNumber(o.get("chapter_number").asDoubleOrNull() ?: 0.0)
        val rawTitle = o.optString("title").orEmpty()
        val rawDl = o.optString("download_link")?.takeIf { it.isNotBlank() }
            ?: o.optString("download_url")?.takeIf { it.isNotBlank() }
            ?: o.optString("pdf_url")?.takeIf { it.isNotBlank() }
            ?: if (o.get("has_pdf")?.takeIf { !it.isJsonNull }?.asBoolean == true && cid.isNotBlank()) {
                "/api/chapters/$cid/download-pdf"
            } else {
                null
            }
        val dl = rawDl?.let {
            if (it.startsWith("http")) it else "$BASE$it"
        }
        return ChapterItem(
            id = cid,
            number = num,
            title = rawTitle.ifBlank { "Chapter $num" },
            url = "/doujindesu/read/$mangaSlug/$cid",
            lastUpdated = o.optString("created_at"),
            downloadUrl = dl,
        )
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

    // ---------- Public API ----------

    suspend fun getHome(): ApiResponse<HomepageData> = try {
        val latestElem = apiGet("/api/manga?limit=20&sort=latest_chapter&type=doujinshi")
        val popularElem = apiGet("/api/manga?limit=10&sort=popular&type=doujinshi")
        val latestItems = latestElem.asJsonArray.map { parseMangaListItem(it.asJsonObject).toHomeItem() }
        val popularItems = popularElem.asJsonArray.map { parseMangaListItem(it.asJsonObject).toHomeItem() }
        val hero = popularItems.firstOrNull()?.let {
            HomeHeroComic(title = it.title, url = it.url, cover = it.cover)
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
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu home")
    }

    suspend fun browse(
        page: Int,
        orderby: String?,
        tipe: String?,
        status: String?,
        genre: String?,
        sorttime: String?,
        search: String? = null,
    ): ApiResponse<BrowseData> = try {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val params = mutableListOf("limit=$PAGE_SIZE", "offset=$offset")
        when (orderby) {
            "popular" -> params += "sort=popular"
            "rating" -> params += "sort=rating"
            "views" -> params += "sort=views"
            else -> params += "sort=latest_chapter"
        }
        val cleanType = when (tipe?.lowercase()?.trim()) {
            "doujinshi" -> "doujinshi"
            "manga" -> "manga"
            "manhwa" -> "manhwa"
            else -> null
        }
        if (cleanType != null) {
            params += "type=" + URLEncoder.encode(cleanType, "UTF-8")
        }
        val cleanStatus = when (status?.lowercase()?.trim()) {
            "publishing", "ongoing" -> "publishing"
            "completed" -> "completed"
            else -> null
        }
        if (cleanStatus != null) {
            params += "status=" + URLEncoder.encode(cleanStatus, "UTF-8")
        }
        if (!genre.isNullOrBlank()) {
            params += "genre=" + URLEncoder.encode(genre.trim(), "UTF-8")
        }
        if (!search.isNullOrBlank()) {
            params += "search=" + URLEncoder.encode(search.trim(), "UTF-8")
        }
        val elem = apiGet("/api/manga?" + params.joinToString("&"))
        val items = elem.asJsonArray.map { parseMangaListItem(it.asJsonObject) }
        ApiResponse(
            data = BrowseData(
                comics = items,
                filterOptions = doujindesuFilterOptions(),
                pagination = PaginationData(
                    currentPage = page,
                    totalPages = null,
                ),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to browse doujindesu")
    }

    private fun doujindesuFilterOptions() = BrowseFilterOptions(
        genres = GenreMasterData.doujindesuMasterGenres.map { BrowseFilterOption(it.name, it.id) },
        status = listOf(
            BrowseFilterOption("All", ""),
            BrowseFilterOption("Ongoing", "publishing"),
            BrowseFilterOption("Completed", "completed"),
        ),
        type = listOf(
            BrowseFilterOption("All", ""),
            BrowseFilterOption("Doujinshi", "doujinshi"),
            BrowseFilterOption("Manga", "manga"),
            BrowseFilterOption("Manhwa", "manhwa"),
        ),
        orderBy = listOf(
            BrowseFilterOption("Latest Updates", "latest"),
            BrowseFilterOption("Most Popular", "popular"),
            BrowseFilterOption("Top Views", "views"),
            BrowseFilterOption("Highest Rating", "rating"),
        ),
        sortTime = emptyList(),
    )

    suspend fun search(query: String, page: Int): ApiResponse<SearchData> = try {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val q = URLEncoder.encode(query, "UTF-8")
        val elem = apiGet("/api/manga?search=$q&limit=$PAGE_SIZE&offset=$offset")
        val items = elem.asJsonArray.map { parseMangaListItem(it.asJsonObject) }
        ApiResponse(
            data = SearchData(
                comics = items,
                totalPages = null,
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to search doujindesu")
    }

    suspend fun getComicDetail(slug: String): ApiResponse<ComicDetail> = try {
        val safeSlug = URLEncoder.encode(slug, "UTF-8")
        val elem = apiGet("/api/manga/$safeSlug")
        val obj = elem.asJsonObject
        val detail = parseMangaDetail(obj)
        val chapters = obj.getAsJsonArray("chapters")?.map {
            parseChapterListItem(it.asJsonObject, slug)
        }?.sortedByDescending { it.number?.toDoubleOrNull() ?: 0.0 }.orEmpty()
        ApiResponse(data = detail.copy(chapterList = chapters))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu detail")
    }

    // Chapter tanpa UUID (bug A16): tebakan slug yang lama dikirim sebagai id
    // API pasti 404 tanpa pemulihan. Daftar chapter di detail komik memuat id
    // aslinya — cocokkan di sana dulu; gagal -> perilaku lama (tebakan slug).
    private suspend fun resolveChapterUuid(slug: String, chapter: String): String {
        val guess = chapter.trim().substringAfterLast('/')
        if (slug.isBlank()) return guess
        val chapters = getComicDetail(slug).data?.chapterList.orEmpty()
        val match = chapters.firstOrNull { item ->
            val id = item.id?.takeIf { it.isNotBlank() } ?: return@firstOrNull false
            chapter.contains(id) || id.contains(guess) || item.url?.contains(guess) == true
        }
        return match?.id?.takeIf { it.isNotBlank() } ?: guess
    }

    suspend fun getChapter(slug: String, chapter: String): ApiResponse<ChapterData> = try {
        val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").find(chapter)?.value
            ?: resolveChapterUuid(slug, chapter)
        val elem = apiGet("/api/chapters/$uuid")
        val obj = elem.asJsonObject
        // Website memuat gambar via /api/proxy-image, bukan URL S3 langsung (403).
        val urls = obj.getAsJsonArray("content_urls")?.map {
            "$BASE/api/proxy-image?url=" + URLEncoder.encode(it.asString, "UTF-8")
        }.orEmpty()
        ApiResponse(data = ChapterData(images = urls))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu chapter")
    }

    suspend fun updates(page: Int): ApiResponse<UpdatesData> = try {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val elem = apiGet("/api/manga?limit=$PAGE_SIZE&offset=$offset&sort=latest_chapter")
        val items = elem.asJsonArray.map { parseMangaListItem(it.asJsonObject) }
        ApiResponse(data = UpdatesData(comics = items))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu updates")
    }

    suspend fun projects(page: Int): ApiResponse<ProjectsData> = try {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val elem = apiGet("/api/manga?limit=$PAGE_SIZE&offset=$offset&sort=popular")
        val items = elem.asJsonArray.map { parseMangaListItem(it.asJsonObject) }
        ApiResponse(data = ProjectsData(projects = items))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu projects")
    }

    suspend fun genre(slug: String, page: Int): ApiResponse<GenrePageData> = try {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val g = URLEncoder.encode(slug, "UTF-8")
        val elem = apiGet("/api/manga?genre=$g&limit=$PAGE_SIZE&offset=$offset")
        val items = elem.asJsonArray.map { parseMangaListItem(it.asJsonObject).toHomeItem() }
        ApiResponse(
            data = GenrePageData(
                genreName = slug,
                genreSlug = slug,
                comics = items,
                pagination = PaginationData(currentPage = page),
            ),
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu genre")
    }

    suspend fun getGenres(): ApiResponse<List<Genre>> = try {
        val elem = apiGet("/api/genres")
        val list = elem.asJsonArray.mapNotNull { item ->
            val obj = item.asJsonObject
            val name = obj.optString("name")?.trim() ?: return@mapNotNull null
            if (name.isBlank() || name == "[]") return@mapNotNull null
            val rawSlug = obj.optString("slug")?.trim()
            val slug = if (!rawSlug.isNullOrBlank()) rawSlug else name.lowercase().replace(" ", "-")
            if (slug.isBlank()) return@mapNotNull null
            val count = obj.get("manga_count")?.takeIf { !it.isJsonNull }?.asInt
                ?: obj.getAsJsonObject("_count")?.get("manga_genres")?.takeIf { !it.isJsonNull }?.asInt
            Genre(id = slug, name = name, count = count)
        }
        ApiResponse(data = list)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ApiResponse(error = e.message ?: "Failed to fetch doujindesu genres")
    }
}
