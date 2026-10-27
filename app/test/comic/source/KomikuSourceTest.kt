package comic.source

import comic.api.*
import comic.scraper.GenreMasterData
import comic.scraper.KomikuScraper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KomikuSourceTest {

    private val scraper: KomikuScraper = mock()
    private val source = KomikuSource(scraper)

    @Test
    fun komikuMasterGenres_contains112DistinctOfficialGenres() {
        val genres = GenreMasterData.komikuMasterGenres
        assertEquals(112, genres.size)

        val uniqueIds = genres.map { it.id }.toSet()
        assertEquals("Genre IDs must be distinct", 112, uniqueIds.size)

        val girlsLove = genres.firstOrNull { it.id == "girls-love" }
        assertNotNull("girls-love genre should exist", girlsLove)
        assertEquals("Girls' Love", girlsLove?.name)

        assertTrue(
            "Genre names should not contain HTML entities like &",
            genres.none { it.name.contains("&") },
        )
    }

    @Test
    fun komikuSource_capabilities_containsExpectedSet() {
        val expectedCapabilities = setOf(
            Capability.SEARCH,
            Capability.FILTER_TYPE,
            Capability.FILTER_GENRE,
            Capability.FILTER_STATUS,
            Capability.FILTER_SORT,
            Capability.LATEST,
            Capability.POPULAR,
        )
        assertEquals(expectedCapabilities, source.capabilities)
        assertEquals("komiku", source.id)
        assertEquals("Komiku", source.name)
    }

    @Test
    fun komikuSource_allGenres_returnsKomikuMasterGenres() = runTest {
        val genres = KomikuSource().allGenres()
        assertEquals(112, genres.size)
        assertEquals(GenreMasterData.komikuMasterGenres, genres)
    }

    @Test
    fun komikuSource_filters_hasExpectedStructure() {
        val filters = KomikuSource().availableFilters()
        val filterKeys = filters.map { it.key }
        assertTrue(filterKeys.contains("orderby"))
        assertTrue(filterKeys.contains("tipe"))
        assertTrue(filterKeys.contains("status"))
        assertTrue(filterKeys.contains("genre"))
        assertTrue(filterKeys.contains("sorttime"))

        val genreFilter = filters.first { it.key == "genre" }
        assertEquals(112, genreFilter.options.size)
        assertEquals(
            GenreMasterData.komikuMasterGenres.map { FilterOption(it.id, it.name) },
            genreFilter.options,
        )

        val sorttimeFilter = filters.first { it.key == "sorttime" }
        assertEquals(mapOf("orderby" to "meta_value_num"), sorttimeFilter.enabledIf)
    }

    @Test
    fun home_nullData_throwsRuntimeExceptionWithApiError() = runTest {
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = null, error = "Timeout 504"))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertTrue("Expected komiku prefix", e.message?.startsWith("komiku:") == true)
            assertTrue("Expected error message", e.message?.contains("Timeout 504") == true)
        }
    }

    @Test
    fun home_nullDataAndNullError_throwsRuntimeExceptionWithDefaultMessage() = runTest {
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = null, error = null))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertEquals("komiku: respons kosong", e.message)
        }
    }

    @Test
    fun home_success_mapsHeroPopularAndLatestSections() = runTest {
        val heroComic = HomeHeroComic(
            title = "Hero Comic",
            url = "https://komiku.org/manga/hero-comic/",
            cover = "https://cdn.komiku.co.id/hero.jpg",
            rating = "9.0",
            type = "Manga",
            author = "Hero Author",
            description = "Hero synopsis",
            latestChapter = HomeLatestChapter(title = "Ch. 50", url = "/read/50"),
        )
        val popular = listOf(
            HomeComicItem(
                title = "Popular 1",
                url = "https://komiku.org/manga/pop-1/",
                cover = "https://cdn.komiku.co.id/pop1.jpg",
                latestChapter = HomeLatestChapter(title = "Ch. 10", url = "/read/10"),
            ),
        )
        val latest = listOf(
            HomeComicItem(
                title = "Latest 1",
                url = "https://komiku.org/manga/latest-1/",
                cover = "https://cdn.komiku.co.id/lat1.jpg",
                latestChapter = HomeLatestChapter(title = "Ch. 1", url = "/read/1"),
            ),
        )
        val homeData = HomepageData(
            heroComic = heroComic,
            popularComics = popular,
            latestComics = latest,
        )
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = homeData))

        val sections = source.home()
        assertEquals(3, sections.size)

        val heroSection = sections[0]
        assertEquals("hero", heroSection.id)
        assertEquals(SectionType.HERO, heroSection.type)
        assertEquals(1, heroSection.items.size)
        assertEquals("hero-comic", heroSection.items[0].id)
        assertEquals("Hero Comic", heroSection.items[0].title)

        val popularSection = sections[1]
        assertEquals("popular", popularSection.id)
        assertEquals(SectionType.ROW, popularSection.type)
        assertEquals("Series Populer", popularSection.title)
        assertTrue(popularSection.hasMore)

        val latestSection = sections[2]
        assertEquals("latest", latestSection.id)
        assertEquals(SectionType.GRID, latestSection.type)
        assertEquals("Update Terbaru", latestSection.title)
        assertTrue(latestSection.hasMore)
    }

    @Test
    fun home_nullHeroComic_synthesizesHeroFromPopular() = runTest {
        val popular = listOf(
            HomeComicItem(
                title = "Popular Fallback",
                url = "https://komiku.org/manga/pop-fallback/",
                cover = "https://cdn.komiku.co.id/pop.jpg",
                latestChapter = HomeLatestChapter(title = "Ch. 10", url = "/read/10"),
            ),
        )
        val homeData = HomepageData(
            heroComic = null,
            popularComics = popular,
            latestComics = emptyList(),
        )
        whenever(scraper.getHome()).thenReturn(ApiResponse(data = homeData))

        val sections = source.home()
        assertEquals(2, sections.size)

        val heroSection = sections[0]
        assertEquals("hero", heroSection.id)
        assertEquals(SectionType.HERO, heroSection.type)
        assertEquals("pop-fallback", heroSection.items[0].id)
        assertEquals("Popular Fallback", heroSection.items[0].title)
    }

    @Test
    fun search_withQuery_callsScraperSearch() = runTest {
        val searchData = SearchData(
            comics = listOf(
                ComicListItem(
                    title = "Solo Leveling",
                    url = "https://komiku.org/manga/solo-leveling/",
                    cover = "https://cdn.komiku.co.id/sl.jpg",
                    type = "Manhwa",
                    rating = 9.8,
                    latestChapter = HomeLatestChapter(title = "Ch. 200", url = "/read/200"),
                ),
            ),
            totalPages = 5,
        )
        whenever(scraper.search(eq("Solo"), eq(1))).thenReturn(ApiResponse(data = searchData))

        val result = source.search("Solo", emptyMap(), page = 1)
        assertEquals(1, result.items.size)
        val comic = result.items[0]
        assertEquals("solo-leveling", comic.id)
        assertEquals("Solo Leveling", comic.title)
        assertEquals("https://cdn.komiku.co.id/sl.jpg", comic.cover)
        assertEquals("Manhwa", comic.type)
        assertEquals("9.8", comic.rating)
        assertEquals("Ch. 200", comic.latestChapter)
        assertEquals("komiku", comic.sourceId)
        assertEquals("Komiku", comic.sourceName)
        assertEquals(1, result.page)
        assertEquals(5, result.totalPages)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun search_emptyQuery_callsScraperBrowseWithFilters() = runTest {
        val browseData = BrowseData(
            comics = listOf(
                ComicListItem(
                    title = "Magic Emperor",
                    url = "https://komiku.org/manga/magic-emperor/",
                    cover = "https://cdn.komiku.co.id/me.jpg",
                    type = "Manhua",
                ),
            ),
            pagination = PaginationData(currentPage = 1, totalPages = 3),
        )
        whenever(
            scraper.browse(
                page = eq(1),
                orderby = eq("meta_value_num"),
                tipe = eq("manhua"),
                status = eq("ongoing"),
                genre = eq("action"),
                sorttime = eq("weekly"),
            ),
        ).thenReturn(ApiResponse(data = browseData))

        val filters = mapOf(
            "orderby" to "meta_value_num",
            "type" to "manhua",
            "status" to "ongoing",
            "genre" to "action",
            "sorttime" to "weekly",
        )
        val result = source.search("   ", filters, page = 1)
        assertEquals(1, result.items.size)
        assertEquals("magic-emperor", result.items[0].id)
        assertEquals(1, result.page)
        assertEquals(3, result.totalPages)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun detail_nullData_throwsIllegalStateException() = runTest {
        whenever(scraper.getComicDetail("unknown-comic")).thenReturn(ApiResponse(data = null, error = "Not found"))

        try {
            source.detail("unknown-comic")
            fail("Expected IllegalStateException on null detail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Not found") == true)
        }
    }

    @Test
    fun detail_success_mapsComicFields() = runTest {
        val detailData = ComicDetail(
            id = "one-piece",
            title = "One Piece",
            thumbnail = "https://cdn.komiku.co.id/op.jpg",
            rating = "9.9",
            status = "Ongoing",
            type = "Manga",
            author = "Oda",
            illustrator = "Oda",
            synopsis = "Pirate adventure",
            genres = listOf("Action", "Adventure"),
            chapterList = listOf(
                ChapterItem(id = "ch-1100", number = "1100", title = "Chapter 1100"),
            ),
            lastUpdated = "1 jam lalu",
        )
        whenever(scraper.getComicDetail("one-piece")).thenReturn(ApiResponse(data = detailData))

        val comic = source.detail("one-piece")
        assertEquals("one-piece", comic.id)
        assertEquals("One Piece", comic.title)
        assertEquals("https://cdn.komiku.co.id/op.jpg", comic.cover)
        assertEquals("9.9", comic.rating)
        assertEquals("Ongoing", comic.status)
        assertEquals("Manga", comic.type)
        assertEquals("Oda", comic.author)
        assertEquals("Oda", comic.illustrator)
        assertEquals("Pirate adventure", comic.synopsis)
        assertEquals(2, comic.genres.size)
        assertEquals("Action", comic.genres[0].id)
        assertEquals("1100", comic.latestChapter)
        assertEquals("komiku", comic.sourceId)
        assertEquals("Komiku", comic.sourceName)
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
            id = "naruto",
            title = "Naruto",
            chapterList = listOf(
                ChapterItem(
                    id = "ch-700",
                    number = "700",
                    title = "Chapter 700",
                    url = "https://komiku.org/naruto-chapter-700/",
                    lastUpdated = "2026-10-18",
                ),
            ),
        )
        whenever(scraper.getComicDetail("naruto")).thenReturn(ApiResponse(data = detailData))

        val chapters = source.chapters("naruto")
        assertEquals(1, chapters.size)
        assertEquals("naruto:ch-700", chapters[0].id)
        assertEquals("700", chapters[0].number)
        assertEquals("Chapter 700", chapters[0].title)
    }

    @Test
    fun pages_nullData_throwsIllegalStateException() = runTest {
        whenever(scraper.getChapter(any(), any(), anyOrNull())).thenReturn(ApiResponse(data = null, error = "Failed to load pages"))

        try {
            source.pages("comic-slug:chapter-slug")
            fail("Expected IllegalStateException on null pages")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Failed to load pages") == true)
        }
    }

    @Test
    fun pages_success_mapsUrlsToIndexedPages() = runTest {
        val images = listOf("https://cdn.komiku.co.id/1.jpg", "https://cdn.komiku.co.id/2.jpg")
        whenever(scraper.getChapter("naruto", "chapter-1", null)).thenReturn(ApiResponse(data = ChapterData(images = images)))

        val pages = source.pages("naruto:chapter-1")
        assertEquals(2, pages.size)
        assertEquals(0, pages[0].index)
        assertEquals("https://cdn.komiku.co.id/1.jpg", pages[0].imageUrl)
        assertEquals(1, pages[1].index)
        assertEquals("https://cdn.komiku.co.id/2.jpg", pages[1].imageUrl)
    }
}
