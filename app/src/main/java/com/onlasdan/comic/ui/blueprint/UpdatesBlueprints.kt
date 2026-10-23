package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.updates.UpdatesBrick
import com.onlasdan.comic.ui.screens.updates.UpdatesLayoutState

interface UpdatesBlueprints {
    val blueprint: ScreenBlueprint<UpdatesLayoutState, UpdatesBrick>
}
