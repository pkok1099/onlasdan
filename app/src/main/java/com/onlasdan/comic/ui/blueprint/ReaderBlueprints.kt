package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.reader.ReaderBrick
import com.onlasdan.comic.ui.screens.reader.ReaderLayoutState

interface ReaderBlueprints {
    val blueprint: ScreenBlueprint<ReaderLayoutState, ReaderBrick>
}
