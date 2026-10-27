package comic.blueprint.doujindesu

import comic.blueprint.BrowseBlueprints
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.browse.BrowseBrick
import comic.browse.BrowseState

object DoujindesuBrowseBlueprints : BrowseBlueprints {
    override val blueprint = blueprint<BrowseState, BrowseBrick> {
        slot(ScreenSlot.Header) {
            place(BrowseBrick.DoujindesuHeader)
        }
        slot(ScreenSlot.Body) {
            place(BrowseBrick.LayoutRow)
            place(BrowseBrick.Results)
            place(BrowseBrick.Pagination) { it.data != null }
        }
        slot(ScreenSlot.BottomSheet) {
            place(BrowseBrick.DoujindesuFilterSheet) { it.showFilters }
        }
    }
}
