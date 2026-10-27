package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.updates.UpdatesBrick
import comic.updates.UpdatesLayoutState

interface UpdatesBlueprints {
    val blueprint: ScreenBlueprint<UpdatesLayoutState, UpdatesBrick>
}
