package comic.scraper

import comic.api.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parser HTML murni KomikindoParser: tanpa jaringan, tanpa state — pisahan dari scraper (audit B4). */
internal object KomikindoParser {
    private const val BASE = "https://komikindo.ch"

    internal val fallbackGenres = listOf(
        BrowseFilterOption("Action", "action"),
        BrowseFilterOption("Adult", "adult"),
        BrowseFilterOption("Adventure", "adventure"),
        BrowseFilterOption("Arts", "arts"),
        BrowseFilterOption("Boys' Love", "boys-love"),
        BrowseFilterOption("Comedy", "comedy"),
        BrowseFilterOption("Crime", "crime"),
        BrowseFilterOption("Demons", "demons"),
        BrowseFilterOption("Drama", "drama"),
        BrowseFilterOption("Drama Supernatural", "drama-supernatural"),
        BrowseFilterOption("Ecchi", "ecchi"),
        BrowseFilterOption("Fantasy", "fantasy"),
        BrowseFilterOption("Gender Bender", "gender-bender"),
        BrowseFilterOption("Girls' Love", "girls-love"),
        BrowseFilterOption("Harem", "harem"),
        BrowseFilterOption("Historical", "historical"),
        BrowseFilterOption("Horror", "horror"),
        BrowseFilterOption("Isekai", "isekai"),
        BrowseFilterOption("Josei", "josei"),
        BrowseFilterOption("Life", "life"),
        BrowseFilterOption("Magical Girls", "magical-girls"),
        BrowseFilterOption("Martial", "martial"),
        BrowseFilterOption("Martial Arts", "martial-arts"),
        BrowseFilterOption("Mature", "mature"),
        BrowseFilterOption("Mecha", "mecha"),
        BrowseFilterOption("Medical", "medical"),
        BrowseFilterOption("Music", "music"),
        BrowseFilterOption("Mystery", "mystery"),
        BrowseFilterOption("Philosophical", "philosophical"),
        BrowseFilterOption("Psychological", "psychological"),
        BrowseFilterOption("Romance", "romance"),
        BrowseFilterOption("School", "school"),
        BrowseFilterOption("School Life", "school-life"),
        BrowseFilterOption("Sci-Fi", "sci-fi"),
        BrowseFilterOption("Seinen", "seinen"),
        BrowseFilterOption("Shoujo", "shoujo"),
        BrowseFilterOption("Shoujo Ai", "shoujo-ai"),
        BrowseFilterOption("Shounen", "shounen"),
        BrowseFilterOption("Shounen Ai", "shounen-ai"),
        BrowseFilterOption("Slice of Life", "slice-of-life"),
        BrowseFilterOption("Smut", "smut"),
        BrowseFilterOption("Sports", "sports"),
        BrowseFilterOption("Superhero", "superhero"),
        BrowseFilterOption("Supernatural", "supernatural"),
        BrowseFilterOption("Thriller", "thriller"),
        BrowseFilterOption("Traged", "traged"),
        BrowseFilterOption("Tragedy", "tragedy"),
        BrowseFilterOption("Wuxia", "wuxia"),
        BrowseFilterOption("Yuri", "yuri"),
    )

    internal val fallbackDemografis = listOf(
        BrowseFilterOption("Josei", "josei"),
        BrowseFilterOption("Seinen", "seinen"),
        BrowseFilterOption("Shoujo", "shoujo"),
        BrowseFilterOption("Shounen", "shounen"),
    )

    internal val fallbackKonten = listOf(
        BrowseFilterOption("Ecchi", "ecchi"),
        BrowseFilterOption("Gore", "gore"),
        BrowseFilterOption("Sexual Violence", "sexual-violence"),
        BrowseFilterOption("Smut", "smut"),
    )

    internal val fallbackTema = listOf(
        BrowseFilterOption("Aliens", "aliens"),
        BrowseFilterOption("Animals", "animals"),
        BrowseFilterOption("Cooking", "cooking"),
        BrowseFilterOption("Crossdressing", "crossdressing"),
        BrowseFilterOption("Delinquents", "delinquents"),
        BrowseFilterOption("Demons", "demons"),
        BrowseFilterOption("Ecchi", "ecchi"),
        BrowseFilterOption("Genderswap", "genderswap"),
        BrowseFilterOption("Ghosts", "ghosts"),
        BrowseFilterOption("Gore", "gore"),
        BrowseFilterOption("Gyaru", "gyaru"),
        BrowseFilterOption("Harem", "harem"),
        BrowseFilterOption("Incest", "incest"),
        BrowseFilterOption("Loli", "loli"),
        BrowseFilterOption("Mafia", "mafia"),
        BrowseFilterOption("Magic", "magic"),
        BrowseFilterOption("Martial Arts", "martial-arts"),
        BrowseFilterOption("Military", "military"),
        BrowseFilterOption("Monster Girls", "monster-girls"),
        BrowseFilterOption("Monsters", "monsters"),
        BrowseFilterOption("Music", "music"),
        BrowseFilterOption("Ninja", "ninja"),
        BrowseFilterOption("Office Workers", "office-workers"),
        BrowseFilterOption("Police", "police"),
        BrowseFilterOption("Post-Apocalyptic", "post-apocalyptic"),
        BrowseFilterOption("Reincarnation", "reincarnation"),
        BrowseFilterOption("Reverse Harem", "reverse-harem"),
        BrowseFilterOption("Samurai", "samurai"),
        BrowseFilterOption("School Life", "school-life"),
        BrowseFilterOption("Shota", "shota"),
        BrowseFilterOption("Smut", "smut"),
        BrowseFilterOption("Supernatural", "supernatural"),
        BrowseFilterOption("Survival", "survival"),
        BrowseFilterOption("Time Travel", "time-travel"),
        BrowseFilterOption("Traditional Games", "traditional-games"),
        BrowseFilterOption("Vampires", "vampires"),
        BrowseFilterOption("Video Games", "video-games"),
        BrowseFilterOption("Villainess", "villainess"),
        BrowseFilterOption("Virtual Reality", "virtual-reality"),
        BrowseFilterOption("Zombies", "zombies"),
    )

