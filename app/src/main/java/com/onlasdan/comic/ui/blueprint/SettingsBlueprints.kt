package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.settings.SettingsBrick
import com.onlasdan.comic.ui.screens.settings.SettingsLayoutState

interface SettingsBlueprints {
    val blueprint: ScreenBlueprint<SettingsLayoutState, SettingsBrick>
}
