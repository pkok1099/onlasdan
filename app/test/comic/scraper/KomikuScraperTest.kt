package comic.scraper

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class KomikuScraperTest {

    @Test
    fun testKomikuLs4ArticleStructure() {
        val html = """
            <section id="Rekomendasi_Komik" class="ls">
                <div class="ls4w rank-panel" id="rank-harian">
                    <article class="ls4">
                        <div class="ls4v">
                            <a href="/manga/ao-ashi/">
                                <img class="lazy" src="/asset/img/lazy.jpg" data-src="https://thumbnail.komiku.to/uploads/ao-ashi.jpg" alt="Baca Ao Ashi">
                            </a>
                            <span class="rank-num">1</span>
                        </div>
                        <div class="ls4j">
                            <h4><a href="/manga/ao-ashi/" title="Baca Komik Ao Ashi">Ao Ashi</a></h4>
                            <span class="ls4s">Drama · 47,735 views</span><br>
                            <a href="/ao-ashi-chapter-410/" class="ls24" title="Komik Ao Ashi Chapter 410">Chapter 410</a>
                        </div>
                    </article>
                </div>
            </section>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val articles = doc.select("#Rekomendasi_Komik article.ls4, #Rekomendasi_Komik article.ls2, #Rekomendasi_Komik article")
        assertEquals(1, articles.size)

        val article = articles.first()!!
        val titleEl = article.selectFirst(".ls4j h4 a, .ls4j h3 a, .ls4j a")
        assertEquals("Ao Ashi", titleEl?.text()?.trim())
        assertEquals("/manga/ao-ashi/", titleEl?.attr("href")?.trim())

        val chapterEl = article.selectFirst(".ls4j a.ls24, .ls4j a.ls2l") ?: article.select(".ls4j a").drop(1).firstOrNull()
        assertEquals("Chapter 410", chapterEl?.text()?.trim())

        val metaText = article.selectFirst(".ls4j .ls4s")?.text()?.trim().orEmpty()
        val type = metaText.split(Regex("[·•\\s]+")).firstOrNull().orEmpty()
        assertEquals("Drama", type)

        val imgEl = article.selectFirst(".ls4v img")
        val dataSrc = imgEl?.attr("data-src")
        assertEquals("https://thumbnail.komiku.to/uploads/ao-ashi.jpg", dataSrc)
    }

    @Test
    fun testKomikuLs2ArticleStructure() {
        val html = """
            <section id="Terbaru">
                <div class="ls2-wrap">
                    <article class="ls2">
                        <div class="ls2v">
                            <a href="/manga/mokushiroku-no-yonkishi/">
                                <img class="lazy" src="https://komiku.org/asset/img/lazy.jpg" data-src="https://thumbnail.komiku.to/uploads/mokushiroku.jpg" alt="Baca Mokushiroku">
                            </a>
                        </div>
                        <div class="ls2j">
                            <h3><a href="/manga/mokushiroku-no-yonkishi/">Mokushiroku no Yonkishi</a></h3>
                            <span class="ls2t">Aksi · 36 detik lalu</span>
                            <a href="/mokushiroku-no-yonkishi-chapter-258/" class="ls2l">Chapter 258</a>
                        </div>
                    </article>
                </div>
            </section>
        """.trimIndent()

        val doc = Jsoup.parse(html)
        val articles = doc.select("#Terbaru article.ls2, #Terbaru article.ls4, #Terbaru article")
        assertEquals(1, articles.size)

        val article = articles.first()!!
        val titleEl = article.selectFirst(".ls2j h3 a, .ls2j h4 a, .ls2j a")
        assertEquals("Mokushiroku no Yonkishi", titleEl?.text()?.trim())

        val typeWithStats = article.selectFirst(".ls2j .ls2t")?.text()?.trim().orEmpty()
        val type = typeWithStats.split(Regex("[·•\\s]+")).firstOrNull().orEmpty()
        assertEquals("Aksi", type)

        val imgEl = article.selectFirst(".ls2v img")
        val dataSrc = imgEl?.attr("data-src")
        assertEquals("https://thumbnail.komiku.to/uploads/mokushiroku.jpg", dataSrc)
    }
}