    internal val fallbackStatus = listOf(BrowseFilterOption("Ongoing", "Ongoing"), BrowseFilterOption("Completed", "Completed"))

    internal val fallbackTypes = listOf(BrowseFilterOption("Manga", "Manga"), BrowseFilterOption("Manhwa", "Manhwa"), BrowseFilterOption("Manhua", "Manhua"))

    internal val fallbackFormats = listOf(BrowseFilterOption("Hitam Putih", "bw"), BrowseFilterOption("Berwarna", "colorized"))

    internal val fallbackOrder = listOf(
        BrowseFilterOption("A-Z", "title"),
        BrowseFilterOption("Z-A", "titlereverse"),
        BrowseFilterOption("Latest Update", "update"),
        BrowseFilterOption("Latest Added", "latest"),
        BrowseFilterOption("Popular", "popular"),
    )

    fun absUrl(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            java.net.URI(BASE).resolve(raw).toString()
        } catch (_: Exception) {
            raw
        }
    }

    fun internalComicUrlFromHref(href: String): String {
        val slug = href.trim().trimEnd('/').split("/").lastOrNull().orEmpty()
        return if (slug.isBlank()) "" else "/komikindo/comic/$slug"
    }

    fun parseAnimePost(item: Element): ComicListItem? {
        val titleRaw = item.selectFirst(".tt h3 a")?.text()?.trim()
            ?: item.selectFirst(".animepostxx-top a")?.attr("title")?.trim()
            ?: return null

        val title = titleRaw.replace(Regex("^Komik\\s+", RegexOption.IGNORE_CASE), "").trim()
        val rawUrl = item.selectFirst("a")?.attr("href").orEmpty()
        val url = internalComicUrlFromHref(rawUrl)
        val thumb = item.selectFirst(".limit img, .animepostxx-top img")?.attr("src")?.takeIf { it.isNotBlank() }?.let { absUrl(it) }
        val type = item.selectFirst(".typeflag")?.classNames()?.firstOrNull { it != "typeflag" } ?: "Manga"

        val comicSlug = rawUrl.trim().trimEnd('/').split("/").lastOrNull().orEmpty()

        if (title.isBlank() || url.isBlank()) return null
        return ComicListItem(
            title = title,
            url = url,
            cover = thumb,
            type = type,
            latestChapter = animePostLatestChapter(item, comicSlug),
        )
    }

    private fun animePostLatestChapter(item: Element, comicSlug: String): HomeLatestChapter? {
        val latestLink = item.selectFirst(".lsch a, .list-ch-skroep a")
        val latestTitle = latestLink?.text()?.trim()
        val latestHref = latestLink?.attr("href")?.takeIf { it.isNotBlank() }
        val latestInternal = if (!latestHref.isNullOrBlank() && comicSlug.isNotBlank()) {
            KomikindoChapterParser.internalReadUrlFromChapterHref(comicSlug, latestHref)
        } else {
            null
        }
        return if (!latestTitle.isNullOrBlank() || !latestInternal.isNullOrBlank()) {
            HomeLatestChapter(title = latestTitle, url = latestInternal)
        } else {
            null
        }
    }

    fun parseBrowseComics(doc: Document): List<ComicListItem> = doc.select(".animposx").mapNotNull { parseAnimePost(it) }

    @Suppress("kotlin:S6619")
    fun parseFilterInputs(doc: Document, inputName: String): List<BrowseFilterOption> {
        val inputs = doc.select("input").filter { el ->
            val n = el.attr("name").trim()
            n == inputName || n == "$inputName[]"
        }
        return inputs.mapNotNull { input ->
            val value = input.attr("value").trim()
            if (value.isBlank()) return@mapNotNull null

            val id = input.attr("id").trim()
            val labelFromFor = if (id.isNotBlank()) doc.selectFirst("label[for='$id']")?.text()?.trim() else null
            val labelWrap = input.parent()?.takeIf { it.tagName().equals("label", ignoreCase = true) }?.text()?.trim()
            val labelSibling = input.parent()?.selectFirst("label")?.text()?.trim()
            val label = (labelFromFor ?: labelWrap ?: labelSibling).orEmpty()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (label.isBlank()) return@mapNotNull null
            BrowseFilterOption(label = label, value = value)
        }.distinctBy { it.value }
    }

    fun parseFilterSelect(doc: Document, name: String): List<BrowseFilterOption> {
        val select = doc.selectFirst("select[name='$name']") ?: return emptyList()
        return select.select("option").mapNotNull { opt ->
            val value = opt.attr("value").trim()
            val label = opt.text().trim()
            if (value.isBlank() || label.isBlank()) null else BrowseFilterOption(label = label, value = value)
        }
    }

    fun parseFilterOptions(doc: Document): BrowseFilterOptions {
        // Komikindo uses a mix of custom dropdowns + checkboxes. Prefer a DOM-structure-agnostic scan
        // for checkbox/radio inputs by name, and resolve their labels via <label for> or wrapping label.
        val genres = parseFilterInputs(doc, "genre")
        val demografis = parseFilterInputs(doc, "demografis")
        val konten = parseFilterInputs(doc, "konten")
        val tema = parseFilterInputs(doc, "tema")
        val status = parseFilterSelect(doc, "status")
        val type = parseFilterSelect(doc, "type")
        val format = parseFilterSelect(doc, "format")
        val orderBy = parseFilterSelect(doc, "order")

        return BrowseFilterOptions(
            genres = genres.orFallback(fallbackGenres),
            demografis = demografis.orFallback(fallbackDemografis),
            konten = konten.orFallback(fallbackKonten),
            tema = tema.orFallback(fallbackTema),
            status = status.orFallback(fallbackStatus),
            type = type.orFallback(fallbackTypes),
            format = format.orFallback(fallbackFormats),
            orderBy = orderBy.orFallback(fallbackOrder),
        )
    }

    private fun List<BrowseFilterOption>.orFallback(fallback: List<BrowseFilterOption>): List<BrowseFilterOption> = if (isNotEmpty()) this else fallback

    fun parseInfoMap(doc: Document): Map<String, List<String>> {
        val infoMap = mutableMapOf<String, List<String>>()
        doc.select(".infox .spe span").forEach { span ->
            val labelEl = span.selectFirst("b") ?: return@forEach
            val label = labelEl.text().trim().removeSuffix(":").trim()
            if (label.isBlank()) return@forEach
            val links = span.select("a").map { it.text().trim() }.filter { it.isNotBlank() }
            val text = span.clone().apply { select("b").remove() }.text().trim()
            val values = if (links.isNotEmpty()) links else text.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (values.isNotEmpty()) infoMap[label.lowercase()] = values
        }
        return infoMap
    }

    fun infoFirst(infoMap: Map<String, List<String>>, vararg keys: String): String? {
        for (k in keys) {
            val v = infoMap[k.lowercase()]?.firstOrNull()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    fun infoList(infoMap: Map<String, List<String>>, vararg keys: String): List<String> {
        for (k in keys) {
            val v = infoMap[k.lowercase()]
            if (!v.isNullOrEmpty()) return v
        }
        return emptyList()
    }

    fun parseExternalLinks(doc: Document, labelLower: String): List<ExternalLink> {
        val span = doc.selectFirst(".infox .spe span:has(b:matchesOwn((?i)^\\s*${Regex.escape(labelLower)}\\s*:?\\s*$))")
        // The selector above is picky; fallback to scan by comparing <b> text.
        val target = span ?: doc.select(".infox .spe span").firstOrNull { s ->
            val b = s.selectFirst("b")?.text()?.trim()?.removeSuffix(":")?.trim()?.lowercase()
            b == labelLower
        }
        if (target == null) return emptyList()
        return target.select(".person").mapNotNull { person ->
            val a = person.selectFirst(".data .name a") ?: return@mapNotNull null
            val name = a.text().trim()
            val href = a.attr("href").trim()
            if (name.isBlank() || href.isBlank()) return@mapNotNull null
            ExternalLink(name = name, url = href)
        }
    }

    fun parseRecommendations(doc: Document): List<HomeComicItem> {
        return doc.select(".relatedpost .serieslist li").mapNotNull { li ->
            val a = li.selectFirst(".leftseries h3 a") ?: li.selectFirst(".leftseries h4 a")
            val recTitle = a?.text()?.trim().orEmpty()
            val recHref = a?.attr("href")?.trim().orEmpty()
            if (recTitle.isBlank() || recHref.isBlank()) return@mapNotNull null
            val recThumb = li.selectFirst(".imgseries img")?.attr("src")?.trim().orEmpty()
                .takeIf { it.isNotBlank() }
                ?.let { absUrl(it) }
            HomeComicItem(
                title = recTitle,
                url = internalComicUrlFromHref(recHref),
                cover = recThumb,
                type = null,
                latestChapter = null,
            )
        }
    }
}
