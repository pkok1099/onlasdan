package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.reader.ReaderBrick
import comic.reader.ReaderLayoutState

interface ReaderBlueprints {
    val blueprint: ScreenBlueprint<ReaderLayoutState, ReaderBrick>
}
