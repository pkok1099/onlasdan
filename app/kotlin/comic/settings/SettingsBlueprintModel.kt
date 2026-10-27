package comic.settings

data class SettingsLayoutState(
    val hasStorageUri: Boolean,
    val showBackupDialog: Boolean,
    val showRestoreDialog: Boolean,
    val showImportDialog: Boolean,
    val showClearCacheDialog: Boolean,
)

enum class SettingsBrick {
    TopBar,
    StorageSection,
    BackupSection,
    CacheSection,
    BackupDialog,
    RestoreDialog,
    ImportDialog,
    ClearCacheDialog,
}
