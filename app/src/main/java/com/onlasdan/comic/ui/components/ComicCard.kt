package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.Genre
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.noirClickable
import com.onlasdan.comic.ui.util.noirCombinedClickable
import com.onlasdan.comic.ui.util.staggeredEntrance

private val ChapterBadgeGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.85f)),
)

/**
 * Pemegang referensi batas koordinat kartu tanpa membungkusnya dalam MutableState.
 * Menghilangkan mutasi snapshot berulang di fase layout selama scroll.
 */
class CardBoundsHolder(var bounds: Rect = Rect.Zero)

@Composable
fun ComicCard(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = 130.dp,
    showSourceTag: Boolean = false,
) {
    val cardHoldHandler = com.onlasdan.comic.ui.LocalCardHoldHandler.current
    val cardBounds = remember { CardBoundsHolder() }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthPx = remember(windowInfo.containerSize) {
        windowInfo.containerSize.width
    }

    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context)
            .data(comic.cover)
            .crossfade(true)
            .build()
    }

    val fallbackSource = com.onlasdan.comic.ui.util.rememberSelectedSource()
    val effectiveSourceId = if (!comic.sourceId.isNullOrBlank()) comic.sourceId else fallbackSource

    val effectiveLongClick = onLongClick ?: {
        val b = cardBounds.bounds
        val col = when {
            b.center.x < screenWidthPx * 0.38f -> 0
            b.center.x < screenWidthPx * 0.70f -> 1
            else -> 2
        }
        cardHoldHandler(
            CardHoldTarget(
                slug = comic.id,
                sourceId = effectiveSourceId,
                title = comic.title,
                coverUrl = comic.cover,
                originBounds = b,
                columnIndex = col,
            ),
        )
    }

    // Settle zoom: poster muncul dengan alpha 0->1 + scale 1.06->1.0 (mewah).
    var imgLoaded by remember { mutableStateOf(false) }
    val imgAlpha by animateNoirFloatAsState(
        targetValue = if (imgLoaded) 1f else 0f,
        animationSpec = NoirMotion.noirEffects(),
        label = "cardImageAlpha",
    )
    val imgScale by animateNoirFloatAsState(
        targetValue = if (imgLoaded) 1f else 1.06f,
        animationSpec = NoirMotion.noirSpatialMedium(),
        label = "cardImageScale",
    )

    val activeCardHold = com.onlasdan.comic.ui.LocalActiveCardHoldTarget.current
    val isBeingMorphed = activeCardHold != null &&
        activeCardHold.slug == comic.id &&
        (activeCardHold.sourceId == effectiveSourceId || activeCardHold.sourceId.isBlank())

    Column(
        modifier = modifier
            .width(width)
            .graphicsLayer {
                alpha = if (isBeingMorphed) 0f else 1f
            }
            .noirCombinedClickable(pressedScale = 0.93f, onLongClick = effectiveLongClick, onClick = onClick),
    ) {
        // Cover container with border and radius (Gestalt: Figure-Ground)
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

            // Top Badges (Rating, Source)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (showSourceTag && !comic.sourceName.isNullOrBlank()) {
                    Surface(
                        shape = ChipShape,
                        color = DarkSurface.copy(alpha = 0.85f),
                        border = BorderStroke(StrokeWidth.regular, DarkOutline),
                    ) {
                        Text(
                            text = comic.sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!comic.rating.isNullOrBlank()) {
                    Surface(
                        shape = ChipShape,
                        color = DarkSurface.copy(alpha = 0.85f),
                        border = BorderStroke(StrokeWidth.regular, DarkOutline),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentPink,
                                modifier = Modifier.size(10.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Text(
                                text = comic.rating,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentPink,
                            )
                        }
                    }
                }
            }

            // Bottom gradient overlay for latest chapter
            if (!comic.latestChapter.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ChapterBadgeGradient)
                        .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                ) {
                    Text(
                        text = comic.latestChapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Comic title
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ComicRow(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 130.dp,
    showSourceTag: Boolean = false,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(
            items = comics,
            key = { "${it.sourceId}_${it.id}" },
            contentType = { "comic_card" },
        ) { comic ->
            ComicCard(
                comic = comic,
                onClick = { onComicClick(comic) },
                width = cardWidth,
                showSourceTag = showSourceTag,
            )
        }
    }
}

@Composable
fun ComicGrid(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    entranceDone: Boolean = true,
    showSourceTag: Boolean = false,
) {
    val rows = remember(comics, columns) { comics.chunked(columns) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                rowItems.forEachIndexed { colIndex, comic ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .staggeredEntrance(0, entranceDone),
                    ) {
                        ComicCard(
                            comic = comic,
                            onClick = { onComicClick(comic) },
                            modifier = Modifier.fillMaxWidth(),
                            showSourceTag = showSourceTag,
                        )
                    }
                }
                // Fill remainder of row if last row has fewer items
                val remainder = columns - rowItems.size
                if (remainder > 0) {
                    repeat(remainder) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Extension LazyListScope untuk merender ComicGrid secara virtualized.
 * Tiap baris menjadi item terpisah di LazyColumn sehingga daur ulang slot
 * dan pelepasan memori berjalan optimal saat scroll.
 */
fun LazyListScope.comicGridItems(
    comics: List<Comic>,
    columns: Int = 3,
    entranceDone: Boolean = true,
    showSourceTag: Boolean = false,
    onComicClick: (Comic) -> Unit,
) {
    val rows = comics.chunked(columns)
    itemsIndexed(
        items = rows,
        key = { idx, row -> row.firstOrNull()?.let { "${it.sourceId}_${it.id}" } ?: "row_$idx" },
        contentType = { _, _ -> "comic_grid_row" },
    ) { rowIndex, rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            rowItems.forEachIndexed { colIndex, comic ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .staggeredEntrance(0, entranceDone),
                ) {
                    ComicCard(
                        comic = comic,
                        onClick = { onComicClick(comic) },
                        modifier = Modifier.fillMaxWidth(),
                        showSourceTag = showSourceTag,
                    )
                }
            }
            val remainder = columns - rowItems.size
            if (remainder > 0) {
                repeat(remainder) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Daftar berperingkat bernomor ala halaman Peringkat web.
 * Data-driven: tanpa cabang per source.
 */
@Composable
fun RankedComicList(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 10,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        comics.take(maxItems).forEachIndexed { index, comic ->
            RankedComicRowItem(
                index = index,
                comic = comic,
                onClick = { onComicClick(comic) },
            )
        }
    }
}

@Composable
private fun RankedComicRowItem(
    index: Int,
    comic: Comic,
    onClick: () -> Unit,
) {
    val cardHoldHandler = com.onlasdan.comic.ui.LocalCardHoldHandler.current
    val activeCardHold = com.onlasdan.comic.ui.LocalActiveCardHoldTarget.current
    val cardBounds = remember { CardBoundsHolder() }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthPx = remember(windowInfo.containerSize) {
        windowInfo.containerSize.width
    }
    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context)
            .data(comic.cover)
            .crossfade(true)
            .build()
    }
    var imgLoaded by remember { mutableStateOf(false) }
    val fallbackSource = com.onlasdan.comic.ui.util.rememberSelectedSource()
    val effectiveSourceId = if (!comic.sourceId.isNullOrBlank()) comic.sourceId else fallbackSource

    val effectiveLongClick = {
        val b = cardBounds.bounds
        val col = when {
            b.center.x < screenWidthPx * 0.38f -> 0
            b.center.x < screenWidthPx * 0.70f -> 1
            else -> 2
        }
        cardHoldHandler(
            CardHoldTarget(
                slug = comic.id,
                sourceId = effectiveSourceId,
                title = comic.title,
                coverUrl = comic.cover,
                originBounds = b,
                columnIndex = col,
            ),
        )
    }

    val isBeingMorphed = activeCardHold != null &&
        activeCardHold.slug == comic.id &&
        (activeCardHold.sourceId == effectiveSourceId || activeCardHold.sourceId.isBlank())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isBeingMorphed) 0f else 1f
            }
            .noirCombinedClickable(pressedScale = 0.93f, onLongClick = effectiveLongClick, onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (index < 3) PrimaryPink else TextSecondary,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .width(52.dp)
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
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = comic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!comic.type.isNullOrBlank()) {
                    AppChip(text = comic.type)
                }
                if (!comic.latestChapter.isNullOrBlank()) {
                    Text(
                        text = comic.latestChapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Grid kartu genre ala "Jelajahi Genre" di web.
 * Data-driven: tanpa cabang per source.
 */
@Composable
fun GenreExplorerGrid(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        genres.chunked(columns).forEach { rowGenres ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                rowGenres.forEach { genre ->
                    Surface(
                        shape = CardShape,
                        color = DarkSurfaceElevated,
                        border = BorderStroke(StrokeWidth.regular, DarkOutline),
                        modifier = Modifier
                            .weight(1f)
                            .noirClickable(pressedScale = 0.93f) { onGenreClick(genre) },
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = Spacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // Isi sisa kolom bila baris terakhir tidak penuh.
                val remainder = columns - rowGenres.size
                if (remainder > 0) {
                    repeat(remainder) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
