package comic.blueprint.shinigami

import comic.blueprint.SettingsBlueprints
import comic.blueprint.core.ScreenBlueprint
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.settings.SettingsBrick
import comic.settings.SettingsLayoutState

object ShinigamiSettingsBlueprints : SettingsBlueprints {
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
