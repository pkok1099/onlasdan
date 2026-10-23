package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.profile.ProfileBrick
import com.onlasdan.comic.ui.screens.profile.ProfileLayoutState

/**
 * Profile is source-agnostic; both sources reuse the same blueprint.
 */
object DefaultProfileBlueprints : ProfileBlueprints {
    override val blueprint: ScreenBlueprint<ProfileLayoutState, ProfileBrick> = blueprint {
        slot(ScreenSlot.Header) { place(ProfileBrick.TopBar, predicate = { true }) }
        slot(ScreenSlot.Body) {
            place(ProfileBrick.Header, predicate = { true })
            place(ProfileBrick.Tabs, predicate = { true })
            place(ProfileBrick.Content, predicate = { true })
        }
    }
}
