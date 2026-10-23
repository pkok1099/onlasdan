package com.onlasdan.comic.ui.blueprint.shinigami

import com.onlasdan.comic.ui.blueprint.BrowseBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.browse.BrowseBrick
import com.onlasdan.comic.ui.screens.browse.BrowseState

object ShinigamiBrowseBlueprints : BrowseBlueprints {
    override val blueprint = blueprint<BrowseState, BrowseBrick> {
        slot(ScreenSlot.Header) {
            place(BrowseBrick.ShinigamiHeader)
        }
        slot(ScreenSlot.Body) {
            place(BrowseBrick.LayoutRow)
            place(BrowseBrick.Results)
            place(BrowseBrick.Pagination) { it.data != null }
        }
        slot(ScreenSlot.BottomSheet) {
            place(BrowseBrick.ShinigamiFilterSheet) { it.showFilters }
        }
    }
}
