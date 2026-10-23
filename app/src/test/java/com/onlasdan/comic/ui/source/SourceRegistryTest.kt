package com.onlasdan.comic.ui.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRegistryTest {

    @Test
    fun sources_initialList_containsDefaultSources() {
        val sources = SourceRegistry.sources
        assertTrue(sources.isNotEmpty())
        val sourceIds = sources.map { it.id.lowercase() }
        assertTrue(sourceIds.contains("komikindo"))
        assertTrue(sourceIds.contains("komiku"))
        assertTrue(sourceIds.contains("shinigami"))
        assertTrue(sourceIds.contains("doujindesu"))
    }

    @Test
    fun defaultSource_isNotNullAndHasId() {
        val defaultSource = SourceRegistry.defaultSource
        assertNotNull(defaultSource)
        assertTrue(defaultSource.id.isNotBlank())
    }

    @Test
    fun get_withKnownId_returnsMatchingSource() {
        val komikindo = SourceRegistry.get("komikindo")
        assertEquals("komikindo", komikindo.id.lowercase())

        val shinigami = SourceRegistry.get("shinigami")
        assertEquals("shinigami", shinigami.id.lowercase())
    }

    @Test
    fun get_withCaseInsensitiveId_returnsMatchingSource() {
        val komiku = SourceRegistry.get("KOMIKU")
        assertEquals("komiku", komiku.id.lowercase())
    }

    @Test
    fun get_withUnknownId_fallsBackToDefaultSource() {
        val fallback = SourceRegistry.get("non_existent_source_12345")
        assertEquals(SourceRegistry.defaultSource.id, fallback.id)
    }

    @Test
    fun registerAndUnregister_customSource_updatesListSafely() {
        val dummy = object : ComicSource {
            override val id: String = "dummy_test_source"
            override val name: String = "Dummy"
            override val iconRes: Int = 0
            override val capabilities: Set<Capability> = emptySet()
            override suspend fun home(): List<Section> = emptyList()
            override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> = Paged(emptyList(), page = 1)
            override suspend fun detail(id: String): Comic = Comic(id = id, title = "Dummy")
            override suspend fun chapters(id: String): List<Chapter> = emptyList()
            override suspend fun pages(chapterId: String): List<Page> = emptyList()
        }

        try {
            SourceRegistry.register(dummy)
            assertEquals("dummy_test_source", SourceRegistry.get("dummy_test_source").id)

            // Register again should be idempotent
            SourceRegistry.register(dummy)
            val occurrences = SourceRegistry.sources.count { it.id == "dummy_test_source" }
            assertEquals(1, occurrences)
        } finally {
            SourceRegistry.unregister("dummy_test_source")
            val removed = SourceRegistry.sources.none { it.id == "dummy_test_source" }
            assertTrue(removed)
        }
    }
}
