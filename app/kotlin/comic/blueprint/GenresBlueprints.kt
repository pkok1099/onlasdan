package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.genres.GenreDetailBrick
import comic.genres.GenreDetailLayoutState
import comic.genres.GenresListBrick
import comic.genres.GenresListLayoutState

interface GenresBlueprints {
    val list: ScreenBlueprint<GenresListLayoutState, GenresListBrick>
    val detail: ScreenBlueprint<GenreDetailLayoutState, GenreDetailBrick>
}
