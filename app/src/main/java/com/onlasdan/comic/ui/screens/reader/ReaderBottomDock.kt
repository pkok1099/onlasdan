package com.onlasdan.comic.ui.screens.reader

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirAnimatedVisibility
import com.onlasdan.comic.ui.util.chapterSlugOf
import com.onlasdan.comic.ui.util.extractSlug
import kotlin.math.roundToInt

@Composable
fun BoxScope.ReaderOverlay(
    state: ReaderState,
    actions: ReaderActions,
    activeExpandedTab: ReaderExpandedTab?,
    onExpandTab: (ReaderExpandedTab?) -> Unit,
    sheetProgress: Float,
    comicDetails: ReaderComicDetail?,
    sourceId: String,
    slug: String,
    readVersion: Int,
    context: Context,
    autoScrollSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    autoNextDelaySeconds: Float,
    onAutoNextDelayChange: (Float) -> Unit,
    immersiveFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    keepScreenOnEnabled: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    seamlessChaptersEnabled: Boolean,
    onSeamlessChange: (Boolean) -> Unit,
    nightDimmerAlpha: Float,
    onNightDimmerChange: (Float) -> Unit,
    onClearFocus: () -> Unit,
) {
    NoirAnimatedVisibility(
        visible = state.showControls || activeExpandedTab != null,
        slideDistance = 56.dp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .zIndex(20f),
    ) {
        ReaderBottomDock(
            state = state,
            actions = actions,
            expandedTab = activeExpandedTab,
            onExpandTab = onExpandTab,
            sheetProgress = sheetProgress,
            comicDetails = comicDetails,
            sourceId = sourceId,
            slug = slug,
            readVersion = readVersion,
            context = context,
            autoScrollSpeed = autoScrollSpeed,
            onSpeedChange = onSpeedChange,
            autoNextDelaySeconds = autoNextDelaySeconds,
            onAutoNextDelayChange = onAutoNextDelayChange,
            immersiveFullscreen = immersiveFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            keepScreenOnEnabled = keepScreenOnEnabled,
            onKeepScreenOnChange = onKeepScreenOnChange,
            seamlessChaptersEnabled = seamlessChaptersEnabled,
            onSeamlessChange = onSeamlessChange,
            nightDimmerAlpha = nightDimmerAlpha,
            onNightDimmerChange = onNightDimmerChange,
            onClearFocus = onClearFocus,
        )
    }
}

