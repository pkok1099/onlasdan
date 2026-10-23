package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.browse.BrowseBrick
import com.onlasdan.comic.ui.screens.browse.BrowseState

interface BrowseBlueprints {
    val blueprint: ScreenBlueprint<BrowseState, BrowseBrick>
}
