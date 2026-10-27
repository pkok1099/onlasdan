package comic.blueprint.komiku

import comic.blueprint.HomeBlueprints
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.home.HomeBrick
import comic.home.HomeLayoutState

object KomikuHomeBlueprints : HomeBlueprints {
    override val blueprint: ScreenBlueprint<HomeLayoutState, HomeBrick> = blueprint {
        slot(ScreenSlot.Body) {
            place(HomeBrick.Hero)

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "UP", "title" to "Series Populer", "showViewAll" to true),
                predicate = { it.hasPopular },
            )
            place(HomeBrick.PopularRow, predicate = { it.hasPopular })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "HOT", "title" to "Hot Manga"),
                predicate = { it.hasHotManga },
            )
            place(HomeBrick.HotMangaRow, predicate = { it.hasHotManga })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "HOT", "title" to "Hot Manhwa"),
                predicate = { it.hasHotManhwa },
            )
            place(HomeBrick.HotManhwaRow, predicate = { it.hasHotManhwa })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "HOT", "title" to "Hot Manhua"),
                predicate = { it.hasHotManhua },
            )
            place(HomeBrick.HotManhuaRow, predicate = { it.hasHotManhua })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "BOOK", "title" to "Jelajahi Genre", "showViewAll" to true),
                predicate = { it.hasGenreCards },
            )
            place(HomeBrick.GenreCardsRow, predicate = { it.hasGenreCards })

            place(HomeBrick.Stats, predicate = { it.hasStats })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "NEW", "title" to "Update Terbaru"),
                predicate = { it.hasLatest },
            )
            place(HomeBrick.LatestControls, predicate = { it.hasLatest })
            place(HomeBrick.LatestGrid, predicate = { it.hasLatest })
        }
    }
}
