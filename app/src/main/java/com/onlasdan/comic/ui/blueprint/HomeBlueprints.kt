package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.home.HomeBrick
import com.onlasdan.comic.ui.screens.home.HomeLayoutState

interface HomeBlueprints {
    val blueprint: ScreenBlueprint<HomeLayoutState, HomeBrick>
}
