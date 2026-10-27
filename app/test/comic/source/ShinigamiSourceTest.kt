package comic.source

import comic.api.*
import comic.scraper.GenreMasterData
import comic.scraper.ShinigamiApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ShinigamiSourceTest {

    private val api: ShinigamiApi = mock()
    private val source = ShinigamiSource(api)

    @Test
    fun sourceMetadata_idAndNameAreCorrect() {
        assertEquals("shinigami", source.id)
        assertEquals("Shinigami", source.name)
        assertTrue(source.capabilities.contains(Capability.SEARCH))
        assertTrue(source.capabilities.contains(Capability.FILTER_GENRE))
        assertTrue(source.capabilities.contains(Capability.FILTER_TYPE))
        assertTrue(source.capabilities.contains(Capability.FILTER_STATUS))
        assertTrue(source.capabilities.contains(Capability.FILTER_SORT))
        assertTrue(source.capabilities.contains(Capability.HERO_CAROUSEL))
        assertTrue(source.capabilities.contains(Capability.MANGA_FEED_LAYOUT))
    }

    @Test
    fun availableFilters_containsAllExpectedFiltersAnd51Genres() {
        val filters = source.availableFilters()
        val filterKeys = filters.map { it.key }
        assertTrue(filterKeys.contains("sort"))
        assertTrue(filterKeys.contains("sort_order"))
        assertTrue(filterKeys.contains("format"))
        assertTrue(filterKeys.contains("status"))
        assertTrue(filterKeys.contains("genre"))

        val genreFilter = filters.first { it.key == "genre" }
        assertEquals(51, genreFilter.options.size)
        assertEquals(GenreMasterData.shinigamiMasterGenres.size, genreFilter.options.size)
        assertEquals("action", genreFilter.options.first().id)
        assertEquals("Action", genreFilter.options.first().label)
    }

    @Test
    fun allGenres_returns51GenresFromGenreMasterData() = runTest {
        val genres = source.allGenres()
        assertEquals(51, genres.size)
        assertEquals(GenreMasterData.shinigamiMasterGenres, genres)
        assertEquals("action", genres.first().id)
        assertEquals("Action", genres.first().name)
    }

    @Test
    fun home_nullData_throwsRuntimeExceptionWithApiError() = runTest {
        whenever(api.getHome()).thenReturn(ApiResponse(data = null, error = "Gateway Timeout 504"))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertTrue("Expected shinigami prefix", e.message?.startsWith("shinigami:") == true)
            assertTrue("Expected error content", e.message?.contains("Gateway Timeout 504") == true)
        }
    }

    @Test
    fun home_nullDataAndNullError_throwsRuntimeExceptionWithDefaultMessage() = runTest {
        whenever(api.getHome()).thenReturn(ApiResponse(data = null, error = null))

        try {
            source.home()
            fail("Expected RuntimeException when home data is null")
        } catch (e: RuntimeException) {
            assertEquals("shinigami: respons kosong", e.message)
        }
    }

    @Test
    fun home_mapsHeroRecommendedLatestAndPopularSections() = runTest {
        val popular = listOf(
            HomeComicItem(
                title = "Popular Comic",
                url = "/shinigami/comic/popular-1",
                cover = "https://assets.shngm.id/pop.jpg",
                rating = "9.0",
                type = "manhwa",
            ),
        )
        val latest = listOf(
            HomeComicItem(
                title = "Latest Comic",
                url = "/shinigami/comic/latest-1",
                cover = "https://assets.shngm.id/lat.jpg",
                rating = "8.5",
                type = "manga",
            ),
        )
        val recommended = listOf(
            ComicListItem(
                title = "Recommended Comic",
                url = "/shinigami/comic/rec-1",
                cover = "https://assets.shngm.id/rec.jpg",
                rating = 9.5,
                type = "manhwa",
            ),
        )

        whenever(api.getHome()).thenReturn(
            ApiResponse(
                data = HomepageData(
                    popularComics = popular,
                    latestComics = latest,
                ),
            ),
        )
        whenever(api.getRecommended(any(), eq(10), anyOrNull())).thenReturn(
            ApiResponse(data = recommended),
        )

        val sections = source.home()
        assertEquals(4, sections.size)
        assertEquals("hero", sections[0].id)
        assertEquals(SectionType.HERO, sections[0].type)
        assertEquals("recommendations", sections[1].id)
        assertEquals(SectionType.ROW, sections[1].type)
        assertEquals("latest", sections[2].id)
        assertEquals(SectionType.GRID, sections[2].type)
        assertEquals("popular", sections[3].id)
        assertEquals(SectionType.ROW, sections[3].type)
    }

    @Test
    fun search_mapsBrowseResponseToPagedComicCorrectly() = runTest {
        val comics = listOf(
            ComicListItem(
                title = "Magic Emperor",
                url = "/shinigami/comic/magic-emperor",
                cover = "https://assets.shngm.id/cover.jpg",
                type = "manhua",
                rating = 9.2,
                latestChapter = HomeLatestChapter(title = "Ch. 500", url = "ch-500"),
            ),
        )
        val browseData = BrowseData(
            comics = comics,
            pagination = PaginationData(currentPage = 1, totalPages = 10),
        )
        whenever(
            api.browse(
                page = eq(1),
                sort = eq("popularity"),
                sortOrder = eq("desc"),
                status = eq("ongoing"),
                format = eq("manhua"),
                type = anyOrNull(),
                genreInclude = eq("action"),
                genreIncludeMode = anyOrNull(),
                search = eq("magic"),
            ),
        ).thenReturn(ApiResponse(data = browseData))

        val filters = mapOf(
            "sort" to "popularity",
            "sort_order" to "desc",
            "status" to "ongoing",
            "format" to "manhua",
            "genre" to "action",
        )
        val result = source.search("magic", filters, page = 1)

        assertEquals(1, result.items.size)
        val comic = result.items[0]
        assertEquals("magic-emperor", comic.id)
        assertEquals("Magic Emperor", comic.title)
        assertEquals("https://assets.shngm.id/cover.jpg", comic.cover)
        assertEquals("manhua", comic.type)
        assertEquals("9.2", comic.rating)
        assertEquals("Ch. 500", comic.latestChapter)
        assertEquals("shinigami", comic.sourceId)
        assertEquals("Shinigami", comic.sourceName)

        assertEquals(1, result.page)
        assertEquals(10, result.totalPages)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun search_throwsRuntimeExceptionWhenApiReturnsError() = runTest {
        whenever(
            api.browse(
                page = any(),
                sort = anyOrNull(),
                sortOrder = anyOrNull(),
                status = anyOrNull(),
                format = anyOrNull(),
                type = anyOrNull(),
                genreInclude = anyOrNull(),
                genreIncludeMode = anyOrNull(),
                search = anyOrNull(),
            ),
        ).thenReturn(ApiResponse(data = null, error = "Server Error 500"))

        try {
            source.search("query", emptyMap(), 1)
            fail("Expected RuntimeException when api returns error")
        } catch (e: RuntimeException) {
            assertEquals("shinigami: Server Error 500", e.message)
        }
    }

    @Test
    fun search_throwsRuntimeExceptionWhenDataIsNull() = runTest {
        whenever(
            api.browse(
                page = any(),
                sort = anyOrNull(),
                sortOrder = anyOrNull(),
                status = anyOrNull(),
                format = anyOrNull(),
                type = anyOrNull(),
                genreInclude = anyOrNull(),
                genreIncludeMode = anyOrNull(),
                search = anyOrNull(),
            ),
        ).thenReturn(ApiResponse(data = null, error = null))

        try {
            source.search("query", emptyMap(), 1)
            fail("Expected RuntimeException when api data is null")
        } catch (e: RuntimeException) {
            assertEquals("shinigami: respons kosong", e.message)
        }
    }

    @Test
    fun detail_nullData_throwsIllegalStateException() = runTest {
        whenever(api.getComicDetail("slug")).thenReturn(ApiResponse(data = null, error = "Detail error"))

        try {
            source.detail("slug")
            fail("Expected IllegalStateException on null detail data")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Detail error") == true)
        }
    }

    @Test
    fun pages_nullData_throwsIllegalStateException() = runTest {
        whenever(api.getChapter(any(), any())).thenReturn(ApiResponse(data = null, error = "Page error"))

        try {
            source.pages("slug:ch-1")
            fail("Expected IllegalStateException on null pages")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Page error") == true)
        }
    }

    @Test
    fun sectionFeed_recommendations_fetchesWithFormat() = runTest {
        val items = listOf(
            ComicListItem(
                title = "Kagurabachi",
                url = "/shinigami/comic/kagurabachi",
                cover = "https://assets.shngm.id/kag.jpg",
                type = "Manga",
            ),
        )
        whenever(api.getRecommended(page = 1, pageSize = 10, format = "manga")).thenReturn(
            ApiResponse(data = items),
        )

        val result = source.sectionFeed("recommendations", "manga", page = 1)
        assertEquals(1, result.items.size)
        assertEquals("Kagurabachi", result.items[0].title)
        assertEquals("Manga", result.items[0].type)
    }

    @Test
    fun sectionFeed_updates_fetchesWithTypeAndPagination() = runTest {
        val items = listOf(
            ComicListItem(
                title = "Mirror Comic",
                url = "/shinigami/comic/mirror-comic",
                cover = "https://assets.shngm.id/mirror.jpg",
                type = "Manhwa",
            ),
        )
        val browseData = BrowseData(
            comics = items,
            pagination = PaginationData(currentPage = 2, totalPages = 50),
        )
        whenever(api.getUpdates(page = 2, pageSize = 24, type = "mirror")).thenReturn(
            ApiResponse(data = browseData),
        )

        val result = source.sectionFeed("latest", "mirror", page = 2)
        assertEquals(1, result.items.size)
        assertEquals("Mirror Comic", result.items[0].title)
        assertEquals(2, result.page)
        assertEquals(50, result.totalPages)
        assertTrue(result.hasNextPage)
    }
}
