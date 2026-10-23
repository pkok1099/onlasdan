package com.onlasdan.comic.ui.screens.profile

data class ProfileLayoutState(val hasBookmarks: Boolean, val hasHistory: Boolean)

enum class ProfileBrick {
    TopBar,
    Header,
    Tabs,
    Content,
}
