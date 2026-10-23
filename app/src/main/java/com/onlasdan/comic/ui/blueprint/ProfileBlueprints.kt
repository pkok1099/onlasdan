package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.profile.ProfileBrick
import com.onlasdan.comic.ui.screens.profile.ProfileLayoutState

interface ProfileBlueprints {
    val blueprint: ScreenBlueprint<ProfileLayoutState, ProfileBrick>
}
