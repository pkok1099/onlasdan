package com.onlasdan.comic.scraper

import org.jsoup.Jsoup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScraperTest {

    @Test
    fun testKomikindoHomeScraping() {
        val html = """
            <div class="mangapopuler">
                <div class="animepost">
                    <div class="tt"><h3><a href="https://komikindo.ch/komik/one-piece/">One Piece</a></h3></div>
                    <div class="limit"><img src="https://example.com/cover.jpg"></div>
                    <div class="lsch"><a href="https://komikindo.ch/one-piece-chapter-1100/">Chapter 1100</a></div>
                </div>
            </div>
            <div class="chapterbaru">
                <div class="animepost">
                    <div class="animepostxx-top">
                        <a href="https://komikindo.ch/komik/naruto/" title="Komik Naruto">Naruto</a>
                        <img src="https://example.com/naruto.jpg">
                    </div>
                    <div class="list-ch-skroep">
                        <a href="https://komikindo.ch/naruto-chapter-700/">Chapter 700</a>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val popular = doc.select(".mangapopuler .animepost")
        val latest = doc.select(".chapterbaru .animepost")

        assertFalse("Popular comics should not be empty", popular.isEmpty())
        assertFalse("Latest comics should not be empty", latest.isEmpty())

        assertTrue(popular.first()?.select(".tt h3 a")?.text()?.contains("One Piece") == true)
        assertTrue(latest.first()?.select(".animepostxx-top a")?.attr("title")?.contains("Naruto") == true)
    }

    @Test
    fun testKomikuHomeScraping() {
        val html = """
            <div class="bge">
                <div class="kan"><h3>Solo Leveling</h3><a href="https://komiku.org/manga/solo-leveling/">Link</a></div>
                <div class="bgei"><img src="https://example.com/solo.jpg"></div>
                <div class="new1"><a href="#"><span>Ch. 200</span></a></div>
            </div>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val items = doc.select(".bge")

        assertFalse("Komiku items should not be empty", items.isEmpty())
        assertTrue(items.first()?.select(".kan h3")?.text()?.contains("Solo Leveling") == true)
    }
}
