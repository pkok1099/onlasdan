package com.onlasdan.comic.ui.blueprint.shinigami

import com.onlasdan.comic.ui.blueprint.ReaderBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.reader.ReaderBrick
import com.onlasdan.comic.ui.screens.reader.ReaderLayoutState

object ShinigamiReaderBlueprints : ReaderBlueprints {
    override val blueprint: ScreenBlueprint<ReaderLayoutState, ReaderBrick> = blueprint {
        slot(ScreenSlot.Body) {
            place(ReaderBrick.Loading, predicate = { !it.hasImages })
            place(ReaderBrick.ImageList, predicate = { it.hasImages })
        }
        slot(ScreenSlot.Overlay) {
            place(ReaderBrick.OverlayControls, predicate = { it.showControls })
        }
        slot(ScreenSlot.BottomSheet) {
            place(ReaderBrick.ChaptersDialog, predicate = { it.showChaptersDialog })
            place(ReaderBrick.SettingsDialog, predicate = { it.showSettingsDialog })
        }
    }
}
