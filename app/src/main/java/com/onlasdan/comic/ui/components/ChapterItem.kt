package com.onlasdan.comic.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.download.ChapterDownloadManager
import com.onlasdan.comic.ui.source.AppDateFormatter
import com.onlasdan.comic.ui.source.Chapter
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.noirClickable
import com.onlasdan.comic.ui.util.noirCombinedClickable

@Composable
fun ChapterRowItem(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRead: Boolean = chapter.isRead,
    downloadStatus: Int? = null,
    downloadProgress: Float? = null,
    onDownloadClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDownloadPdf: ((Chapter) -> Unit)? = null,
) {
    val titleColor = if (isRead) TextSecondary else TextPrimary
    val checkmarkTint = AccentPink
    val rowContext = LocalContext.current
    // Unduhan pertama meminta izin notifikasi (hasil unduhan tampil sebagai
    // notifikasi sejak backlog #1); unduhan tetap jalan apa pun jawabannya.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ensureNotificationPermission = remember(rowContext) {
        {
            if (androidx.core.content.ContextCompat.checkSelfPermission(rowContext, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .noirCombinedClickable(
                pressedScale = 0.96f,
                onLongClick = onLongClick,
                onClick = {
                    if (selectionMode && onToggleSelect != null) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .padding(end = Spacing.md)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryPink else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) PrimaryPink else DarkOutline,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (isRead) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.detail_chapter_read),
                            tint = checkmarkTint,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = titleColor,
                        maxLines = 1,
                    )
                }

                if (!chapter.lastUpdated.isNullOrBlank()) {
                    Text(
                        text = AppDateFormatter.format(chapter.lastUpdated),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (!chapter.downloadUrl.isNullOrBlank() && onDownloadPdf != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(StrokeWidth.regular, PrimaryPink.copy(alpha = 0.6f)),
                        modifier = Modifier.noirClickable(pressedScale = 0.88f) { onDownloadPdf(chapter) },
                    ) {
                        Text(
                            text = stringResource(R.string.detail_download_pdf),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryPink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (!selectionMode && onDownloadClick != null) {
                    when (downloadStatus) {
                        ChapterDownloadManager.STATUS_COMPLETED -> {
                            IconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.detail_download_success),
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        ChapterDownloadManager.STATUS_DOWNLOADING -> {
                            // Ketuk spinner untuk membatalkan (backlog #1):
                            // onDownloadClick yang sama — pemanggil membedakan
                            // dari statusnya (COMPLETED = hapus, aktif = batal).
                            IconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress ?: 0f },
                                        color = PrimaryPink,
                                        trackColor = DarkOutline,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.detail_cancel_download),
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        ChapterDownloadManager.STATUS_QUEUED -> {
                            IconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = AccentPink,
                                        trackColor = DarkOutline,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.detail_cancel_download),
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        ChapterDownloadManager.STATUS_FAILED -> {
                            IconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = stringResource(R.string.detail_download_failed),
                                    tint = AccentPink,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        else -> {
                            IconButton(
                                onClick = {
                                    ensureNotificationPermission()
                                    onDownloadClick()
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = stringResource(R.string.detail_download_chapter),
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                } else if (!selectionMode) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isRead) TextTertiary else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = StrokeWidth.hairline,
            color = DarkOutline.copy(alpha = 0.5f),
        )
    }
}

/**
 * 3-column compact chapter grid card for modern manga/manhwa UI (e.g. Shinigami).
 */
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRead: Boolean = chapter.isRead,
    isNewest: Boolean = false,
    downloadStatus: Int? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val borderColor = when {
        isSelected -> PrimaryPink
        isRead -> DarkOutline.copy(alpha = 0.5f)
        else -> DarkOutline
    }
    val backgroundColor = when {
        isSelected -> PrimaryPink.copy(alpha = 0.15f)
        isRead -> DarkSurface.copy(alpha = 0.6f)
        else -> DarkSurfaceElevated
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) StrokeWidth.regular else StrokeWidth.hairline, borderColor),
        modifier = modifier
            .noirCombinedClickable(
                pressedScale = 0.94f,
                onLongClick = onLongClick,
                onClick = {
                    if (selectionMode && onToggleSelect != null) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Chapter Title / Number
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold,
                    ),
                    color = if (isRead) TextSecondary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                // Date
                val dateText = if (!chapter.lastUpdated.isNullOrBlank()) {
                    AppDateFormatter.format(chapter.lastUpdated)
                } else {
                    ""
                }
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Badges
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryPink else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) PrimaryPink else DarkOutline,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            } else if (isNewest) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 6.dp, bottomStart = 4.dp),
                    color = PrimaryPink,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = "UP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            } else if (downloadStatus == ChapterDownloadManager.STATUS_COMPLETED) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp),
                )
            }
        }
    }
}
