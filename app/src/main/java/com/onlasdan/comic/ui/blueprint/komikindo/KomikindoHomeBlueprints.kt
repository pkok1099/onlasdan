package com.onlasdan.comic.ui.blueprint.komikindo

import com.onlasdan.comic.ui.blueprint.HomeBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.home.HomeBrick
import com.onlasdan.comic.ui.screens.home.HomeLayoutState

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
