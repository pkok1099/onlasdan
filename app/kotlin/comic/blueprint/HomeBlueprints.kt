package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.home.HomeBrick
import comic.home.HomeLayoutState

interface HomeBlueprints {
    val blueprint: ScreenBlueprint<HomeLayoutState, HomeBrick>
}
