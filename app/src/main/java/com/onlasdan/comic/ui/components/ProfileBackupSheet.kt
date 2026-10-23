package com.onlasdan.comic.ui.components

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.chrome.ChromeContext
import com.onlasdan.comic.ui.download.ChapterDownloadManager
import com.onlasdan.comic.ui.theme.DarkOutline
import com.onlasdan.comic.ui.theme.DarkOutlineVariant
import com.onlasdan.comic.ui.theme.DarkSurface
import com.onlasdan.comic.ui.theme.FieldSurface
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary
import com.onlasdan.comic.ui.util.DatabaseBackupStore
import com.onlasdan.comic.ui.util.UiPreferences
import com.onlasdan.comic.ui.util.noirClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sheet Profile & Backup (\"Noir & Blush\" — Pure Physics).
 * Mekar ke atas berpusat dari koordinat ikon Profile (paling kanan Pill Dock).
 */
@Composable
fun ProfileBackupSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorXFraction: Float = 0.88f,
    chromeContext: ChromeContext? = null,
    onNavigateBookmarks: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    NoirMorphingSheet(
        visible = visible,
        onDismiss = onDismiss,
        anchorXFraction = anchorXFraction,
        maxHeight = 480.dp,
    ) {
        ProfileBackupSheetContent(
            chromeContext = chromeContext,
            onDismiss = onDismiss,
            onNavigateBookmarks = onNavigateBookmarks,
            onNavigateHistory = onNavigateHistory,
            onNavigateProfile = onNavigateProfile,
            onNavigateSettings = onNavigateSettings,
        )
    }
}

/**
 * Content-only variant of ProfileBackupSheet for use inside UnifiedMorphingDock.
 * Contains the scrollable profile & backup UI without any sheet/morphing wrapper.
 */
