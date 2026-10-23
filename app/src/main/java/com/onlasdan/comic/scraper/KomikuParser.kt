package com.onlasdan.comic.scraper

import com.onlasdan.comic.api.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parser HTML murni KomikuParser: tanpa jaringan, tanpa state — pisahan dari scraper (audit B4). */
internal object KomikuParser {
    private const val BASE = "https://komiku.org"
    private const val ATTR_DATA_SRC = "data-src"
    private const val SEL_LS2V_IMG = ".ls2v img"
    private const val SEL_LS4V_IMG = ".ls4v img"

    fun absUrl(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            java.net.URI(BASE).resolve(raw).toString()
        } catch (_: Exception) {
            raw
        }
    }

    fun rewriteCdn(raw: String): String {
        val s = raw.trim()
        return s
            .replace(Regex("^https?://img\\.komiku\\.id/", RegexOption.IGNORE_CASE), "https://cdn.komiku.co.id/")
            .replace(Regex("^//img\\.komiku\\.id/", RegexOption.IGNORE_CASE), "https://cdn.komiku.co.id/")
            .replace(Regex("^https?://thumbnail\\.komiku\\.to/", RegexOption.IGNORE_CASE), "https://thumbnail.komiku.org/")
            .replace(Regex("^//thumbnail\\.komiku\\.to/", RegexOption.IGNORE_CASE), "https://thumbnail.komiku.org/")
    }

    fun normalizeImageUrl(raw: String): String = absUrl(rewriteCdn(raw))

    fun internalizeComicUrl(raw: String): String {
        val abs = absUrl(raw).trim()
        if (abs.isBlank()) return ""
        val slug = abs.trimEnd('/').split("/").lastOrNull().orEmpty()
        return if (slug.isBlank()) "" else "/komiku/comic/$slug"
    }

    fun internalizeChapterUrl(raw: String): String {
        val abs = absUrl(raw).trim()
        if (abs.isBlank()) return ""
        val chapterSlug = abs.trimEnd('/').split("/").lastOrNull().orEmpty()
        if (chapterSlug.isBlank()) return ""
        val comicSlug = chapterSlug.split("-chapter-").firstOrNull().orEmpty().ifBlank { chapterSlug }
        return "/komiku/read/$comicSlug/$chapterSlug"
    }

    fun toHomeComicItemFromLs2(article: Element): HomeComicItem? {
        val titleEl = article.selectFirst(".ls2j h3 a, .ls2j h4 a, .ls2j a")
        val title = titleEl?.text()?.trim().orEmpty()
        val comicHref = titleEl?.attr("href")?.trim().orEmpty()
        if (title.isBlank() || comicHref.isBlank()) return null
        val chapterEl = article.selectFirst(".ls2j a.ls2l, .ls2j a.ls24") ?: article.select(".ls2j a").drop(1).firstOrNull()
        val chapterTitle = chapterEl?.text()?.trim()
        val chapterHref = chapterEl?.attr("href")?.trim()
        val typeWithStats = article.selectFirst(".ls2j .ls2t")?.text()?.trim().orEmpty()
        val type = typeWithStats.split(Regex("[·•\\s]+")).firstOrNull().orEmpty()
        val imgEl = article.selectFirst(SEL_LS2V_IMG) ?: article.selectFirst("img")
        val coverRaw = imgEl?.attr(ATTR_DATA_SRC)?.ifBlank {
            imgEl.attr("data-original").ifBlank {
                imgEl.attr("src")
            }
        } ?: imgEl?.attr("src").orEmpty()

        return HomeComicItem(
            title = title,
            url = internalizeComicUrl(comicHref),
            cover = coverRaw.takeIf { it.isNotBlank() && !it.contains("lazy.jpg", ignoreCase = true) }?.let { normalizeImageUrl(it) },
            type = type.ifBlank { null },
            latestChapter = HomeLatestChapter(
                title = chapterTitle,
                url = chapterHref?.takeIf { it.isNotBlank() }?.let { internalizeChapterUrl(it) },
            ),
        )
    }

    fun toHomeComicItemFromLs4(article: Element): HomeComicItem? {
        val titleEl = article.selectFirst(".ls4j h4 a, .ls4j h3 a, .ls4j a")
        val title = titleEl?.text()?.trim().orEmpty()
        val comicHref = titleEl?.attr("href")?.trim().orEmpty()
        if (title.isBlank() || comicHref.isBlank()) return null
        val chapterEl = article.selectFirst(".ls4j a.ls24, .ls4j a.ls2l") ?: article.select(".ls4j a").drop(1).firstOrNull()
        val chapterTitle = chapterEl?.text()?.trim()
        val chapterHref = chapterEl?.attr("href")?.trim()
        val metaText = article.selectFirst(".ls4j .ls4s")?.text()?.trim().orEmpty()
        val type = metaText.split(Regex("[·•\\s]+")).firstOrNull().orEmpty()
        val imgEl = article.selectFirst(SEL_LS4V_IMG) ?: article.selectFirst("img")
        val coverRaw = imgEl?.attr(ATTR_DATA_SRC)?.ifBlank {
            imgEl.attr("data-original").ifBlank {
                imgEl.attr("src")
            }
        } ?: imgEl?.attr("src").orEmpty()

        return HomeComicItem(
            title = title,
            url = internalizeComicUrl(comicHref),
            cover = coverRaw.takeIf { it.isNotBlank() && !it.contains("lazy.jpg", ignoreCase = true) }?.let { normalizeImageUrl(it) },
            type = type.ifBlank { null },
            latestChapter = HomeLatestChapter(
                title = chapterTitle,
                url = chapterHref?.takeIf { it.isNotBlank() }?.let { internalizeChapterUrl(it) },
            ),
        )
    }

    fun toHomeComicItem(article: Element): HomeComicItem? = if (article.hasClass("ls4") || article.selectFirst(".ls4j") != null) {
        toHomeComicItemFromLs4(article)
    } else {
        toHomeComicItemFromLs2(article)
    }

    fun extractStats(doc: Document): HomeStats? {
        val text = doc.selectFirst("#Filter p")?.text().orEmpty()
        if (text.isBlank()) return null
        val titleMatch = Regex("Jelajahi\\s+([\\d\\.]+)\\s+judul").find(text)
        val chapterMatch = Regex("dan\\s+([\\d\\.]+)\\s+jumlah chapter", RegexOption.IGNORE_CASE).find(text)
        val totalTitles = titleMatch?.groupValues?.getOrNull(1)
        val totalChapters = chapterMatch?.groupValues?.getOrNull(1)
        if (totalTitles.isNullOrBlank() && totalChapters.isNullOrBlank()) return null
        return HomeStats(totalTitles = totalTitles, totalChapters = totalChapters)
    }

    fun parseBgeCards(html: String): List<ComicListItem> {
        val doc = Jsoup.parse(html)
        return doc.select(".bge").mapNotNull { parseBgeCard(it) }
    }

    private fun bgeCover(img: Element?): String? {
        val coverRaw = img?.attr(ATTR_DATA_SRC)?.ifBlank {
            img.attr("data-original").ifBlank {
                img.attr("src")
            }
        } ?: img?.attr("src").orEmpty()
        return coverRaw.takeIf { it.isNotBlank() && !it.contains("lazy.jpg", ignoreCase = true) }?.let { normalizeImageUrl(it) }
    }

    private fun parseBgeCard(el: Element): ComicListItem? {
        val title = el.selectFirst(".kan h3")?.text()?.trim().orEmpty()
        val comicHref = el.selectFirst(".kan > a")?.attr("href")
            ?: el.selectFirst(".bgei a")?.attr("href").orEmpty()
        val cover = bgeCover(el.selectFirst(".bgei img"))
        val type = el.selectFirst(".tpe1_inf b")?.text()?.trim()
        val latestLink = el.select(".new1").lastOrNull()?.selectFirst("a")
        val latestTitle = latestLink?.select("span")?.lastOrNull()?.text()?.trim()
        val latestUrl = latestLink?.attr("href")?.takeIf { it.isNotBlank() }

        if (title.isBlank() || comicHref.isBlank()) return null
        return ComicListItem(
            title = title,
            url = "/komiku/comic/" + comicHref.trim().trimEnd('/').split("/").last(),
            cover = cover?.let { normalizeImageUrl(it) },
            type = type,
            latestChapter = if (!latestTitle.isNullOrBlank() || !latestUrl.isNullOrBlank()) {
                HomeLatestChapter(
                    title = latestTitle,
                    url = latestUrl?.let { href ->
                        val chapterSlug = href.trim().trimEnd('/').split("/").last()
                        val comicSlug = chapterSlug.split("-chapter-").firstOrNull().orEmpty().ifBlank { chapterSlug }
                        "/komiku/read/$comicSlug/$chapterSlug"
                    },
                )
            } else {
                null
            },
        )
    }

    fun parseKomikuGenreOptions(html: String): List<BrowseFilterOption> {
        val doc = Jsoup.parse(html)
        return doc.select("select[name='genre'] option").mapNotNull { opt ->
            val value = opt.attr("value").trim()
            val text = opt.text().replace(Regex("\\s+"), " ").trim()
            if (value.isBlank() || text.isBlank()) return@mapNotNull null
            // "Adventure (2.087)" -> label "Adventure", value "adventure"
            val match = Regex("^(.*)\\((.*)\\)\\s*$").find(text)
            val name = (match?.groupValues?.getOrNull(1) ?: text).trim()
            if (name.isBlank()) return@mapNotNull null
            BrowseFilterOption(label = name, value = value)
        }.filter { it.value.lowercase() != "all" }
    }
}
