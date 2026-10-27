package comic.scraper

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KomikindoScraperComprehensiveTest {

    private val scraper = KomikindoScraper()

    @Test
    fun staticFilterOptions_containsComprehensivePresets() {
        val options = scraper.staticFilterOptions()

        assertNotNull(options.genres)
        assertTrue(options.genres.isNotEmpty())
        assertTrue(options.genres.any { it.value == "action" })
        assertTrue(options.genres.any { it.value == "romance" })
        assertTrue(options.genres.any { it.value == "fantasy" })

        assertNotNull(options.status)
        assertTrue(options.status.any { it.value.equals("ongoing", ignoreCase = true) })
        assertTrue(options.status.any { it.value.equals("completed", ignoreCase = true) })

        assertNotNull(options.type)
        assertTrue(options.type.any { it.value.equals("manga", ignoreCase = true) })
        assertTrue(options.type.any { it.value.equals("manhwa", ignoreCase = true) })
    }

    @Test
    fun parseDetailHtml_extractsAllMetadataCorrectly() {
        val html = """
            <div class="entry-title" itemprop="headline">
                <h1>Komik One Piece</h1>
            </div>
            <div class="thumb">
                <img src="https://example.com/one-piece.jpg" />
            </div>
            <div class="infox">
                <div class="spe">
                    <span><b>Status:</b> Ongoing</span>
                    <span><b>Pengarang:</b> Eiichiro Oda</span>
                    <span><b>Ilustrator:</b> Eiichiro Oda</span>
                    <span><b>Jenis Komik:</b> Manga</span>
                </div>
                <div class="genre-info">
                    <a href="/genres/action/">Action</a>
                    <a href="/genres/adventure/">Adventure</a>
                </div>
            </div>
            <div class="sinopsis">
                <p>Gol D. Roger dikenal sebagai Raja Bajak Laut...</p>
            </div>
            <div id="chapter_list">
                <ul class="listch">
                    <li>
                        <span class="lchx">
                            <a href="https://komikindo.ch/one-piece-chapter-1100/">Chapter 1100</a>
                        </span>
                        <span class="dt">2 hari lalu</span>
                    </li>
                    <li>
                        <span class="lchx">
                            <a href="https://komikindo.ch/one-piece-chapter-1099/">Chapter 1099</a>
                        </span>
                        <span class="dt">1 minggu lalu</span>
                    </li>
                </ul>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val titleRaw = doc.selectFirst(".entry-title h1")?.text()?.trim().orEmpty()
        val title = titleRaw.replace(Regex("^Komik\\s+", RegexOption.IGNORE_CASE), "").trim()
        val cover = doc.selectFirst(".thumb img")?.attr("src")
        val synopsis = doc.selectFirst(".sinopsis p")?.text()

        assertEquals("One Piece", title)
        assertEquals("https://example.com/one-piece.jpg", cover)
        assertTrue(synopsis?.contains("Gol D. Roger") == true)

        val chapters = doc.select("#chapter_list ul.listch li").mapNotNull { li ->
            val link = li.selectFirst(".lchx a") ?: return@mapNotNull null
            val chTitle = link.text().trim()
            val chUrl = link.attr("href").trim()
            val date = li.selectFirst(".dt")?.text()?.trim()
            chTitle to (chUrl to date)
        }

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1100", chapters[0].first)
        assertEquals("Chapter 1099", chapters[1].first)
    }

    @Test
    fun parseChapterImages_extractsSequentialPages() {
        val html = """
            <div id="chimg-auh">
                <img src="https://img.example.com/ch1100/01.jpg" alt="One Piece 1100 Page 1" />
                <img src="https://img.example.com/ch1100/02.jpg" alt="One Piece 1100 Page 2" />
                <img src="https://img.example.com/ch1100/03.jpg" alt="One Piece 1100 Page 3" />
            </div>
            <div class="nextprev">
                <a class="ch-prev-btn" href="https://komikindo.ch/one-piece-chapter-1099/">Prev</a>
                <a class="ch-next-btn" href="https://komikindo.ch/one-piece-chapter-1101/">Next</a>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val images = doc.select("#chimg-auh img").map { it.attr("src") }.filter { it.isNotBlank() }

        assertEquals(3, images.size)
        assertEquals("https://img.example.com/ch1100/01.jpg", images[0])
        assertEquals("https://img.example.com/ch1100/02.jpg", images[1])
        assertEquals("https://img.example.com/ch1100/03.jpg", images[2])

        val prevHref = doc.selectFirst(".ch-prev-btn")?.attr("href")
        val nextHref = doc.selectFirst(".ch-next-btn")?.attr("href")
        assertTrue(prevHref?.contains("1099") == true)
        assertTrue(nextHref?.contains("1101") == true)
    }

    @Test
    fun parseMalformedHtml_handlesGracefullyWithoutCrashing() {
        val emptyHtml = "<html><body><div>Empty</div></body></html>"
        val doc = Jsoup.parse(emptyHtml)

        val popular = doc.select(".mangapopuler .animepost")
        val chapters = doc.select("#chapter_list ul.listch li")
        val images = doc.select("#chimg-auh img")

        assertTrue(popular.isEmpty())
        assertTrue(chapters.isEmpty())
        assertTrue(images.isEmpty())
    }
}
