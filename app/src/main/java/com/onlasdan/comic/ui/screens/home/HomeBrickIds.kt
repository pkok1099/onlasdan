package com.onlasdan.comic.ui.screens.home

/**
 * String-based brick identifiers for HomeScreen.
 *
 * These IDs are source-specific and allow for future extensibility.
 * Format: "source:type_name" (e.g., "komiku:hero", "generic:header")
 *
 * This enables:
 * - New sources to add custom brick IDs without modifying the enum
 * - Runtime brick registration
 * - Composable brick types
 */
object HomeBrickIds {
    // Common/generic bricks (used by all sources)
    const val HERO = "komiku:hero"
    const val GENERIC_HEADER = "generic:header"
    const val POPULAR_ROW = "komiku:popular_row"
    const val HOT_MANGA_ROW = "komiku:hot_manga_row"
    const val HOT_MANHWA_ROW = "komiku:hot_manhwa_row"
    const val HOT_MANHUA_ROW = "komiku:hot_manhua_row"
    const val GENRE_CARDS_ROW = "komiku:genre_cards_row"
    const val STATS = "komiku:stats"
    const val LATEST_CONTROLS = "komiku:latest_controls"
    const val LATEST_GRID = "komiku:latest_grid"
    const val EXPLORE_GENRES_CARD = "komiku:explore_genres_card"
    const val SPACER = "generic:spacer"
}

/**
 * Extension to map HomeBrick enum to string IDs for backward compatibility.
 */
val HomeBrick.id: String
    get() = when (this) {
        HomeBrick.Hero -> HomeBrickIds.HERO
        HomeBrick.GenericHeader -> HomeBrickIds.GENERIC_HEADER
        HomeBrick.PopularRow -> HomeBrickIds.POPULAR_ROW
        HomeBrick.HotMangaRow -> HomeBrickIds.HOT_MANGA_ROW
        HomeBrick.HotManhwaRow -> HomeBrickIds.HOT_MANHWA_ROW
        HomeBrick.HotManhuaRow -> HomeBrickIds.HOT_MANHUA_ROW
        HomeBrick.GenreCardsRow -> HomeBrickIds.GENRE_CARDS_ROW
        HomeBrick.Stats -> HomeBrickIds.STATS
        HomeBrick.LatestControls -> HomeBrickIds.LATEST_CONTROLS
        HomeBrick.LatestGrid -> HomeBrickIds.LATEST_GRID
        HomeBrick.ExploreGenresCard -> HomeBrickIds.EXPLORE_GENRES_CARD
        HomeBrick.Spacer -> HomeBrickIds.SPACER
    }
