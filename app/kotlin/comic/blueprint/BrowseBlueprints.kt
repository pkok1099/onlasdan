package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.browse.BrowseBrick
import comic.browse.BrowseState

interface BrowseBlueprints {
    val blueprint: ScreenBlueprint<BrowseState, BrowseBrick>
}
