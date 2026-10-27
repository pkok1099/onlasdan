package comic.scraper

import com.google.gson.JsonParser
import java.net.URLEncoder
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoujindesuScraperTest {

    private val scraper = DoujindesuScraper()

    private fun obj(json: String) = JsonParser.parseString(json).asJsonObject

    private fun attachUrlInterceptor(
        scraperInstance: DoujindesuScraper,
        interceptedUrls: MutableList<String>,
        statusCode: Int = 200,
        responseBody: String = "[]",
    ) {
        val field = DoujindesuScraper::class.java.getDeclaredField("client")
        field.isAccessible = true
        val originalClient = field.get(scraperInstance) as OkHttpClient
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            interceptedUrls.add(request.url.toString())
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message(if (statusCode == 200) "OK" else "Error")
                .body(responseBody.toResponseBody("application/json".toMediaType()))
                .build()
        }
        val interceptedClient = originalClient.newBuilder().addInterceptor(interceptor).build()
        field.set(scraperInstance, interceptedClient)
    }

    @Test
    fun `browse constructs expected URL query parameters for sort type status genre and search`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedScraper = DoujindesuScraper()
        attachUrlInterceptor(
            scraperInstance = interceptedScraper,
            interceptedUrls = urls,
            responseBody = "[$SAMPLE_ITEM_JSON]",
        )

        val result = interceptedScraper.browse(
            page = 2,
            orderby = "popular",
            tipe = "manhwa",
            status = "publishing",
            genre = "anal",
            sorttime = null,
            search = "test query",
        )

        assertNotNull(result.data)
        assertEquals(1, result.data?.comics?.size)
        assertEquals("Shussan Tekireki Hitozuma Netorare", result.data?.comics?.first()?.title)

        assertEquals(1, urls.size)
        val url = urls[0]
        assertTrue("Expected limit=20 in URL: $url", url.contains("limit=20"))
        assertTrue("Expected offset=20 in URL: $url", url.contains("offset=20"))
        assertTrue("Expected sort=popular in URL: $url", url.contains("sort=popular"))
        assertTrue("Expected type=manhwa in URL: $url", url.contains("type=manhwa"))
        assertTrue("Expected status=publishing in URL: $url", url.contains("status=publishing"))
        assertTrue("Expected genre=anal in URL: $url", url.contains("genre=anal"))
        assertTrue("Expected search=test+query in URL: $url", url.contains("search=test+query"))
    }

    @Test
    fun `browse returns ApiResponse with error when HTTP fails`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedScraper = DoujindesuScraper()
        attachUrlInterceptor(
            scraperInstance = interceptedScraper,
            interceptedUrls = urls,
            statusCode = 500,
            responseBody = "Internal Server Error",
        )

        val result = interceptedScraper.browse(page = 1, orderby = null, tipe = null, status = null, genre = null, sorttime = null)
        assertNull(result.data)
        assertNotNull(result.error)
        assertTrue("Expected error to mention HTTP 500", result.error?.contains("500") == true)
    }

    @Test
    fun `fmtChapterNumber formats integers without decimals and retains floating decimals`() {
        assertEquals("12", scraper.fmtChapterNumber(12.0))
        assertEquals("12.5", scraper.fmtChapterNumber(12.5))
        assertEquals("0", scraper.fmtChapterNumber(0.0))
    }

    @Test
    fun `deriveKey matches expected JS test vector`() {
        val expectedCodes = intArrayOf(
            44, 118, 87, 37, 82, 88, 33, 60, 121, 41, 68, 76, 46, 88, 124, 100,
            98, 101, 119, 47, 119, 116, 97, 55, 73, 64, 44, 86, 95, 65, 33, 92,
        )
        val expectedKey = expectedCodes.map { it.toChar() }.joinToString("")
        val derived = scraper.deriveKey(497548L)
        assertEquals(expectedKey, derived)
    }

    @Test
    fun `decrypt recovers roundtrip encrypted payload`() {
        val slot = System.currentTimeMillis() / 3600000L
        val key = scraper.deriveKey(slot)
        val plain = """{"title":"Special Delivery","slug":"special-delivery"}"""

        // Encrypt according to reverse of xorStream
        val raw = URLEncoder.encode(plain, "UTF-8").toByteArray(Charsets.ISO_8859_1)
        val out = StringBuilder()
        var l = 42
        val kl = key.length
        for (x in raw.indices) {
            val y = raw[x].toInt() and 0xFF
            val c = (y xor key[x % kl].code xor (x * 13) xor l) and 0xFF
            out.append(String.format("%02x", c))
            l = (l + c) % 256
        }
        val cipherHex = out.toString()

        val decrypted = scraper.decrypt(cipherHex)
        assertEquals(plain, decrypted)
    }

    @Test
    fun `parseMangaListItem maps to ComicListItem`() {
        val item = scraper.parseMangaListItem(obj(SAMPLE_ITEM_JSON))
        assertEquals("Shussan Tekireki Hitozuma Netorare", item.title)
        assertEquals("/doujindesu/manga/shussan-tekireki-hitozuma-netorare", item.url)
        assertEquals("https://pic.desu.xxx/content/manga/cover.webp", item.cover)
        assertEquals("doujinshi", item.type)
    }

    @Test
    fun `parseMangaDetail maps fields and extracts genres`() {
        val detail = scraper.parseMangaDetail(obj(SAMPLE_DETAIL_JSON))
        assertEquals("Pure Perverted Love", detail.title)
        assertEquals("Sexy Beam", detail.author)
        assertEquals("Byeon Taejung", detail.illustrator)
        assertEquals(listOf("Sleeping", "Netorare"), detail.genres)
        assertEquals("Manhwa", detail.type)
        assertEquals("Ongoing", detail.status)
        assertEquals("8.8", detail.rating)
        assertTrue(detail.synopsis!!.contains("Sinopsis:\nBerhenti"))
        assertEquals(listOf("Alt 1", "Alt 2"), detail.alternativeTitles)
        assertEquals("doujindesu", detail.source)
    }

    @Test
    fun `cleanHtml handles entities and linebreaks`() {
        val html = "&lt;p&gt;&lt;strong&gt;Sinopsis:&lt;/strong&gt;&lt;br /&gt;Teks.&lt;/p&gt;"
        val cleaned = scraper.cleanHtml(html)
        assertEquals("Sinopsis:\nTeks.", cleaned)

        assertNull(scraper.cleanHtml("   "))
        assertNull(scraper.cleanHtml(null))
    }

    @Test
    fun `parseChapterListItem formats number and builds url`() {
        val ch = scraper.parseChapterListItem(obj(SAMPLE_CHAPTER_JSON), "test-manga")
        assertEquals("f487b8bb-21cc-46c5-8a88-296a1dc1a41d", ch.id)
        assertEquals("26", ch.number)
        assertEquals("Chapter 26", ch.title)
        assertEquals("/doujindesu/read/test-manga/f487b8bb-21cc-46c5-8a88-296a1dc1a41d", ch.url)
    }

    companion object {
        private const val SAMPLE_ITEM_JSON = """{
            "id": "1916371a-462e-4e10-87c9-7f9053c3651f",
            "title": "Shussan Tekireki Hitozuma Netorare",
            "slug": "shussan-tekireki-hitozuma-netorare",
            "cover_url": "https://pic.desu.xxx/content/manga/cover.webp",
            "status": "completed",
            "type": "doujinshi"
        }"""

        private const val SAMPLE_DETAIL_JSON = """{
            "id": "5d62fd64-06e6-4a63-812f-589ceaa3d8b8",
            "title": "Pure Perverted Love",
            "slug": "pure-perverted-love",
            "description": "&lt;p&gt;&lt;strong&gt;Sinopsis:&lt;/strong&gt;&lt;br /&gt;Berhenti...&lt;/p&gt;",
            "cover_url": "https://cdn-static.desu.xxx/uploads/9926w.jpg",
            "author": "Sexy Beam",
            "artist": "Byeon Taejung",
            "status": "publishing",
            "type": "manhwa",
            "rating": 8.8,
            "alt_titles": "Alt 1, Alt 2",
            "manga_genres": [
                {"genres": {"name": "Sleeping"}},
                {"genres": {"name": "Netorare"}}
            ]
        }"""

        private const val SAMPLE_CHAPTER_JSON = """{
            "id": "f487b8bb-21cc-46c5-8a88-296a1dc1a41d",
            "chapter_number": 26,
            "title": "Chapter 26",
            "created_at": "2026-10-18T01:51:04.563Z"
        }"""
    }
}
