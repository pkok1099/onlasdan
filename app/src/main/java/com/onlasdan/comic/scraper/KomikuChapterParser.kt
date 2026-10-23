package com.onlasdan.comic.scraper

import com.onlasdan.comic.api.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parser halaman chapter KomikuChapterParser: pisahan dari parser utama (audit B4). */
internal object KomikuChapterParser {

    private const val ATTR_DATA_SRC = "data-src"

    fun findChapterRealUrl(detail: ComicDetail?, chapterId: String): String? = detail?.chapterList?.firstOrNull { it.id == chapterId }?.realUrl

    fun extractChapterImageSrc(img: Element): String? {
        val src = img.attr(ATTR_DATA_SRC).ifBlank {
            img.attr("data-original").ifBlank {
                img.attr("src")
            }
        }
        if (src.isBlank()) return null
        if (src.contains("Loading.gif", ignoreCase = true)) return null
        if (src.contains("komikuplus2.jpg", ignoreCase = true)) return null
        if (src.contains("komiku-promosi", ignoreCase = true)) return null
        if (src.contains("promooktober", ignoreCase = true)) return null
        if (src.contains("google.svg", ignoreCase = true)) return null
        if (src.contains("gravatar.com", ignoreCase = true)) return null
        if (src.contains("lazy.jpg", ignoreCase = true)) return null
        return KomikuParser.normalizeImageUrl(src)
    }

    fun extractImagesBySelectors(doc: Document, selectors: List<String>): List<String> {
        for (sel in selectors) {
            val candidates = doc.select(sel).mapNotNull(::extractChapterImageSrc).distinct()
            if (candidates.isNotEmpty()) return candidates
        }
        return emptyList()
    }

    fun extractImagesFromChapterDataScript(doc: Document): List<String> {
        val scriptContent = doc.select("script")
            .asSequence()
            .map { it.data() }
            .firstOrNull { it.contains("var chapterData") }
            ?: return emptyList()

        val rawJson = Regex("var\\s+chapterData\\s*=\\s*({[^;]+});")
            .find(scriptContent)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        if (rawJson.isBlank()) return emptyList()

        val cleaned = rawJson
            .replace("'", "\"")
            .replace(Regex("([{,]\\s*)(\\w+):"), "$1\"$2\":")
        return chapterDataImageUrls(cleaned)
    }

    fun extractImagesFromAllImages(doc: Document): List<String> {
        val hostRegex = Regex("(komiku\\.org|komiku\\.id|komiku\\.co\\.id)", RegexOption.IGNORE_CASE)
        return doc.select("img")
            .mapNotNull { img ->
                val src = img.attr("src").ifBlank { img.attr(ATTR_DATA_SRC) }
                if (src.isBlank()) return@mapNotNull null
                if (src.contains("thumbnail")) return@mapNotNull null
                if (!hostRegex.containsMatchIn(src)) return@mapNotNull null
                KomikuParser.normalizeImageUrl(src)
            }
            .distinct()
    }

    private fun chapterDataImageUrls(cleaned: String): List<String> = try {
        val json = org.json.JSONObject(cleaned)
        val arr: org.json.JSONArray? = when {
            json.has("images") -> json.optJSONArray("images")
            json.has("img") -> json.optJSONArray("img")
            json.has("image") -> json.optJSONArray("image")
            else -> null
        }
        if (arr == null) {
            emptyList()
        } else {
            buildList {
                for (i in 0 until arr.length()) {
                    val raw = arr.optString(i)
                    if (raw.isNullOrBlank()) continue
                    add(KomikuParser.normalizeImageUrl(raw))
                }
            }.distinct()
        }
    } catch (_: Exception) {
        emptyList()
    }
}
