package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.settings.SettingsBrick
import comic.settings.SettingsLayoutState

interface SettingsBlueprints {
    val blueprint: ScreenBlueprint<SettingsLayoutState, SettingsBrick>
}
