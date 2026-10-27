package comic.source

import comic.api.*
import comic.scraper.DoujindesuScraper
import comic.scraper.GenreMasterData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DoujindesuSourceTest {

    private val scraper: DoujindesuScraper = mock()
    private val source = DoujindesuSource(scraper)

    @Test
    fun sourceMetadata_idAndNameAreCorrect() {
        assertEquals("doujindesu", source.id)
        assertEquals("DoujinDesu", source.name)
        assertTrue(source.capabilities.contains(Capability.SEARCH))
        assertTrue(source.capabilities.contains(Capability.FILTER_GENRE))
        assertTrue(source.capabilities.contains(Capability.FILTER_TYPE))
        assertTrue(source.capabilities.contains(Capability.FILTER_STATUS))
        assertTrue(source.capabilities.contains(Capability.FILTER_SORT))
    }

    @Test
    fun availableFilters_containsGenreAndMasterGenres() {
        val filters = source.availableFilters()
        val filterKeys = filters.map { it.key }
        assertTrue(filterKeys.contains("sort"))
        assertTrue(filterKeys.contains("type"))
        assertTrue(filterKeys.contains("status"))
        assertTrue(filterKeys.contains("genre"))

        val genreFilter = filters.first { it.key == "genre" }
        assertEquals(GenreMasterData.doujindesuMasterGenres.size, genreFilter.options.size)
        assertEquals(GenreMasterData.doujindesuMasterGenres.first().id, genreFilter.options.first().id)
    }

    @Test
    fun home_nullData_throwsRuntimeExceptionWithApiErrorMessage() = runTest {
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = null, error = "Cloudflare 403"))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertTrue("Expected doujindesu prefix in error", e.message?.startsWith("doujindesu:") == true)
            assertTrue("Expected original error message", e.message?.contains("Cloudflare 403") == true)
        }
    }

    @Test
    fun home_nullDataAndNullError_throwsRuntimeExceptionWithDefaultMessage() = runTest {
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = null, error = null))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertEquals("doujindesu: respons kosong", e.message)
        }
    }

    @Test
    fun home_success_mapsHeroDoujinshiAndManhwaSections() = runTest {
        val popular = (1..10).map { i ->
            HomeComicItem(
                title = "Popular $i",
                url = "/doujindesu/manga/pop-$i",
                cover = "https://example.com/cover$i.jpg",
                rating = "9.$i",
            )
        }
        val latest = listOf(
            HomeComicItem(
                title = "Latest 1",
                url = "/doujindesu/manga/latest-1",
                type = "doujinshi",
            ),
        )
        val homeData = HomepageData(
            popularComics = popular,
            latestComics = latest,
            source = "doujindesu",
        )
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = homeData))

        val manhwaList = listOf(
            ComicListItem(
                title = "Manhwa 1",
                url = "/doujindesu/manga/manhwa-1",
                type = "manhwa",
            ),
        )
        val browseData = BrowseData(comics = manhwaList)
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = eq("latest_chapter"),
                tipe = eq("manhwa"),
                status = anyOrNull(),
                genre = anyOrNull(),
                sorttime = anyOrNull(),
                search = anyOrNull(),
            ),
        ).thenReturn(ApiResponse(data = browseData))

        val sections = source.home()
        assertEquals(3, sections.size)

        // Hero section takes max 8 items
        val heroSection = sections[0]
        assertEquals("hero", heroSection.id)
        assertEquals(SectionType.HERO, heroSection.type)
        assertEquals(8, heroSection.items.size)
        assertEquals("pop-1", heroSection.items[0].id)
        assertEquals("Popular 1", heroSection.items[0].title)

        // Doujinshi section
        val doujinSection = sections[1]
        assertEquals("doujinshi_manga", doujinSection.id)
        assertEquals(SectionType.GRID, doujinSection.type)
        assertEquals(mapOf("type" to "doujinshi"), doujinSection.browseFilters)

        // Manhwa section
        val manhwaSection = sections[2]
        assertEquals("manhwa", manhwaSection.id)
        assertEquals(SectionType.GRID, manhwaSection.type)
        assertEquals(mapOf("type" to "manhwa"), manhwaSection.browseFilters)
    }

    @Test
    fun search_mapsBrowseResponseToPagedComicCorrectly() = runTest {
        val comics = listOf(
            ComicListItem(
                title = "Sample Manga",
                url = "/doujindesu/manga/sample-manga",
                cover = "https://pic.desu.xxx/cover.webp",
                type = "manga",
                rating = 8.5,
                latestChapter = HomeLatestChapter(title = "Ch. 12", url = "ch-12"),
                lastUpdated = "2026-10-18T00:00:00Z",
            ),
        )
        val browseData = BrowseData(
            comics = comics,
            pagination = PaginationData(currentPage = 1, totalPages = 4),
        )
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = eq("popular"),
                tipe = eq("manga"),
                status = eq("publishing"),
                genre = eq("romance"),
                sorttime = anyOrNull(),
                search = eq("Sample"),
            ),
        ).thenReturn(ApiResponse(data = browseData))

        val filters = mapOf(
            "sort" to "popular",
            "type" to "manga",
            "status" to "publishing",
            "genre" to "romance",
        )
        val result = source.search("Sample", filters, page = 1)

        assertEquals(1, result.items.size)
        val comic = result.items[0]
        assertEquals("sample-manga", comic.id)
        assertEquals("Sample Manga", comic.title)
        assertEquals("https://pic.desu.xxx/cover.webp", comic.cover)
        assertEquals("manga", comic.type)
        assertEquals("8.5", comic.rating)
        assertEquals("Ch. 12", comic.latestChapter)
        assertEquals("2026-10-18T00:00:00Z", comic.lastUpdated)
        assertEquals("doujindesu", comic.sourceId)
        assertEquals("DoujinDesu", comic.sourceName)

        assertEquals(1, result.page)
        assertEquals(4, result.totalPages)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun search_blankQueryTreatedAsNull() = runTest {
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = anyOrNull(),
                tipe = anyOrNull(),
                status = anyOrNull(),
                genre = anyOrNull(),
                sorttime = anyOrNull(),
                search = eq(null),
            ),
        ).thenReturn(ApiResponse(data = BrowseData(comics = emptyList())))

        source.search("   ", emptyMap(), page = 1)

        verify(scraper).browse(
            page = eq(1),
            orderby = anyOrNull(),
            tipe = anyOrNull(),
            status = anyOrNull(),
            genre = anyOrNull(),
            sorttime = anyOrNull(),
            search = eq(null),
        )
    }

    @Test
    fun search_hasNextPageCalculatedProperlyWhenTotalPagesNull() = runTest {
        // When totalPages is null and comics count >= 20 -> hasNextPage = true
        val twentyComics = (1..20).map { ComicListItem(title = "Title $it", url = "/doujindesu/manga/t-$it") }
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = anyOrNull(),
                tipe = anyOrNull(),
                status = anyOrNull(),
                genre = anyOrNull(),
                sorttime = anyOrNull(),
                search = anyOrNull(),
            ),
        ).thenReturn(ApiResponse(data = BrowseData(comics = twentyComics, pagination = null)))

        val res20 = source.search("", emptyMap(), 1)
        assertTrue(res20.hasNextPage)

        // When totalPages is null and comics count < 20 -> hasNextPage = false
        val fiveComics = (1..5).map { ComicListItem(title = "Title $it", url = "/doujindesu/manga/t-$it") }
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = anyOrNull(),
                tipe = anyOrNull(),
                status = anyOrNull(),
                genre = anyOrNull(),
                sorttime = anyOrNull(),
                search = anyOrNull(),
            ),
        ).thenReturn(ApiResponse(data = BrowseData(comics = fiveComics, pagination = null)))

        val res5 = source.search("", emptyMap(), 1)
        assertFalse(res5.hasNextPage)
    }

    @Test
    fun detail_nullData_throwsIllegalStateException() = runTest {
        whenever(scraper.getComicDetail("slug-123")).thenReturn(ApiResponse(data = null, error = "Not found"))

        try {
            source.detail("slug-123")
            fail("Expected IllegalStateException on null detail data")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Not found") == true)
        }
    }

    @Test
    fun detail_success_mapsDetailFields() = runTest {
        val detailData = ComicDetail(
            id = "test-slug",
            title = "Test Comic",
            thumbnail = "https://example.com/cover.jpg",
            rating = "8.8",
            status = "Ongoing",
            type = "Manhwa",
            author = "Author A",
            illustrator = "Artist B",
            synopsis = "Synopsis here",
            genres = listOf("Sleeping", "Big Breast"),
            chapterList = listOf(
                ChapterItem(id = "ch-1", number = "1", title = "Chapter 1"),
            ),
            lastUpdated = "2026-10-18T00:00:00Z",
            groups = listOf("Group 1"),
            series = listOf("Series 1"),
            characters = listOf("Char 1"),
            views = "12345",
        )
        whenever(scraper.getComicDetail("test-slug")).thenReturn(ApiResponse(data = detailData))

        val comic = source.detail("test-slug")
        assertEquals("test-slug", comic.id)
        assertEquals("Test Comic", comic.title)
        assertEquals("https://example.com/cover.jpg", comic.cover)
        assertEquals("8.8", comic.rating)
        assertEquals("Ongoing", comic.status)
        assertEquals("Manhwa", comic.type)
        assertEquals("Author A", comic.author)
        assertEquals("Artist B", comic.illustrator)
        assertEquals("Synopsis here", comic.synopsis)
        assertEquals(2, comic.genres.size)
        assertEquals("sleeping", comic.genres[0].id)
        assertEquals("Sleeping", comic.genres[0].name)
        assertEquals("big-breast", comic.genres[1].id)
        assertEquals("Big Breast", comic.genres[1].name)
        assertEquals("1", comic.latestChapter)
        assertEquals("doujindesu", comic.sourceId)
        assertEquals("DoujinDesu", comic.sourceName)
        assertEquals(listOf("Group 1"), comic.groups)
        assertEquals(listOf("Series 1"), comic.series)
        assertEquals(listOf("Char 1"), comic.characters)
        assertEquals("12345", comic.views)
    }

    @Test
    fun chapters_nullData_returnsEmptyList() = runTest {
        whenever(scraper.getComicDetail("slug")).thenReturn(ApiResponse(data = null))

        val list = source.chapters("slug")
        assertTrue(list.isEmpty())
    }

    @Test
    fun chapters_success_mapsChapters() = runTest {
        val detailData = ComicDetail(
            id = "manga-slug",
            title = "Title",
            chapterList = listOf(
                ChapterItem(
                    id = "uuid-123",
                    number = "26",
                    title = "Chapter 26",
                    url = "/doujindesu/read/manga-slug/uuid-123",
                    lastUpdated = "2026-10-18T01:51:04.563Z",
                    downloadUrl = "https://doujin.desu.xxx/dl",
                ),
            ),
        )
        whenever(scraper.getComicDetail("manga-slug")).thenReturn(ApiResponse(data = detailData))

        val chapters = source.chapters("manga-slug")
        assertEquals(1, chapters.size)
        assertEquals("manga-slug:uuid-123", chapters[0].id)
        assertEquals("26", chapters[0].number)
        assertEquals("Chapter 26", chapters[0].title)
        assertEquals("/doujindesu/read/manga-slug/uuid-123", chapters[0].url)
        assertEquals("https://doujin.desu.xxx/dl", chapters[0].downloadUrl)
    }

    @Test
    fun pages_nullData_throwsIllegalStateException() = runTest {
        whenever(scraper.getChapter(any(), any())).thenReturn(ApiResponse(data = null, error = "Failed to load chapter"))

        try {
            source.pages("slug:uuid-123")
            fail("Expected IllegalStateException on null pages")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Failed to load chapter") == true)
        }
    }

    @Test
    fun pages_success_mapsUrlsToIndexedPages() = runTest {
        val urls = listOf("https://example.com/1.webp", "https://example.com/2.webp")
        whenever(scraper.getChapter("slug", "uuid-123")).thenReturn(ApiResponse(data = ChapterData(images = urls)))

        val pages = source.pages("slug:uuid-123")
        assertEquals(2, pages.size)
        assertEquals(0, pages[0].index)
        assertEquals("https://example.com/1.webp", pages[0].imageUrl)
        assertEquals(1, pages[1].index)
        assertEquals("https://example.com/2.webp", pages[1].imageUrl)
    }

    @Test
    fun allGenres_returnsScraperDataWhenAvailable() = runTest {
        val scraperGenres = listOf(
            Genre(id = "milf", name = "MILF", count = 2332),
            Genre(id = "yuri", name = "Yuri", count = 244),
        )
        whenever(scraper.getGenres()).thenReturn(ApiResponse(data = scraperGenres))

        val result = source.allGenres()
        assertEquals(2, result.size)
        assertEquals("milf", result[0].id)
        assertEquals("yuri", result[1].id)
    }

    @Test
    fun allGenres_fallsBackToMasterGenresWhenScraperReturnsEmpty() = runTest {
        whenever(scraper.getGenres()).thenReturn(ApiResponse(data = emptyList()))

        val result = source.allGenres()
        assertEquals(GenreMasterData.doujindesuMasterGenres, result)
    }

    @Test
    fun allGenres_fallsBackToMasterGenresWhenScraperThrows() = runTest {
        whenever(scraper.getGenres()).thenThrow(RuntimeException("Network error"))

        val result = source.allGenres()
        assertEquals(GenreMasterData.doujindesuMasterGenres, result)
    }
}
