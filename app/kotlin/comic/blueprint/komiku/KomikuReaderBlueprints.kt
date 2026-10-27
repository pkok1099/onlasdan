package comic.blueprint.komiku

import comic.blueprint.ReaderBlueprints
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.reader.ReaderBrick
import comic.reader.ReaderLayoutState

object KomikuReaderBlueprints : ReaderBlueprints {
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
