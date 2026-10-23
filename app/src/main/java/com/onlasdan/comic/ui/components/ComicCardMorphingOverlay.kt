package com.onlasdan.comic.ui.components

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.BookmarkEntity
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.DbChangeNotifier
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Target data untuk aksi interaksi tekan tahan pada Card komik.
 */
data class CardHoldTarget(
    val slug: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val originBounds: Rect = Rect.Zero,
    val columnIndex: Int = 0,
)

/**
 * Overlay ekspansi dan perubahan bentuk (in-place morphing) saat menahan Card komik,
 * diadopsi dari card.html dengan 100% fisika pegas Noir (NoirMotion):
 * - Kolom 0 & 1: melebar ke kanan (cover di kiri, menu di kanan).
 * - Kolom 2: melebar ke kiri (cover di kanan, menu di kiri).
 * - Menutup: menciut kembali presisi ke koordinat thumbnail asal (reverse morph).
 */
@Composable
fun ComicCardMorphingOverlay(
    target: CardHoldTarget?,
    onDismiss: () -> Unit,
    onLaunchReader: (slug: String, chapterSlug: String, sourceId: String) -> Unit,
    onOpenDetail: (slug: String, sourceId: String) -> Unit,
) {
    var activeTarget by remember { mutableStateOf<CardHoldTarget?>(null) }
    val morphProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val db = remember { ComicDatabase.get(context) }

    var isBookmarked by remember { mutableStateOf(false) }
    var lastReadChapterSlug by remember { mutableStateOf<String?>(null) }
    var lastReadChapterTitle by remember { mutableStateOf<String?>(null) }
    var firstChapterSlug by remember { mutableStateOf<String?>(null) }

    // Buka dan tutup morphing dengan pegas Noir berbobot
    LaunchedEffect(target) {
        if (target != null) {
            val effectiveSrc = if (target.sourceId.isNotBlank()) target.sourceId else SourceRegistry.defaultSource.id
            activeTarget = target.copy(sourceId = effectiveSrc)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            withContext(Dispatchers.IO) {
                val bookmarkedCount = db.bookmarkDao().exists(target.slug, effectiveSrc)
                isBookmarked = bookmarkedCount > 0

                val hist = db.historyDao().getHistory(target.slug, effectiveSrc)
                    ?: if (effectiveSrc.isNotBlank()) db.historyDao().getHistory(target.slug, "") else null
                lastReadChapterSlug = hist?.chapterSlug
                lastReadChapterTitle = hist?.chapterTitle
            }

            withContext(NoirForceMotionScale) {
                morphProgress.animateTo(1f, NoirMotion.pillThrowSpring())
            }
        }
    }

    val currentItem = activeTarget ?: return
    val p = morphProgress.value
    if (p <= 0.001f && target == null) return

    var isClosing by remember { mutableStateOf(false) }

    val animateDismiss: (afterDismiss: (() -> Unit)?) -> Unit = { action ->
        if (!isClosing) {
            isClosing = true
            scope.launch {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                withContext(NoirForceMotionScale) {
                    morphProgress.animateTo(0f, NoirMotion.pillDropSpring())
                }
                onDismiss()
                activeTarget = null
                isClosing = false
                action?.invoke()
            }
        }
    }

    BackHandler(enabled = p > 0.05f) {
        animateDismiss(null)
    }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }

    val origin = currentItem.originBounds
    val (startLeft, startTop, startWidth, startHeight) = if (origin == Rect.Zero || origin.isEmpty) {
        val (w, h) = 120.dp to 180.dp
        listOf((screenWidth - w) / 2, (screenHeight - h) / 2, w, h)
    } else {
        with(density) {
            listOf(origin.left.toDp(), origin.top.toDp(), origin.width.toDp(), origin.height.toDp())
        }
    }

    val expandedWidth = (startWidth * 2.18f).coerceAtMost(screenWidth - 24.dp)
    val expandedHeight = (startHeight + 16.dp).coerceAtLeast(196.dp)

    val isExpandRight = currentItem.columnIndex == 0 || currentItem.columnIndex == 1
    val destLeft = if (isExpandRight) {
        val maxRight = screenWidth - 12.dp
        if (startLeft + expandedWidth > maxRight) maxRight - expandedWidth else startLeft
    } else {
        val calcLeft = startLeft + startWidth - expandedWidth
        if (calcLeft < 12.dp) 12.dp else calcLeft
    }
    val destTopMax = (screenHeight - expandedHeight - 64.dp).coerceAtLeast(48.dp)
    val destTop = (startTop - 8.dp).coerceIn(48.dp, destTopMax)

    val curLeft = lerp(startLeft, destLeft, p)
    val curTop = lerp(startTop, destTop, p)
    val curWidth = lerp(startWidth, expandedWidth, p)
    val curHeight = lerp(startHeight, expandedHeight, p)

    val handleBookmarkToggle: () -> Unit = {
        scope.launch {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            withContext(Dispatchers.IO) {
                if (isBookmarked) {
                    db.bookmarkDao().delete(currentItem.slug, currentItem.sourceId)
                } else {
                    db.bookmarkDao().upsert(
                        BookmarkEntity(
                            comicSlug = currentItem.slug,
                            source = currentItem.sourceId,
                            title = currentItem.title,
                            thumbnail = currentItem.coverUrl,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
            isBookmarked = !isBookmarked
            DbChangeNotifier.onDbChanged(context)
            AppToast.show(
                text = if (isBookmarked) "Disimpan ke Bookmark" else "Bookmark dihapus",
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            )
        }
    }

    val handleReadAction: (firstChapterOnly: Boolean) -> Unit = { firstChapterOnly ->
        animateDismiss {
            scope.launch {
                val targetSlug = if (!firstChapterOnly && !lastReadChapterSlug.isNullOrBlank()) {
                    lastReadChapterSlug
                } else {
                    firstChapterSlug ?: withContext(Dispatchers.IO) {
                        try {
                            val src = SourceRegistry.get(currentItem.sourceId)
                            val chs = src.chapters(currentItem.slug)
                            val ch = if (firstChapterOnly) (chs.lastOrNull() ?: chs.firstOrNull()) else chs.firstOrNull()
                            ch?.id.orEmpty().substringAfterLast(':')
                        } catch (_: Exception) {
                            ""
                        }
                    }
                }
                if (!targetSlug.isNullOrBlank()) {
                    onLaunchReader(currentItem.slug, targetSlug, currentItem.sourceId)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
    ) {
        // Backdrop overlay gelap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.75f * p.coerceIn(0f, 1f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateDismiss(null) },
                ),
        )

        val containerPadding = lerp(0.dp, 8.dp, p)
        val containerRadius = lerp(12.dp, 18.dp, p)

        // Card Container yang ber-morphing
        Surface(
            shape = RoundedCornerShape(containerRadius),
            color = if (p > 0.15f) OverlaySurface else DarkSurfaceElevated,
            border = BorderStroke(
                1.2.dp,
                if (p > 0.15f) PrimaryPink.copy(alpha = 0.45f * p.coerceIn(0f, 1f)) else DarkOutline,
            ),
            shadowElevation = (16.dp * p.coerceIn(0f, 1f)),
            modifier = Modifier
                .offset { IntOffset(curLeft.roundToPx(), curTop.roundToPx()) }
                .size(width = curWidth, height = curHeight)
                .shadow((16.dp * p.coerceIn(0f, 1f)), RoundedCornerShape(containerRadius))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        animateDismiss { onOpenDetail(currentItem.slug, currentItem.sourceId) }
                    },
                ),
        ) {
            val menu: @Composable RowScope.() -> Unit = {
                MorphingMenuActions(
                    currentItem = currentItem,
                    progress = p,
                    isBookmarked = isBookmarked,
                    lastReadChapterTitle = lastReadChapterTitle,
                    onBookmarkToggle = handleBookmarkToggle,
                    onStartRead = { handleReadAction(true) },
                    onResumeRead = { handleReadAction(false) },
                    modifier = Modifier.weight(1f),
                )
            }
            val cover = @Composable {
                MorphingCoverThumb(
                    currentItem = currentItem,
                    fillWhenClosed = p < 0.25f,
                    modifier = Modifier.fillMaxHeight(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(containerPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isExpandRight) {
                    cover()
                    if (p > 0.2f) {
                        Spacer(modifier = Modifier.width(lerp(0.dp, 10.dp, ((p - 0.2f) / 0.8f).coerceIn(0f, 1f))))
                        menu()
                    }
                } else {
                    if (p > 0.2f) {
                        menu()
                        Spacer(modifier = Modifier.width(lerp(0.dp, 10.dp, ((p - 0.2f) / 0.8f).coerceIn(0f, 1f))))
                    }
                    cover()
                }
            }
        }
    }
}

@Composable
private fun MorphingCoverThumb(
    currentItem: CardHoldTarget,
    fillWhenClosed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(
                if (fillWhenClosed) Modifier.fillMaxWidth() else Modifier.aspectRatio(2f / 3f),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentItem.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = currentItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, PureBlack.copy(alpha = 0.85f)),
                    ),
                ),
        )

        if (currentItem.sourceId.isNotBlank()) {
            Surface(
                color = PureBlack.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.padding(6.dp).align(Alignment.TopStart),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(PrimaryPink))
                    Text(
                        text = currentItem.sourceId.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MorphingMenuActions(
    currentItem: CardHoldTarget,
    progress: Float,
    isBookmarked: Boolean,
    lastReadChapterTitle: String?,
    onBookmarkToggle: () -> Unit,
    onStartRead: () -> Unit,
    onResumeRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuAlpha = ((progress - 0.25f) / 0.75f).coerceIn(0f, 1f)
    val menuTranslateY = (1f - menuAlpha) * 12f

    Column(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                alpha = menuAlpha
                translationY = menuTranslateY
            },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Susun 1: Info Judul Komik & Progres
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            Text(currentItem.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = lastReadChapterTitle?.let { stringResource(R.string.card_action_last_read_prefix, it) } ?: stringResource(R.string.card_action_not_read_yet)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 2.dp))

        // Susun 2: 3 Tombol Vertikal Berbobot
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MorphingVerticalButton(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = stringResource(R.string.card_action_read_start),
                subtitle = stringResource(R.string.card_action_start_sub),
                containerColor = PrimaryPink.copy(alpha = 0.16f),
                borderColor = PrimaryPink.copy(alpha = 0.40f),
                iconColor = PrimaryPink,
                textColor = BlushWhite,
                onClick = onStartRead,
            )

            val continueSub = if (lastReadChapterTitle != null) "Ch. $lastReadChapterTitle" else "Ch. Terbaru"
            MorphingVerticalButton(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.card_action_read_resume),
                subtitle = continueSub,
                iconColor = SuccessGreen,
                onClick = onResumeRead,
            )

            val bmTitle = stringResource(if (isBookmarked) R.string.card_action_bookmarked_title else R.string.card_action_bookmark_add)
            val bmSub = stringResource(if (isBookmarked) R.string.card_action_bookmarked_sub else R.string.card_action_bookmark_sub)
            MorphingVerticalButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                title = bmTitle,
                subtitle = bmSub,
                iconColor = if (isBookmarked) BookmarkAmber else TextSecondary,
                onClick = onBookmarkToggle,
            )
        }
    }
}

@Composable
private fun MorphingVerticalButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit,
    containerColor: Color = Color.White.copy(alpha = 0.06f),
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    textColor: Color = TextPrimary,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(0.9.dp, borderColor),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(iconColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textColor, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
