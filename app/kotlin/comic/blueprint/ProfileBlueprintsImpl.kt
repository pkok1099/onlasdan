package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.profile.ProfileBrick
import comic.profile.ProfileLayoutState

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
