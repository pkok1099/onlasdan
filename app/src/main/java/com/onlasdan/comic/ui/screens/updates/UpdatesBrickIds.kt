package com.onlasdan.comic.ui.screens.updates

/**
 * String-based brick identifiers for UpdatesScreen.
 */
object UpdatesBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val CONTROLS_ROW = "komiku:controls_row"
    const val LOADING = "komiku:loading"
    const val ERROR = "komiku:error"
    const val EMPTY = "komiku:empty"
    const val RESULTS = "komiku:results"
    const val PAGINATION = "komiku:pagination"
}

/**
 * Extension to map UpdatesBrick enum to string IDs for backward compatibility.
 */
val UpdatesBrick.id: String
    get() = when (this) {
        UpdatesBrick.TopBar -> UpdatesBrickIds.TOP_BAR
        UpdatesBrick.ControlsRow -> UpdatesBrickIds.CONTROLS_ROW
        UpdatesBrick.Loading -> UpdatesBrickIds.LOADING
        UpdatesBrick.Error -> UpdatesBrickIds.ERROR
        UpdatesBrick.Empty -> UpdatesBrickIds.EMPTY
        UpdatesBrick.Results -> UpdatesBrickIds.RESULTS
        UpdatesBrick.Pagination -> UpdatesBrickIds.PAGINATION
    }
