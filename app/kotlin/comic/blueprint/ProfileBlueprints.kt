package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.profile.ProfileBrick
import comic.profile.ProfileLayoutState

interface ProfileBlueprints {
    val blueprint: ScreenBlueprint<ProfileLayoutState, ProfileBrick>
}
