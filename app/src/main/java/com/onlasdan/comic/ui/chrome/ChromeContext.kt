package com.onlasdan.comic.ui.chrome

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf

enum class ChromeRouteTag {
    Home,
    Browse,
    Genres,
    Profile,
}

data class ChromeContext(
    val routeTag: ChromeRouteTag,
    val isHd: Boolean? = null,
    val columns: Int? = null,
    val openSearch: (() -> Unit)? = null,
    val openDownloads: (() -> Unit)? = null,
    val toggleHd: (() -> Unit)? = null,
    val setColumns: ((Int) -> Unit)? = null,
    val openBrowseFilter: (() -> Unit)? = null,
    val resetBrowseFilter: (() -> Unit)? = null,
    val openGenresSearch: (() -> Unit)? = null,
    val toggleGenresSort: (() -> Unit)? = null,
    val setHistorySort: ((String) -> Unit)? = null,
)

val LocalChromeContext = staticCompositionLocalOf<MutableState<ChromeContext?>> {
    error("LocalChromeContext not provided")
}
