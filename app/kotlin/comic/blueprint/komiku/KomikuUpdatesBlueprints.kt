package comic.blueprint.komiku

import comic.blueprint.UpdatesBlueprints
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.updates.UpdatesBrick
import comic.updates.UpdatesLayoutState

object KomikuUpdatesBlueprints : UpdatesBlueprints {
    override val blueprint: ScreenBlueprint<UpdatesLayoutState, UpdatesBrick> = blueprint {
        slot(ScreenSlot.Header) { place(UpdatesBrick.TopBar) }
        slot(ScreenSlot.Body) {
            place(UpdatesBrick.ControlsRow)
            place(UpdatesBrick.Loading, predicate = { it.isLoading && !it.hasData })
            place(UpdatesBrick.Error, predicate = { it.hasError })
            place(UpdatesBrick.Empty, predicate = { !it.isLoading && !it.hasError && !it.hasData })
            place(UpdatesBrick.Results, predicate = { it.hasResults && !it.hasError })
            place(UpdatesBrick.Pagination, predicate = { it.hasResults && !it.hasError })
        }
    }
}