@Composable
fun HoldableDockIcon(
    icon: ImageVector,
    tooltipText: String,
    contentDescription: String,
    onClick: () -> Unit,
    onShowTooltip: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TextPrimary,
    activeTooltip: String? = null,
) {
    val isShowingTooltip = activeTooltip == tooltipText

    Box(
        modifier = modifier
            .pointerInput(enabled, tooltipText) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                        onShowTooltip(null)
                    },
                    onLongPress = {
                        onShowTooltip(tooltipText)
                    },
                    onTap = {
                        onClick()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isShowingTooltip) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PureBlack.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.6f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .zIndex(25f),
            ) {
                Text(
                    text = tooltipText,
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 36.dp)
                .alpha(if (enabled) 1f else 0.3f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776")
fun ReaderBottomDock(
    state: ReaderState,
    actions: ReaderActions,
    expandedTab: ReaderExpandedTab?,
    onExpandTab: (ReaderExpandedTab?) -> Unit,
    sheetProgress: Float,
    comicDetails: ReaderComicDetail?,
    sourceId: String,
    slug: String,
    readVersion: Int,
    context: Context,
    autoScrollSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    autoNextDelaySeconds: Float,
    onAutoNextDelayChange: (Float) -> Unit,
    immersiveFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    keepScreenOnEnabled: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    seamlessChaptersEnabled: Boolean,
    onSeamlessChange: (Boolean) -> Unit,
    nightDimmerAlpha: Float,
    onNightDimmerChange: (Float) -> Unit,
    onClearFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTooltip by remember { mutableStateOf<String?>(null) }

    val chapters = state.comicDetails?.chapterList.orEmpty()
    val idx = chapters.indexOfFirst {
        val chSlug = it.id ?: extractSlug(it.url)
        chSlug == state.chapterSlug ||
            extractSlug(it.url) == state.chapterSlug ||
            it.id == state.chapterSlug ||
            it.id?.substringAfterLast(':') == state.chapterSlug
    }
    val hasPrev = idx in 0 until (chapters.size - 1)
    val hasNext = idx > 0

    Surface(
        color = NoirSurface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    PrimaryPink.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f),
                ),
            ),
        ),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = lerp(0.90f, 0.98f, sheetProgress.coerceIn(0f, 1f))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 1. M3 Handle Bar (hanya saat sheet mengembang) ──
            if (sheetProgress > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                    )
                }
            }

            // ── 2. Baris Judul & Info Chapter (SELALU DI ATAS DOCK) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (sheetProgress > 0.05f) 6.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val chapterLabel = "Ch. ${state.chapterNumber}"
                val pageLabel = if (state.pageCount > 0 && state.pageIndex > 0) " • ${state.pageIndex}/${state.pageCount}" else ""
                Text(
                    text = "$chapterLabel$pageLabel",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }

            // ── 3. Garis Pemisah (Divider) di bawah Judul ──────────
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 0.8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
            )

            // ── 4. Konten Sheet yang Mengembang dari Dock ─────────
            if (sheetProgress > 0.005f && expandedTab != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val currentHeight = (placeable.height * sheetProgress.coerceIn(0f, 1f)).roundToInt().coerceAtLeast(0)
                            layout(placeable.width, currentHeight) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .graphicsLayer {
                            alpha = (sheetProgress * 1.25f).coerceIn(0f, 1f)
                        },
                ) {
                    when (expandedTab) {
                        ReaderExpandedTab.Chapters -> ReaderChaptersSheetContent(
                            comicDetails = comicDetails,
                            sourceId = sourceId,
                            slug = slug,
                            chapterSlug = state.chapterSlug,
                            readVersion = readVersion,
                            context = context,
                            onNavigate = { nextSlug ->
                                actions.onNavigateChapter(nextSlug)
                                onExpandTab(null)
                            },
                        )

                        ReaderExpandedTab.Settings -> ReaderSettingsSheetContent(
                            speed = autoScrollSpeed,
                            onSpeedChange = onSpeedChange,
                            autoNextEnabled = state.autoNextEnabled,
                            onAutoNextChange = { actions.onToggleAutoNext() },
                            autoNextDelaySeconds = autoNextDelaySeconds,
                            onAutoNextDelayChange = onAutoNextDelayChange,
                            immersiveFullscreen = immersiveFullscreen,
                            onToggleFullscreen = onToggleFullscreen,
                            keepScreenOnEnabled = keepScreenOnEnabled,
                            onKeepScreenOnChange = onKeepScreenOnChange,
                            seamlessChaptersEnabled = seamlessChaptersEnabled,
                            onSeamlessChange = onSeamlessChange,
                            nightDimmerAlpha = nightDimmerAlpha,
                            onNightDimmerChange = onNightDimmerChange,
                            onClearFocus = onClearFocus,
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 0.8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                }
            }

            // ── 5. Baris 6 Icon Tombol Dock di Bawah ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Prev
                HoldableDockIcon(
                    icon = Icons.Default.ChevronLeft,
                    tooltipText = stringResource(R.string.reader_tooltip_prev_chapter),
                    contentDescription = stringResource(R.string.reader_cd_prev_chapter),
                    enabled = hasPrev,
                    onClick = {
                        if (hasPrev) {
                            val target = chapters[idx + 1]
                            actions.onNavigateChapter(chapterSlugOf(target.id ?: extractSlug(target.url)))
                        }
                    },
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                )

                // 2. Settings (dengan highlight jika aktif)
                val isSettingsActive = expandedTab == ReaderExpandedTab.Settings && sheetProgress > 0.05f
                HoldableDockIcon(
                    icon = Icons.Default.Settings,
                    tooltipText = stringResource(R.string.reader_tooltip_settings),
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = if (isSettingsActive) PrimaryPink else TextPrimary,
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                    onClick = {
                        onExpandTab(if (expandedTab == ReaderExpandedTab.Settings) null else ReaderExpandedTab.Settings)
                    },
                )

                // 3. Chapters (dengan highlight jika aktif)
                val isChaptersActive = expandedTab == ReaderExpandedTab.Chapters && sheetProgress > 0.05f
                HoldableDockIcon(
                    icon = Icons.AutoMirrored.Filled.List,
                    tooltipText = stringResource(R.string.reader_tooltip_chapters),
                    contentDescription = stringResource(R.string.cd_chapters),
                    tint = if (isChaptersActive) PrimaryPink else TextPrimary,
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                    onClick = {
                        onExpandTab(if (expandedTab == ReaderExpandedTab.Chapters) null else ReaderExpandedTab.Chapters)
                    },
                )

                // 4. Auto Scroll
                HoldableDockIcon(
                    icon = if (state.autoScrollActive) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    tooltipText = stringResource(R.string.reader_tooltip_auto_scroll),
                    contentDescription = stringResource(R.string.reader_cd_auto_scroll),
                    tint = if (state.autoScrollActive) PrimaryPink else TextPrimary,
                    onClick = actions.onAutoScroll,
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                )

                // 5. Auto Next
                val isAutoNextLocked = seamlessChaptersEnabled
                HoldableDockIcon(
                    icon = Icons.Default.SkipNext,
                    tooltipText = if (isAutoNextLocked) {
                        stringResource(R.string.reader_tooltip_auto_next_locked)
                    } else {
                        stringResource(R.string.reader_tooltip_auto_next)
                    },
                    contentDescription = stringResource(R.string.reader_cd_auto_next),
                    tint = if (state.autoNextEnabled || isAutoNextLocked) PrimaryPink else TextPrimary,
                    onClick = {
                        if (!isAutoNextLocked) {
                            actions.onToggleAutoNext()
                        } else {
                            activeTooltip = context.getString(R.string.reader_tooltip_auto_next_locked)
                        }
                    },
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                )

                // 6. Next
                HoldableDockIcon(
                    icon = Icons.Default.ChevronRight,
                    tooltipText = stringResource(R.string.reader_tooltip_next_chapter),
                    contentDescription = stringResource(R.string.reader_cd_next_chapter),
                    enabled = hasNext,
                    onClick = {
                        if (hasNext) {
                            val target = chapters[idx - 1]
                            actions.onNavigateChapter(chapterSlugOf(target.id ?: extractSlug(target.url)))
                        }
                    },
                    activeTooltip = activeTooltip,
                    onShowTooltip = { activeTooltip = it },
                )
            }
        }
    }
}
