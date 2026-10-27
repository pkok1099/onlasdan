package comic.blueprint.komiku

import comic.blueprint.BrowseBlueprints
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.browse.BrowseBrick
import comic.browse.BrowseState

object KomikuBrowseBlueprints : BrowseBlueprints {
    override val blueprint = blueprint<BrowseState, BrowseBrick> {
        slot(ScreenSlot.Header) {
            place(BrowseBrick.KomikuHeader)
        }
        slot(ScreenSlot.Body) {
            place(BrowseBrick.LayoutRow)
            place(BrowseBrick.Results)
            place(BrowseBrick.Pagination) { it.data != null }
        }
        slot(ScreenSlot.BottomSheet) {
            place(BrowseBrick.KomikuFilterSheet) { it.showFilters }
        }
    }
}
