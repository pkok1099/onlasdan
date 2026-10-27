package comic.scraper

import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Mapping tests for ShinigamiApi using real response shapes captured
 * in docs/superpowers/specs/shinigami-api.md (recon 2026-10-06).
 * Includes URL parameter construction tests via OkHttp client interception.
 */
class ShinigamiApiTest {

    private val api = ShinigamiApi()

    private fun obj(json: String) = JsonParser.parseString(json).asJsonObject

    private fun attachUrlInterceptor(apiInstance: ShinigamiApi, interceptedUrls: MutableList<String>) {
        val field = ShinigamiApi::class.java.getDeclaredField("client")
        field.isAccessible = true
        val originalClient = field.get(apiInstance) as OkHttpClient
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            interceptedUrls.add(request.url.toString())
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"retcode":0,"data":[],"meta":{"total_page":5}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }
        val interceptedClient = originalClient.newBuilder().addInterceptor(interceptor).build()
        field.set(apiInstance, interceptedClient)
    }

    @Test
    fun `browse constructs expected URL query parameters for sort format status and genre_include`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedApi = ShinigamiApi()
        attachUrlInterceptor(interceptedApi, urls)

        val result = interceptedApi.browse(
            page = 2,
            sort = "rating",
            sortOrder = "asc",
            status = "completed",
            format = "manhwa",
            genreInclude = "action",
            search = "leveling",
        )

        assertNotNull(result.data)
        assertEquals(1, urls.size)
        val url = urls[0]

        assertTrue("Expected page=2 in URL: $url", url.contains("page=2"))
        assertTrue("Expected page_size=20 in URL: $url", url.contains("page_size=20"))
        assertTrue("Expected sort=rating in URL: $url", url.contains("sort=rating"))
        assertTrue("Expected sort_order=asc in URL: $url", url.contains("sort_order=asc"))
        assertTrue("Expected status=completed in URL: $url", url.contains("status=completed"))
        assertTrue("Expected format=manhwa in URL: $url", url.contains("format=manhwa"))
        assertTrue("Expected genre_include=action in URL: $url", url.contains("genre_include=action"))
        assertTrue("Expected q=leveling in URL: $url", url.contains("q=leveling"))
    }

    @Test
    fun `browse maps popular sort to popularity`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedApi = ShinigamiApi()
        attachUrlInterceptor(interceptedApi, urls)

        interceptedApi.browse(
            page = 1,
            sort = "popular",
        )

        assertEquals(1, urls.size)
        val url = urls[0]
        assertTrue("Expected sort=popularity in URL: $url", url.contains("sort=popularity"))
    }

    @Test
    fun `getRecommended constructs expected URL query parameters`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedApi = ShinigamiApi()
        attachUrlInterceptor(interceptedApi, urls)

        interceptedApi.getRecommended(page = 1, pageSize = 12, format = "manhwa")

        assertEquals(1, urls.size)
        val url = urls[0]
        assertTrue("Expected is_recommended=true in URL: $url", url.contains("is_recommended=true"))
        assertTrue("Expected format=manhwa in URL: $url", url.contains("format=manhwa"))
        assertTrue("Expected sort=latest in URL: $url", url.contains("sort=latest"))
        assertTrue("Expected sort_order=desc in URL: $url", url.contains("sort_order=desc"))
    }

    @Test
    fun `getUpdates constructs expected URL query parameters with type and pagination`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedApi = ShinigamiApi()
        attachUrlInterceptor(interceptedApi, urls)

        interceptedApi.getUpdates(page = 2, pageSize = 24, type = "mirror")

        assertEquals(1, urls.size)
        val url = urls[0]
        assertTrue("Expected page=2 in URL: $url", url.contains("page=2"))
        assertTrue("Expected page_size=24 in URL: $url", url.contains("page_size=24"))
        assertTrue("Expected type=mirror in URL: $url", url.contains("type=mirror"))
        assertTrue("Expected sort=latest in URL: $url", url.contains("sort=latest"))
        assertTrue("Expected sort_order=desc in URL: $url", url.contains("sort_order=desc"))
    }

    @Test
    fun `browse separates format and type query parameters`() = runTest {
        val urls = mutableListOf<String>()
        val interceptedApi = ShinigamiApi()
        attachUrlInterceptor(interceptedApi, urls)

        interceptedApi.browse(
            page = 1,
            format = "manhwa",
            type = "project",
        )

        assertEquals(1, urls.size)
        val url = urls[0]
        assertTrue("Expected format=manhwa in URL: $url", url.contains("format=manhwa"))
        assertTrue("Expected type=project in URL: $url", url.contains("type=project"))
    }

    @Test
    fun `manga list item maps to ComicListItem`() {
        val item = api.parseMangaListItem(obj(LIST_ITEM_JSON))
        assertEquals("Magic Emperor", item.title)
        assertEquals("/shinigami/comic/8c4fdbee-ffb8-427d-ace7-f21fe6b72f77", item.url)
        assertEquals(
            "https://assets.shngm.id/thumbnail/image/8c4fdbee-ffb8-427d-ace7-f21fe6b72f77.jpg",
            item.cover,
        )
        assertEquals("Manhua", item.type)
    }

    @Test
    fun `manga detail maps taxonomy to authors artists genres`() {
        val detail = api.parseMangaDetail(obj(DETAIL_JSON))
        assertEquals("Magic Emperor", detail.title)
        assertEquals("Zhang Xing", detail.author)
        assertEquals("Li Wei", detail.illustrator)
        assertEquals(listOf("Action", "Fantasy"), detail.genres)
        assertEquals("Manhua", detail.type)
        assertEquals("Ongoing", detail.status)
        assertEquals("Seorang kaisar sihir yang bereinkarnasi.", detail.synopsis)
        assertEquals(listOf("Magic Emperor, 魔皇大管家"), detail.alternativeTitles)
    }

    @Test
    fun `chapter detail builds ordered image urls`() {
        val images = api.parseChapterImages(obj(CHAPTER_DETAIL_JSON))
        assertEquals(3, images.size)
        assertEquals(
            "https://assets.shngm.id/chapter/manga_aaa/chapter_bbb/1-deadbeef.jpg",
            images[0],
        )
        assertEquals(
            "https://assets.shngm.id/chapter/manga_aaa/chapter_bbb/3-deadbeef.jpg",
            images[2],
        )
    }

    @Test
    fun `chapter list maps items latest-first with formatted numbers and internal urls`() {
        val items = api.parseChapterList(obj(CHAPTER_LIST_JSON).getAsJsonArray("data"), "manga-aaa")
        assertEquals(2, items.size)
        assertEquals("12.5", items[0].number)
        assertEquals("Judul Spesial", items[0].title)
        assertEquals("1", items[1].number)
        assertEquals("Chapter 1", items[1].title) // blank chapter_title falls back
        assertEquals("/shinigami/read/manga-aaa/ch-bbb", items[1].url)
    }

    @Test
    fun `nonzero retcode throws with api message`() {
        try {
            api.checkEnvelope(obj("""{"retcode":400001,"message":"Bad Request"}"""))
            fail("expected RuntimeException")
        } catch (e: RuntimeException) {
            assertTrue(e.message!!.contains("Bad Request"))
        }
    }

    companion object {
        // Shapes from spec §2.1 (real field names, trimmed).
        private const val LIST_ITEM_JSON = """{
            "manga_id": "8c4fdbee-ffb8-427d-ace7-f21fe6b72f77",
            "title": "Magic Emperor",
            "alternative_title": "Magic Emperor, 魔皇大管家",
            "cover_image_url": "https://assets.shngm.id/thumbnail/image/8c4fdbee-ffb8-427d-ace7-f21fe6b72f77.jpg",
            "bookmark_count": 50974,
            "country_id": "CN"
        }"""

        // Shapes from spec §2.2.
        private const val DETAIL_JSON = """{
            "manga_id": "8c4fdbee-ffb8-427d-ace7-f21fe6b72f77",
            "title": "Magic Emperor",
            "alternative_title": "Magic Emperor, 魔皇大管家",
            "description": "Seorang kaisar sihir yang bereinkarnasi.",
            "status": 1,
            "country_id": "CN",
            "cover_image_url": "https://assets.shngm.id/thumbnail/image/8c4fdbee-ffb8-427d-ace7-f21fe6b72f77.jpg",
            "bookmark_count": 50974,
            "view_count": 77444514,
            "user_rate": 4.5,
            "taxonomy": {
                "Author": [{"taxonomy_id": 1, "slug": "zhang-xing", "name": "Zhang Xing"}],
                "Artist": [{"taxonomy_id": 2, "slug": "li-wei", "name": "Li Wei"}],
                "Genre": [
                    {"taxonomy_id": 25, "slug": "action", "name": "Action"},
                    {"taxonomy_id": 26, "slug": "fantasy", "name": "Fantasy"}
                ],
                "Format": [{"taxonomy_id": 9, "slug": "manhua", "name": "Manhua"}]
            }
        }"""

        // Shapes from spec §3.1.
        private const val CHAPTER_DETAIL_JSON = """{
            "chapter_id": "ch-bbb",
            "manga_id": "manga-aaa",
            "chapter_number": 1,
            "chapter_title": "",
            "base_url": "https://assets.shngm.id",
            "chapter": {
                "path": "/chapter/manga_aaa/chapter_bbb/",
                "data": ["1-deadbeef.jpg", "2-deadbeef.jpg", "3-deadbeef.jpg"]
            }
        }"""

        // Shapes from spec §2.3.
        private const val CHAPTER_LIST_JSON = """{"data": [
            {"chapter_id": "ch-bbb", "manga_id": "manga-aaa", "chapter_number": 1,
              "chapter_title": "", "thumbnail_image_url": "",
              "view_count": 100, "release_date": "2024-11-27T09:42:17Z"},
            {"chapter_id": "ch-ccc", "manga_id": "manga-aaa", "chapter_number": 12.5,
              "chapter_title": "Judul Spesial", "thumbnail_image_url": "",
              "view_count": 50, "release_date": "2024-12-01T09:42:17Z"}
        ]}"""
    }
}
