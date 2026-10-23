package com.onlasdan.comic.ui.blueprint.komikindo

import com.onlasdan.comic.ui.blueprint.UpdatesBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.updates.UpdatesBrick
import com.onlasdan.comic.ui.screens.updates.UpdatesLayoutState

object KomikindoUpdatesBlueprints : UpdatesBlueprints {
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
