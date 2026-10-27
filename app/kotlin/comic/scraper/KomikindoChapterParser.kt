package comic.scraper

import comic.api.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parser halaman chapter KomikindoChapterParser: pisahan dari parser utama (audit B4). */
internal object KomikindoChapterParser {

    fun internalReadUrlFromChapterHref(comicSlug: String, href: String): String {
        val chapterId = href.trim().trimEnd('/').split("/").lastOrNull().orEmpty()
        return if (chapterId.isBlank()) "" else "/komikindo/read/$comicSlug/$chapterId"
    }

    fun extractChapterNumber(text: String): Double {
        val m = Regex("(\\d+(?:\\.\\d+)?)").find(text)
        return m?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    }

    fun parseChapters(doc: Document, slug: String): List<ChapterItem> {
        val chapterSelectors = listOf("#chapterlist ul li", ".eps_lst .listeps ul li", "#chapterlist li", ".eps_lst li")
        for (sel in chapterSelectors) {
            val nodes = doc.select(sel)
            if (nodes.isEmpty()) continue
            val parsed = nodes.mapNotNull { li ->
                val rawHref = li.selectFirst("a")?.attr("href")?.trim().orEmpty()
                val chUrl = rawHref.takeIf { it.isNotBlank() }?.let { KomikindoParser.absUrl(it) } ?: return@mapNotNull null
                val id = chUrl.trimEnd('/').split("/").lastOrNull().orEmpty()
                if (id.isBlank()) return@mapNotNull null
                val chTitle = li.selectFirst(".chapternum, .lchx a")?.text()?.trim()
                    ?: li.selectFirst("a")?.text()?.trim().orEmpty()
                val last = li.selectFirst(".chapterdate, .dt a, .date")?.text()?.trim()
                ChapterItem(
                    id = id,
                    number = null,
                    title = chTitle,
                    url = "/komikindo/read/$slug/$id",
                    realUrl = chUrl,
                    lastUpdated = last,
                )
            }
            if (parsed.isNotEmpty()) return parsed.sortedByDescending { extractChapterNumber(it.title) }
        }
        return emptyList()
    }

    fun extractChapterImageUrls(doc: Document): List<String> {
        val readerSelectors = listOf("#readerarea img", ".reader-area img", ".reading-content img", ".chapter-area img", "article img")
        val imgEls = readerSelectors.asSequence()
            .map { doc.select(it) }
            .firstOrNull { it.isNotEmpty() }
            ?: doc.select("img")

        return imgEls
            .mapNotNull { chapterImageUrl(it) }
            .filterNot { it.contains("loading", ignoreCase = true) }
            .distinct()
            .map { KomikindoParser.absUrl(it) }
    }

    // Satu elemen img -> URL halaman, atau null bila bukan gambar halaman
    // (logo/placeholder dibuang).

    fun chapterImageUrl(img: Element): String? {
        val src = img.attr("data-src").ifBlank { img.attr("src") }.ifBlank { null } ?: return null
        val pathOnly = src.substringBefore('?').substringBefore('#')
        val isPageImage = src.contains("wp-content", ignoreCase = true) || src.contains("/data/") || pathOnly.endsWith(".jpg", ignoreCase = true) || pathOnly.endsWith(".png", ignoreCase = true) || pathOnly.endsWith(".webp", ignoreCase = true)
        val isJunk = src.contains("logo", ignoreCase = true) || img.attr("alt").contains("komikindo", ignoreCase = true) || img.className().orEmpty().contains("logo", ignoreCase = true)
        return src.takeIf { isPageImage && !isJunk }
    }
}
