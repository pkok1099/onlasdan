package com.onlasdan.comic.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlasdan.comic.BuildConfig
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.components.AppToast
import com.onlasdan.comic.ui.components.SimpleTopBar
import com.onlasdan.comic.ui.screens.settings.SettingsBrick
import com.onlasdan.comic.ui.screens.settings.SettingsLayoutState
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.AnimationMode
import com.onlasdan.comic.ui.util.AppCacheStore
import com.onlasdan.comic.ui.util.DatabaseBackupStore
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.UiPreferences
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.bindAutoHideOnScroll
import com.onlasdan.comic.ui.util.collapsibleHeader
import com.onlasdan.comic.ui.util.rememberNoirSpec
import com.onlasdan.comic.ui.util.rememberSelectedSource
import com.onlasdan.comic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
@Suppress("kotlin:S3776")
fun SettingsScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cacheSuccessTemplate = stringResource(R.string.settings_dialog_cache_success)
    val source = rememberSelectedSource()
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.settings?.blueprint }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = onBack, onChooseSource = onBack)
        return
    }
    val msgToDefault = stringResource(R.string.settings_storage_to_default)
    val msgBackupSuccess = stringResource(R.string.settings_dialog_backup_success)
    val msgBackupFailPrefix = stringResource(R.string.settings_dialog_backup_failed_prefix)
    val msgRestoreSuccess = stringResource(R.string.settings_dialog_restore_success)
    val msgRestoreFailPrefix = stringResource(R.string.settings_dialog_restore_failed_prefix)
    val msgImportSuccess = stringResource(R.string.settings_dialog_import_success)
    val msgImportFailPrefix = stringResource(R.string.settings_dialog_import_failed_prefix)
    val msgCacheFailPrefix = stringResource(R.string.settings_dialog_cache_failed_prefix)
    val scope = rememberCoroutineScope()
    var storageUri by remember { mutableStateOf(DatabaseBackupStore.getCustomTreeUri(context)) }
    var importDbUri by remember { mutableStateOf<Uri?>(null) }
    var showBackupInfo by remember { mutableStateOf(false) }
    var showRestoreInfo by remember { mutableStateOf(false) }
    var showImportInfo by remember { mutableStateOf(false) }
    var showClearCacheInfo by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }
    var isClearingCache by remember { mutableStateOf(false) }

    fun refreshCacheSize() {
        scope.launch {
            cacheSizeBytes = AppCacheStore.getCacheSizeBytes(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    val cacheFormatted = cacheSizeBytes?.let { AppCacheStore.formatSize(it) }
        ?: stringResource(R.string.settings_cache_calculating)
    val listState = rememberLazyListState()
    val headerVisible = rememberSaveable { mutableStateOf(true) }
    bindAutoHideOnScroll(listState = listState, headerVisible = headerVisible)

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            DatabaseBackupStore.setCustomTreeUri(context, uri)
            DatabaseBackupStore.setBackupEnabled(context, true)
            storageUri = uri
            scope.launch {
                DatabaseBackupStore.exportDb(context)
                    .onSuccess { AppToast.success("Backup database disimpan.") }
                    .onFailure { AppToast.error("Gagal backup database: ${it.message}") }
            }
        }
    }

    val pickDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        importDbUri = uri
        showImportInfo = uri != null
    }

    Scaffold(
        topBar = {
            val headerProgress by animateNoirFloatAsState(
                targetValue = if (headerVisible.value) 1f else 0f,
                animationSpec = rememberNoirSpec(NoirMotion.spatialSpring()),
                label = "settingsHeaderProgress",
            )
            val headerAlpha by animateNoirFloatAsState(
                targetValue = if (headerVisible.value) 1f else 0f,
                animationSpec = rememberNoirSpec(NoirMotion.effectsSpring()),
                label = "settingsHeaderAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .collapsibleHeader(
                        progress = { headerProgress },
                        alpha = { headerAlpha },
                    ),
            ) {
                SimpleTopBar(stringResource(R.string.screen_settings), onBack)
            }
        },
    ) { innerPadding ->
        val state = remember(storageUri, showBackupInfo, showRestoreInfo, showImportInfo, showClearCacheInfo) {
            SettingsLayoutState(
                hasStorageUri = storageUri != null,
                showBackupDialog = showBackupInfo,
                showRestoreDialog = showRestoreInfo,
                showImportDialog = showImportInfo,
                showClearCacheDialog = showClearCacheInfo,
            )
        }
        val headerPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Header].orEmpty() }
        val bodyPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Body].orEmpty() }
        val overlayPlacements = remember(blueprint) { blueprint.slots[ScreenSlot.Overlay].orEmpty() }
        val enabled = remember(bodyPlacements, state) {
            bodyPlacements.filter { it.predicate(state) }.map { it.brick }.toSet()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Source default — always visible, driven by SourceRegistry (no source-name branching)
            item {
                val defaultSourceId by mainViewModel.defaultSource.collectAsStateWithLifecycle()
                SettingSection(title = stringResource(R.string.settings_source_default_title)) {
                    Text(
                        text = stringResource(R.string.settings_source_default_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    SourceRegistry.sources.forEach { src ->
                        val selected = src.id.equals(defaultSourceId, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ChipShape)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { mainViewModel.setDefaultSource(src.id) },
                                )
                                .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryPink,
                                    unselectedColor = TextSecondary,
                                ),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = src.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) TextPrimary else TextSecondary,
                            )
                        }
                    }
                }
            }

            // Animasi UI — in-app toggle (MOTION.md §3). Default Aktif:
            // animasi tetap hidup meski system animator scale 0x.
            item {
                val animationMode by UiPreferences.animationMode(context)
                    .collectAsStateWithLifecycle(initialValue = AnimationMode.FULL)
                SettingSection(title = stringResource(R.string.settings_animation_title)) {
                    Text(
                        text = stringResource(R.string.settings_animation_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        AnimationMode.FULL to stringResource(R.string.settings_animation_full),
                        AnimationMode.MINIMAL to stringResource(R.string.settings_animation_minimal),
                    ).forEach { (mode, label) ->
                        val selected = mode == animationMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ChipShape)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        scope.launch { UiPreferences.setAnimationMode(context, mode) }
                                    },
                                )
                                .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryPink,
                                    unselectedColor = TextSecondary,
                                ),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) TextPrimary else TextSecondary,
                            )
                        }
                    }
                }
            }

            if (enabled.contains(SettingsBrick.StorageSection)) {
                item {
                    SettingSection(title = stringResource(R.string.settings_storage_title)) {
                        Text(
                            text = storageUri?.lastPathSegment ?: stringResource(R.string.settings_storage_path_default),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_storage_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { pickFolderLauncher.launch(null) }) {
                                Text(stringResource(R.string.settings_storage_pick_folder))
                            }
                            OutlinedButton(
                                onClick = {
                                    DatabaseBackupStore.setCustomTreeUri(context, null)
                                    storageUri = null
                                    AppToast.show(msgToDefault)
                                },
                            ) {
                                Text(stringResource(R.string.settings_storage_default))
                            }
                        }
                    }
                }
            }

            if (enabled.contains(SettingsBrick.BackupSection)) {
                item {
                    SettingSection(title = stringResource(R.string.settings_backup_title)) {
                        Text(
                            text = stringResource(R.string.settings_backup_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showBackupInfo = true }) {
                                Text(stringResource(R.string.settings_backup_now))
                            }
                            OutlinedButton(onClick = { showRestoreInfo = true }) {
                                Text(stringResource(R.string.settings_restore))
                            }
                            OutlinedButton(onClick = { pickDbLauncher.launch(arrayOf("*/*")) }) {
                                Text(stringResource(R.string.settings_import_db))
                            }
                        }
                    }
                }
            }

            if (enabled.contains(SettingsBrick.CacheSection)) {
                item {
                    SettingSection(title = stringResource(R.string.settings_cache_title)) {
                        Text(
                            text = stringResource(R.string.settings_cache_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cache_size_label, cacheFormatted),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Button(
                                onClick = { showClearCacheInfo = true },
                                enabled = !isClearingCache,
                            ) {
                                Text(
                                    if ((cacheSizeBytes ?: 0L) > 0L) {
                                        stringResource(R.string.settings_cache_clear_with_size, cacheFormatted)
                                    } else {
                                        stringResource(R.string.settings_cache_clear)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Footer versi aplikasi — penanda build yang terinstal.
            // Single source of truth: BuildConfig (dari gradle.properties).
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        overlayPlacements.forEach { placement ->
            if (!placement.predicate(state)) return@forEach
            when (placement.brick) {
                SettingsBrick.BackupDialog -> ConfirmDialog(
                    title = stringResource(R.string.settings_dialog_backup_title),
                    body = stringResource(R.string.settings_dialog_backup_body),
                    confirmLabel = stringResource(R.string.settings_dialog_backup_confirm),
                    onConfirm = {
                        showBackupInfo = false
                        scope.launch {
                            DatabaseBackupStore.exportDb(context)
                                .onSuccess { AppToast.success(msgBackupSuccess) }
                                .onFailure { AppToast.error("$msgBackupFailPrefix ${it.message ?: ""}") }
                        }
                    },
                    onDismiss = { showBackupInfo = false },
                )

                SettingsBrick.RestoreDialog -> ConfirmDialog(
                    title = stringResource(R.string.settings_dialog_restore_title),
                    body = stringResource(R.string.settings_dialog_restore_body),
                    confirmLabel = stringResource(R.string.settings_dialog_restore_confirm),
                    onConfirm = {
                        showRestoreInfo = false
                        scope.launch {
                            DatabaseBackupStore.restoreDb(context)
                                .onSuccess { AppToast.success(msgRestoreSuccess) }
                                .onFailure { AppToast.error("$msgRestoreFailPrefix ${it.message ?: ""}") }
                        }
                    },
                    onDismiss = { showRestoreInfo = false },
                )

                SettingsBrick.ImportDialog -> ConfirmDialog(
                    title = stringResource(R.string.settings_dialog_import_title),
                    body = stringResource(R.string.settings_dialog_import_body),
                    confirmLabel = stringResource(R.string.settings_dialog_import_confirm),
                    onConfirm = {
                        val uri = importDbUri
                        showImportInfo = false
                        importDbUri = null
                        if (uri == null) return@ConfirmDialog
                        runCatching {
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(uri, flags)
                        }
                        scope.launch {
                            DatabaseBackupStore.restoreFromUri(context, uri)
                                .onSuccess { AppToast.success(msgImportSuccess) }
                                .onFailure { AppToast.error("$msgImportFailPrefix ${it.message ?: ""}") }
                        }
                    },
                    onDismiss = {
                        showImportInfo = false
                        importDbUri = null
                    },
                )

                SettingsBrick.ClearCacheDialog -> ConfirmDialog(
                    title = stringResource(R.string.settings_dialog_cache_title),
                    body = stringResource(R.string.settings_dialog_cache_body, cacheFormatted),
                    confirmLabel = stringResource(R.string.settings_dialog_cache_confirm),
                    onConfirm = {
                        showClearCacheInfo = false
                        isClearingCache = true
                        scope.launch {
                            AppCacheStore.clearCache(context)
                                .onSuccess { freed ->
                                    isClearingCache = false
                                    val freedFormatted = AppCacheStore.formatSize(freed)
                                    AppToast.success(cacheSuccessTemplate.format(freedFormatted))
                                    refreshCacheSize()
                                }
                                .onFailure { error ->
                                    isClearingCache = false
                                    AppToast.error("$msgCacheFailPrefix ${error.message ?: ""}")
                                    refreshCacheSize()
                                }
                        }
                    },
                    onDismiss = { showClearCacheInfo = false },
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
