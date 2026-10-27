package comic.blueprint.komikindo

import comic.blueprint.HomeBlueprints
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.home.HomeBrick
import comic.home.HomeLayoutState

object KomikindoHomeBlueprints : HomeBlueprints {
    override val blueprint: ScreenBlueprint<HomeLayoutState, HomeBrick> = blueprint {
        slot(ScreenSlot.Body) {
            place(HomeBrick.Hero)

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "UP", "title" to "Popular Series", "showViewAll" to true),
                predicate = { it.hasPopular },
            )
            place(HomeBrick.PopularRow, predicate = { it.hasPopular })

            place(
                HomeBrick.GenericHeader,
                metadata = mapOf("badge" to "NEW", "title" to "Latest Updates"),
                predicate = { it.hasLatest },
            )
            place(HomeBrick.LatestControls, predicate = { it.hasLatest })
            place(HomeBrick.LatestGrid, predicate = { it.hasLatest })
        }
    }
}
