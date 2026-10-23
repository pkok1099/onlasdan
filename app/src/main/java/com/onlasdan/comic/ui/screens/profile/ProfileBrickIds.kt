package com.onlasdan.comic.ui.screens.profile

/**
 * String-based brick identifiers for ProfileScreen.
 */
object ProfileBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val HEADER = "komiku:header"
    const val TABS = "komiku:tabs"
    const val CONTENT = "komiku:content"
}

/**
 * Extension to map ProfileBrick enum to string IDs for backward compatibility.
 */
val ProfileBrick.id: String
    get() = when (this) {
        ProfileBrick.TopBar -> ProfileBrickIds.TOP_BAR
        ProfileBrick.Header -> ProfileBrickIds.HEADER
        ProfileBrick.Tabs -> ProfileBrickIds.TABS
        ProfileBrick.Content -> ProfileBrickIds.CONTENT
    }
