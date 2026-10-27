# Adding a New Comic Source

This project uses an extensible source abstraction architecture. Adding a new source requires only **one new adapter file** implementing `ComicSource` and **one line of registration** in `SourceRegistry`. No UI changes or conditional checks (`if/when(source)`) are ever needed.

---

## 1. Capabilities

A source advertises what features it supports via `Set<Capability>`:

| Capability | Description |
|---|---|
| `SEARCH` | Source supports textual keyword search |
| `FILTER_TYPE` | Source supports filtering by media type (Manga, Manhwa, Manhua, Doujinshi) |
| `FILTER_GENRE` | Source supports filtering by genre tags |
| `FILTER_STATUS` | Source supports filtering by status (Ongoing, Completed) |
| `FILTER_SORT` | Source supports custom sorting (Latest, Popular, Rating, etc.) |
| `RANKING` | Source supports top/ranking queries |
| `LATEST` | Source provides a latest updates feed |
| `POPULAR` | Source provides a popular series feed |
| `HD_MODE` | Source supports toggling HD image qualities |
| `BOOKMARK_SYNC` | Source supports syncing remote bookmarks |

---

## 2. Source Adapter Template

Create a new file `app/kotlin/comic/source/MyNewSource.kt`:

```kotlin
package comic.source

import comic.R

class MyNewSource : ComicSource {
    override val id: String = "mynewsource"
    override val name: String = "My New Source"
    override val iconRes: Int = R.drawable.ic_source_default

    override val capabilities: Set<Capability> = setOf(
        Capability.SEARCH,
        Capability.FILTER_GENRE,
        Capability.FILTER_STATUS,
        Capability.LATEST,
        Capability.POPULAR
    )

    override suspend fun home(): List<Section> {
        // Fetch and map to List<Section> containing HERO, ROW, or GRID
        return listOf(
            Section(
                id = "hero",
                title = null,
                type = SectionType.HERO,
                items = listOf(/* Comic */)
            ),
            Section(
                id = "popular",
                title = "Series Populer",
                type = SectionType.ROW,
                items = listOf(/* Comic */)
            ),
            Section(
                id = "latest",
                title = "Update Terbaru",
                type = SectionType.GRID,
                items = listOf(/* Comic */)
            )
        )
    }

    override suspend fun search(query: String, filters: Map<String, String>, page: Int): Paged<Comic> {
        // Query scraper/API and return Paged<Comic>
        return Paged(
            items = listOf(/* Comic */),
            page = page,
            totalPages = 10,
            hasNextPage = page < 10
        )
    }

    override suspend fun detail(id: String): Comic {
        // Fetch comic details and return unified Comic model
        return Comic(
            id = id,
            title = "Manga Title",
            sourceId = this.id,
            sourceName = this.name
        )
    }

    override suspend fun chapters(id: String): List<Chapter> {
        // Fetch chapters and return List<Chapter>
        return listOf(
            Chapter(
                id = "$id:chapter-1",
                number = "1",
                title = "Chapter 1"
            )
        )
    }

    override suspend fun pages(chapterId: String): List<Page> {
        // Fetch image pages and return List<Page>
        return listOf(
            Page(index = 0, imageUrl = "https://example.com/page1.jpg")
        )
    }
}
```

---

## 3. Registration

Open `app/kotlin/comic/source/SourceRegistry.kt` and add your source to the `_sources` list:

```kotlin
private val _sources = mutableListOf<ComicSource>(
    ShinigamiSource(),
    KomikindoSource(),
    KomikuSource(),
    DoujindesuSource(),
    MyNewSource() // <-- Register your source here
)
```

That's it!
- The top bar source switcher automatically includes `MyNewSource`.
- Combined search queries `MyNewSource` in parallel alongside other sources.
- Home sections and detail drawer render dynamically according to its data and capabilities.
- Settings automatically displays `MyNewSource` in default source selection.
