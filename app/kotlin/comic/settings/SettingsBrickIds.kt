package comic.settings

/**
 * String-based brick identifiers for SettingsScreen.
 */
object SettingsBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val STORAGE_SECTION = "komiku:storage_section"
    const val BACKUP_SECTION = "komiku:backup_section"
    const val CACHE_SECTION = "komiku:cache_section"
    const val BACKUP_DIALOG = "komiku:backup_dialog"
    const val RESTORE_DIALOG = "komiku:restore_dialog"
    const val IMPORT_DIALOG = "komiku:import_dialog"
    const val CLEAR_CACHE_DIALOG = "komiku:clear_cache_dialog"
}

/**
 * Extension to map SettingsBrick enum to string IDs for backward compatibility.
 */
val SettingsBrick.id: String
    get() = when (this) {
        SettingsBrick.TopBar -> SettingsBrickIds.TOP_BAR
        SettingsBrick.StorageSection -> SettingsBrickIds.STORAGE_SECTION
        SettingsBrick.BackupSection -> SettingsBrickIds.BACKUP_SECTION
        SettingsBrick.CacheSection -> SettingsBrickIds.CACHE_SECTION
        SettingsBrick.BackupDialog -> SettingsBrickIds.BACKUP_DIALOG
        SettingsBrick.RestoreDialog -> SettingsBrickIds.RESTORE_DIALOG
        SettingsBrick.ImportDialog -> SettingsBrickIds.IMPORT_DIALOG
        SettingsBrick.ClearCacheDialog -> SettingsBrickIds.CLEAR_CACHE_DIALOG
    }
