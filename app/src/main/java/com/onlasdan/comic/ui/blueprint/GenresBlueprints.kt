package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.genres.GenreDetailBrick
import com.onlasdan.comic.ui.screens.genres.GenreDetailLayoutState
import com.onlasdan.comic.ui.screens.genres.GenresListBrick
import com.onlasdan.comic.ui.screens.genres.GenresListLayoutState

interface GenresBlueprints {
    val list: ScreenBlueprint<GenresListLayoutState, GenresListBrick>
    val detail: ScreenBlueprint<GenreDetailLayoutState, GenreDetailBrick>
}
