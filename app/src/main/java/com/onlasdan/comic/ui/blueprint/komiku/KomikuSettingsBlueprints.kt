package com.onlasdan.comic.ui.blueprint.komiku

import com.onlasdan.comic.ui.blueprint.SettingsBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.settings.SettingsBrick
import com.onlasdan.comic.ui.screens.settings.SettingsLayoutState

object KomikuSettingsBlueprints : SettingsBlueprints {
    override val blueprint: ScreenBlueprint<SettingsLayoutState, SettingsBrick> = blueprint {
        slot(ScreenSlot.Header) { place(SettingsBrick.TopBar) }
        slot(ScreenSlot.Body) {
            place(SettingsBrick.StorageSection)
            place(SettingsBrick.BackupSection)
            place(SettingsBrick.CacheSection)
        }
        slot(ScreenSlot.Overlay) {
            place(SettingsBrick.BackupDialog, predicate = { it.showBackupDialog })
            place(SettingsBrick.RestoreDialog, predicate = { it.showRestoreDialog })
            place(SettingsBrick.ImportDialog, predicate = { it.showImportDialog })
            place(SettingsBrick.ClearCacheDialog, predicate = { it.showClearCacheDialog })
        }
    }
}
