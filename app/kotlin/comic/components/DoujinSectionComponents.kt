package comic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import comic.LocalActiveCardHoldTarget
import comic.LocalCardHoldHandler
import comic.R
import comic.components.ComicThumbSfxStamp
import comic.components.comicShockwaveCard
import comic.source.AppDateFormatter
import comic.source.Comic
import comic.theme.*
import comic.util.NoirMotion
import comic.util.animateNoirFloatAsState
import comic.util.noirClickable
import comic.util.noirCombinedClickable
import comic.util.rememberSelectedSource

@Composable
fun CategoryBadge(
    type: String?,
    modifier: Modifier = Modifier,
) {
    ComicCategoryBadge(type = type, modifier = modifier)
}

@Composable
fun StatusBadge(
    status: String?,
    modifier: Modifier = Modifier,
) {
    ComicStatusBadge(status = status, modifier = modifier)
}

fun formatRelativeShort(lastUpdated: String?): String {
    if (lastUpdated.isNullOrBlank()) return ""
    val formatted = AppDateFormatter.format(lastUpdated)
    return formatted.replace(" lalu", "").trim()
}

@Composable
fun DoujinSectionBanner(
    title: String,
    isGrid: Boolean,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryPinkDark.copy(alpha = 0.85f),
                            DarkSurfaceElevated,
                            DarkSurface,
                        ),
                    ),
                )
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PrimaryPink),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                }

                IconButton(
                    onClick = onToggleLayout,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant),
                ) {
                    Icon(
                        imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = stringResource(if (isGrid) R.string.cd_list_view else R.string.cd_grid_view),
                        tint = AccentPink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DoujinComicCard(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val cardHoldHandler = LocalCardHoldHandler.current
    val activeCardHold = LocalActiveCardHoldTarget.current
    val cardBounds = remember { CardBoundsHolder() }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthPx = remember(windowInfo.containerSize) { windowInfo.containerSize.width }
    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context).data(comic.cover).crossfade(true).build()
    }
    val fallbackSource = rememberSelectedSource()
    val effectiveSourceId = if (!comic.sourceId.isNullOrBlank()) comic.sourceId else fallbackSource

    val effectiveLongClick = onLongClick ?: {
        val col = when {
            cardBounds.bounds.center.x < screenWidthPx * 0.38f -> 0
            cardBounds.bounds.center.x < screenWidthPx * 0.70f -> 1
            else -> 2
        }
        cardHoldHandler(
            CardHoldTarget(
                slug = comic.id,
                sourceId = effectiveSourceId,
                title = comic.title,
                coverUrl = comic.cover,
                originBounds = cardBounds.bounds,
                columnIndex = col,
            ),
        )
    }

    val isBeingMorphed = activeCardHold != null &&
        activeCardHold.slug == comic.id &&
        (activeCardHold.sourceId == effectiveSourceId || activeCardHold.sourceId.isBlank())

    val relativeTime = remember(comic.lastUpdated) { formatRelativeShort(comic.lastUpdated) }
    var imgLoaded by remember { mutableStateOf(false) }
    val imgAlpha by animateNoirFloatAsState(
        targetValue = if (imgLoaded) 1f else 0f,
        animationSpec = NoirMotion.Effects.Float,
        label = "doujinCardImageAlpha",
    )
    val imgScale by animateNoirFloatAsState(
        targetValue = if (imgLoaded) 1f else 1.06f,
        animationSpec = NoirMotion.SpatialMedium.asFloat,
        label = "doujinCardImageScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .comicShockwaveCard(cardBounds)
            .graphicsLayer { alpha = if (isBeingMorphed) 0f else 1f }
            .noirCombinedClickable(pressedScale = 0.93f, onLongClick = effectiveLongClick, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // Cover (2:3 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(CardShape)
                .background(DarkSurfaceElevated)
                .border(BorderStroke(StrokeWidth.hairline, DarkOutline), CardShape)
                .onGloballyPositioned { coordinates ->
                    cardBounds.bounds = coordinates.boundsInRoot()
                },
        ) {
            if (!imgLoaded) {
                ShimmerBox(modifier = Modifier.fillMaxSize(), shape = CardShape)
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = imgAlpha
                        scaleX = imgScale
                        scaleY = imgScale
                    },
            )
            ComicThumbSfxStamp(seed = comic.id, type = comic.type)

            // Category badge at top-left
            if (!comic.type.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.xs),
                ) {
                    CategoryBadge(type = comic.type)
                }
            }

            // Star rating badge at top-right
            if (!comic.rating.isNullOrBlank()) {
                Surface(
                    shape = ChipShape,
                    color = DarkSurface.copy(alpha = 0.85f),
                    border = BorderStroke(StrokeWidth.regular, PrimaryPink.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ComicSfxYellow,
                            modifier = Modifier.size(9.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = comic.rating,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = ComicSfxYellow,
                        )
                    }
                }
            }
        }

        // Title 2 lines
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )

        // Pill chapter + relative timestamp ("15 jam")
        val chapterLabel = comic.latestChapter
        if (!chapterLabel.isNullOrBlank() || relativeTime.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(StrokeWidth.regular, DarkOutline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!chapterLabel.isNullOrBlank()) {
                        Text(
                            text = chapterLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = AccentPink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!chapterLabel.isNullOrBlank() && relativeTime.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                        )
                    }
                    if (relativeTime.isNotBlank()) {
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoujinComicListItem(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val cardHoldHandler = LocalCardHoldHandler.current
    val activeCardHold = LocalActiveCardHoldTarget.current
    val cardBounds = remember { CardBoundsHolder() }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthPx = remember(windowInfo.containerSize) { windowInfo.containerSize.width }
    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context).data(comic.cover).crossfade(true).build()
    }
    var imgLoaded by remember { mutableStateOf(false) }
    val fallbackSource = rememberSelectedSource()
    val effectiveSourceId = if (!comic.sourceId.isNullOrBlank()) comic.sourceId else fallbackSource

    val effectiveLongClick = onLongClick ?: {
        val col = when {
            cardBounds.bounds.center.x < screenWidthPx * 0.38f -> 0
            cardBounds.bounds.center.x < screenWidthPx * 0.70f -> 1
            else -> 2
        }
        cardHoldHandler(
            CardHoldTarget(
                slug = comic.id,
                sourceId = effectiveSourceId,
                title = comic.title,
                coverUrl = comic.cover,
                originBounds = cardBounds.bounds,
                columnIndex = col,
            ),
        )
    }

    val isBeingMorphed = activeCardHold != null &&
        activeCardHold.slug == comic.id &&
        (activeCardHold.sourceId == effectiveSourceId || activeCardHold.sourceId.isBlank())

    val relativeTime = remember(comic.lastUpdated) { formatRelativeShort(comic.lastUpdated) }

    Surface(
        shape = CardShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        modifier = modifier
            .fillMaxWidth()
            .comicShockwaveCard(cardBounds)
            .graphicsLayer { alpha = if (isBeingMorphed) 0f else 1f }
            .noirCombinedClickable(pressedScale = 0.93f, onLongClick = effectiveLongClick, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(BorderStroke(StrokeWidth.hairline, DarkOutline), RoundedCornerShape(8.dp))
                    .onGloballyPositioned { coordinates ->
                        cardBounds.bounds = coordinates.boundsInRoot()
                    },
            ) {
                if (!imgLoaded) {
                    ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp))
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(type = comic.type)

                    if (!comic.rating.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = comic.rating,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentPink,
                            )
                        }
                    }
                }

                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Chapter + Relative time pill
                val chapterLabel = comic.latestChapter
                if (!chapterLabel.isNullOrBlank() || relativeTime.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!chapterLabel.isNullOrBlank()) {
                            Text(
                                text = chapterLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AccentPink,
                            )
                        }
                        if (!chapterLabel.isNullOrBlank() && relativeTime.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        if (relativeTime.isNotBlank()) {
                            Text(
                                text = relativeTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
