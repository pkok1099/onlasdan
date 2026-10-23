package com.onlasdan.comic.ui.screens.reader

/**
 * String-based brick identifiers for ReaderScreen.
 */
object ReaderBrickIds {
    const val LOADING = "komiku:loading"
    const val IMAGE_LIST = "komiku:image_list"
    const val OVERLAY_CONTROLS = "komiku:overlay_controls"
    const val CHAPTERS_DIALOG = "komiku:chapters_dialog"
    const val SETTINGS_DIALOG = "komiku:settings_dialog"
}

/**
 * Extension to map ReaderBrick enum to string IDs for backward compatibility.
 */
val ReaderBrick.id: String
    get() = when (this) {
        ReaderBrick.Loading -> ReaderBrickIds.LOADING
        ReaderBrick.ImageList -> ReaderBrickIds.IMAGE_LIST
        ReaderBrick.OverlayControls -> ReaderBrickIds.OVERLAY_CONTROLS
        ReaderBrick.ChaptersDialog -> ReaderBrickIds.CHAPTERS_DIALOG
        ReaderBrick.SettingsDialog -> ReaderBrickIds.SETTINGS_DIALOG
    }