@Composable
fun ProfileBackupSheetContent(
    chromeContext: ChromeContext? = null,
    onDismiss: () -> Unit = {},
    onNavigateBookmarks: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val downloadsDb = remember { ComicDatabase.get(context) }
    val allDownloads by downloadsDb.downloadDao().observeAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    val downloadsSeenAt by UiPreferences.downloadsSeenAt(context).collectAsStateWithLifecycle(initialValue = 0L)
    val unreadDownloads = allDownloads.count { it.status == ChapterDownloadManager.STATUS_COMPLETED && it.downloadedAt > downloadsSeenAt }
    val backupSuccessText = stringResource(R.string.storage_backup_success)
    val backupFailedText = stringResource(R.string.storage_backup_failed)
    val restoreSuccessText = stringResource(R.string.storage_restore_success)
    val restoreFailedText = stringResource(R.string.storage_restore_failed)
    val scrollState = rememberScrollState()
    val view = LocalView.current
    // Scope yang selamat dari penutupan sheet: operasi DB krusial (backup/
    // restore) tidak boleh batal di tengah hanya karena sheet tertutup.
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    // Operasi penyimpanan yang sedang berjalan ("" = diam). Selama terisi:
    // tombol penyimpanan nonaktif, navigasi ditahan, back ditahan (Fase A UX).
    var busyAction by remember { mutableStateOf("") }
    BackHandler(enabled = busyAction.isNotEmpty()) {}

    fun runStoreOp(action: String, op: suspend () -> Result<*>, failText: String, onSuccess: () -> Unit) {
        if (busyAction.isNotEmpty()) return
        busyAction = action
        appScope.launch(Dispatchers.IO) {
            val result = op()
            withContext(Dispatchers.Main) {
                busyAction = ""
                if (result.isSuccess) onSuccess() else AppToast.error(failText)
            }
        }
    }

    var currentStorageUri by remember { mutableStateOf(DatabaseBackupStore.getCustomTreeUri(context)) }
    var lastBackupTime by remember { mutableLongStateOf(DatabaseBackupStore.getLastBackupTime(context)) }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            DatabaseBackupStore.setCustomTreeUri(context, uri)
            DatabaseBackupStore.setBackupEnabled(context, true)
            currentStorageUri = uri
            runStoreOp("backup", { DatabaseBackupStore.exportDb(context) }, backupFailedText) {
                lastBackupTime = DatabaseBackupStore.getLastBackupTime(context)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                AppToast.success(backupSuccessText)
            }
        }
    }
    val storageLabel = currentStorageUri?.lastPathSegment ?: "Documents/ComicReader"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.md),
    ) {
        // Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(PrimaryPink, PrimaryPink.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.chrome_profile_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = stringResource(R.string.chrome_profile_session_local),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Contextual Controls (HD & Kolom jika tersedia)
        if (chromeContext?.toggleHd != null || chromeContext?.setColumns != null) {
            Surface(
                color = FieldSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    SheetSectionTitle(stringResource(R.string.chrome_section_context))

                    Spacer(Modifier.height(Spacing.xs))

                    if (chromeContext.toggleHd != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.chrome_context_toggle_hd),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Switch(
                                checked = chromeContext.isHd == true,
                                onCheckedChange = { chromeContext.toggleHd.invoke() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryPink,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurface,
                                ),
                            )
                        }
                    }

                    if (chromeContext.setColumns != null && chromeContext.columns != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ViewColumn,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.chrome_context_columns),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3, 4).forEach { col ->
                                    val isSelected = chromeContext.columns == col
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryPink else DarkSurface)
                                            .clickable { chromeContext.setColumns.invoke(col) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = col.toString(),
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // Quick Nav Links Card
        Surface(
            color = FieldSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.sm)) {
                SheetSectionTitle(stringResource(R.string.chrome_section_nav), padded = true)

                ProfileNavItem(
                    icon = Icons.Default.Bookmark,
                    title = stringResource(R.string.chrome_menu_bookmarks),
                    onClick = {
                        if (busyAction.isEmpty()) {
                            onDismiss()
                            onNavigateBookmarks()
                        }
                    },
                )
                ProfileNavItem(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.chrome_menu_history),
                    onClick = {
                        if (busyAction.isEmpty()) {
                            onDismiss()
                            onNavigateHistory()
                        }
                    },
                )
                chromeContext?.openDownloads?.let { openDownloads ->
                    Box {
                        ProfileNavItem(
                            icon = Icons.Default.CloudDownload,
                            title = stringResource(R.string.chrome_menu_downloads),
                            onClick = {
                                if (busyAction.isEmpty()) {
                                    onDismiss()
                                    openDownloads()
                                }
                            },
                        )
                        if (unreadDownloads > 0) {
                            Text(
                                text = unreadDownloads.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 36.dp)
                                    .background(PrimaryPink, CircleShape)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                ProfileNavItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.chrome_menu_profile_backup),
                    onClick = {
                        if (busyAction.isEmpty()) {
                            onDismiss()
                            onNavigateProfile()
                        }
                    },
                )
                ProfileNavItem(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.screen_settings),
                    onClick = {
                        if (busyAction.isEmpty()) {
                            onDismiss()
                            onNavigateSettings()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // Database Backup & Restore Card
        Surface(
            color = FieldSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "BACKUP & RESTORE DATABASE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPink,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = storageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                }

                Spacer(Modifier.height(Spacing.xs))

                val lastBackupText = if (lastBackupTime > 0L) {
                    val formatted = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(lastBackupTime))
                    stringResource(R.string.storage_backup_last_time, formatted)
                } else {
                    stringResource(R.string.storage_backup_never)
                }
                Text(text = lastBackupText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                Spacer(Modifier.height(Spacing.md))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // Export Backup Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .noirClickable(pressedScale = 0.94f, enabled = busyAction.isEmpty()) {
                                if (currentStorageUri == null) {
                                    pickFolderLauncher.launch(null)
                                } else {
                                    runStoreOp("backup", { DatabaseBackupStore.exportDb(context) }, backupFailedText) {
                                        lastBackupTime = DatabaseBackupStore.getLastBackupTime(context)
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        AppToast.success(backupSuccessText)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPink.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.40f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BackupButtonContent(Icons.Default.CloudUpload, stringResource(R.string.storage_btn_backup_now), PrimaryPink, PrimaryPink, FontWeight.SemiBold, loading = busyAction == "backup")
                        }
                    }

                    // Restore Backup Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .noirClickable(pressedScale = 0.94f, enabled = busyAction.isEmpty()) {
                                runStoreOp("restore", { DatabaseBackupStore.restoreDb(context) }, restoreFailedText) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    AppToast.success(restoreSuccessText)
                                    onDismiss()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BackupButtonContent(Icons.Default.CloudDownload, stringResource(R.string.storage_restore_confirm), TextSecondary, TextPrimary, FontWeight.Medium, loading = busyAction == "restore")
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // Change Folder Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .noirClickable(pressedScale = 0.96f, enabled = busyAction.isEmpty()) {
                            pickFolderLauncher.launch(null)
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.storage_btn_change_folder),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun ProfileNavItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp),
        )
    }
}
