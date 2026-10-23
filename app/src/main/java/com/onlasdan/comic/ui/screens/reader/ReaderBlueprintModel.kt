package com.onlasdan.comic.ui.screens.reader

data class ReaderLayoutState(
    val hasImages: Boolean,
    val showControls: Boolean,
    val showChaptersDialog: Boolean,
    val showSettingsDialog: Boolean,
)

enum class ReaderBrick {
    Loading,
    ImageList,
    OverlayControls,
    ChaptersDialog,
    SettingsDialog,
}
