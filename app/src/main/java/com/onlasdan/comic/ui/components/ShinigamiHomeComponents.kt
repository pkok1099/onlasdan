package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.Paged
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.formatChapterShort
import com.onlasdan.comic.ui.util.formatTimeCompact
import com.onlasdan.comic.ui.util.getCountryFlagEmoji
import com.onlasdan.comic.ui.util.noirClickable
import com.onlasdan.comic.ui.util.noirCombinedClickable
import com.onlasdan.comic.ui.util.pressScale
import com.onlasdan.comic.ui.util.staggeredEntrance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hero carousel bergaya Shinigami ID: banner 4:3 tinggi dengan rating, tag New/Popular, dan cuplikan sinopsis.
 */
@Composable
fun ShinigamiHeroCarousel(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (comics.isEmpty()) return
    val carouselItems = remember(comics) { comics.take(6) }
    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    LaunchedEffect(pagerState, carouselItems.size) {
        if (carouselItems.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000L)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % carouselItems.size
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = NoirMotion.spatialSpring(),
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            val comic = carouselItems.getOrNull(pageIndex) ?: return@HorizontalPager
            ShinigamiHeroSlide(
                comic = comic,
                onClick = { onComicClick(comic) },
            )
        }

        // Pager dots indicator
        if (carouselItems.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(carouselItems.size) { dotIdx ->
                    val isActive = dotIdx == pagerState.currentPage
                    val targetWidth = if (isActive) 18f else 6f
                    val dotWidth by animateNoirFloatAsState(
                        targetValue = targetWidth,
                        animationSpec = NoirMotion.spatialSpring(),
                        label = "shinigamiDotWidth_$dotIdx",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth.dp)
                            .clip(CircleShape)
                            .background(if (isActive) PrimaryPink else DarkOutlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShinigamiHeroSlide(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    var imgLoaded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .aspectRatio(4f / 3.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    ) {
        if (!imgLoaded) {
            ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp))
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comic.cover)
                .crossfade(true)
                .build(),
            contentDescription = comic.title,
            contentScale = ContentScale.Crop,
            onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
            modifier = Modifier.fillMaxSize(),
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PureBlack.copy(alpha = 0.45f),
                            PureBlack.copy(alpha = 0.96f),
                        ),
                        startY = 80f,
                    ),
                ),
        )

        // Overlay text content at bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = comic.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Rating badge
                val ratingScore = comic.rating?.ifBlank { "8.6" } ?: "8.6"
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkSurface.copy(alpha = 0.85f),
                    border = BorderStroke(StrokeWidth.hairline, DarkOutline),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentPink,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = ratingScore,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentPink,
                        )
                    }
                }

                // New tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TagOrange.copy(alpha = 0.2f),
                    border = BorderStroke(StrokeWidth.hairline, TagAmber),
                ) {
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = TagAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                // Popular tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TagRed,
                ) {
                    Text(
                        text = "Popular",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (!comic.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = comic.synopsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Section Rekomendasi ala Shinigami dengan filter format (Manhwa, Manga, Manhua)
 * dan poster tinggi ber-badge bendera serta waktu rilis.
 */
@Composable
fun ShinigamiRecommendationsSection(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    onLoadFormat: (suspend (format: String) -> List<Comic>)? = null,
) {
    var selectedFormat by remember { mutableStateOf("Manhwa") }
    val formats = remember { listOf("Manhwa", "Manga", "Manhua") }
    val scope = rememberCoroutineScope()

    val cachedFormats = remember {
        mutableStateMapOf<String, List<Comic>>().apply {
            if (comics.isNotEmpty()) {
                put("Manhwa", comics)
            }
        }
    }
    var isLoading by remember { mutableStateOf(false) }

    fun selectFormat(fmt: String) {
        if (selectedFormat == fmt) return
        selectedFormat = fmt
        if (!cachedFormats.containsKey(fmt) && onLoadFormat != null) {
            scope.launch {
                isLoading = true
                try {
                    val result = onLoadFormat(fmt.lowercase())
                    if (result.isNotEmpty()) {
                        cachedFormats[fmt] = result
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ShinigamiRecs", "Gagal memuat format $fmt", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val currentComics = cachedFormats[selectedFormat] ?: emptyList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Section Header
        Text(
            text = "Rekomendasi",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        // Filter chips: [ Manhwa ] [ Manga ] [ Manhua ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            formats.forEach { fmt ->
                val isSelected = selectedFormat.equals(fmt, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryPink else DarkSurfaceElevated,
                    border = BorderStroke(StrokeWidth.hairline, if (isSelected) PrimaryPink else DarkOutline),
                    modifier = Modifier.noirClickable(pressedScale = 0.94f) {
                        selectFormat(fmt)
                    },
                ) {
                    Text(
                        text = fmt,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Horizontal Row of Recommendation Cards (atau Loading Shimmer)
        if (isLoading) {
            ShimmerScope {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(
                        count = 4,
                        key = { it },
                        contentType = { "shinigami_rec_skeleton" },
                    ) {
                        CardPlaceholder(width = 140.dp)
                    }
                }
            }
        } else if (currentComics.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(
                    items = currentComics,
                    key = { "${it.sourceId}_${it.id}" },
                    contentType = { "shinigami_rec_card" },
                ) { comic ->
                    ShinigamiRecommendationCard(
                        comic = comic,
                        onClick = { onComicClick(comic) },
                    )
                }
            }
        }
    }
}

@Composable
fun ShinigamiRecommendationCard(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    width: Dp = 140.dp,
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

    val flag = getCountryFlagEmoji(comic.type)
    val timeText = formatRelativeShort(comic.lastUpdated)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.hairline, DarkOutline),
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .graphicsLayer {
                alpha = if (isBeingMorphed) 0f else 1f
            }
            .onGloballyPositioned { coordinates ->
                cardBounds.bounds = coordinates.boundsInRoot()
            }
            .noirCombinedClickable(pressedScale = 0.94f, onLongClick = effectiveLongClick, onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!imgLoaded) {
                ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp))
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize(),
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Top-Left: Time + UP badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PureBlack.copy(alpha = 0.75f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = if (timeText.isNotBlank()) timeText else "15h",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = TagRed,
                            modifier = Modifier.padding(start = 2.dp),
                        ) {
                            Text(
                                text = "U",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.sp,
                                    lineHeight = 9.sp,
                                    fontWeight = FontWeight.Black,
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                    }
                }

                // Top-Right: Country Flag
                if (flag != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PureBlack.copy(alpha = 0.65f),
                    ) {
                        Text(
                            text = flag,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section Update ala Shinigami: filter tab Project/Mirror + toggle view grid/list,
 * serta card 3 kolom dengan 2 baris chapter di bawah poster dan pagination angka.
 */
@Composable
fun ShinigamiUpdateSection(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLoadUpdates: (suspend (type: String, page: Int) -> Paged<Comic>)? = null,
    entranceDone: Boolean = true,
) {
    var selectedFilter by remember { mutableStateOf("Project") }
    var currentPage by remember { mutableIntStateOf(1) }
    var isGrid by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val cachedPages = remember {
        mutableStateMapOf<String, Paged<Comic>>().apply {
            if (comics.isNotEmpty()) {
                put("Project:1", Paged(items = comics, page = 1, totalPages = 57, hasNextPage = true))
            }
        }
    }
    var isLoading by remember { mutableStateOf(false) }

    fun loadPage(type: String, page: Int) {
        val key = "$type:$page"
        if (cachedPages.containsKey(key)) return
        if (onLoadUpdates == null) return
        scope.launch {
            isLoading = true
            try {
                val result = onLoadUpdates(type.lowercase(), page)
                cachedPages[key] = result
            } catch (e: Exception) {
                android.util.Log.e("ShinigamiUpdate", "Gagal memuat update $key", e)
            } finally {
                isLoading = false
            }
        }
    }

    val currentPaged = cachedPages["$selectedFilter:$currentPage"]
    val currentComics = currentPaged?.items ?: if (selectedFilter == "Project" && currentPage == 1) comics else emptyList()
    val totalPages = currentPaged?.totalPages ?: 57

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Update",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )

            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_action_see_all),
                    color = AccentPink,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        // Controls: [ Project ] [ Mirror ] + [ ▦ ] [ ☲ ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                val tabs = listOf("Project", "Mirror")
                tabs.forEach { tab ->
                    val isSelected = selectedFilter.equals(tab, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryPink else DarkSurfaceElevated,
                        border = BorderStroke(StrokeWidth.hairline, if (isSelected) PrimaryPink else DarkOutline),
                        modifier = Modifier.noirClickable(pressedScale = 0.94f) {
                            if (!isSelected) {
                                selectedFilter = tab
                                currentPage = 1
                                loadPage(tab, 1)
                            }
                        },
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // Grid / List toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(StrokeWidth.hairline, DarkOutline),
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    IconButton(
                        onClick = { isGrid = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = stringResource(R.string.cd_grid_view),
                            tint = if (isGrid) PrimaryPink else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(
                        onClick = { isGrid = false },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = stringResource(R.string.cd_list_view),
                            tint = if (!isGrid) PrimaryPink else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Loading Shimmer or Card Container
        if (isLoading) {
            ShimmerScope {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    repeat(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            repeat(3) {
                                CardPlaceholder(
                                    modifier = Modifier.weight(1f),
                                    width = Dp.Unspecified,
                                )
                            }
                        }
                    }
                }
            }
        } else if (isGrid) {
            val rows = remember(currentComics) { currentComics.chunked(3) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                rows.forEachIndexed { rIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        rowItems.forEachIndexed { cIdx, comic ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .staggeredEntrance(0, entranceDone),
                            ) {
                                ShinigamiUpdateCard(
                                    comic = comic,
                                    onClick = { onComicClick(comic) },
                                )
                            }
                        }
                        val remainder = 3 - rowItems.size
                        if (remainder > 0) {
                            repeat(remainder) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                currentComics.forEachIndexed { idx, comic ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0, entranceDone),
                    ) {
                        ComicCard(
                            comic = comic,
                            onClick = { onComicClick(comic) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Numbered Pagination
        if (!isLoading && totalPages > 1) {
            NumberPagination(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageSelected = { newPage ->
                    currentPage = newPage
                    loadPage(selectedFilter, newPage)
                },
            )
        }
    }
}

/**
 * Kartu update 3 kolom khas Shinigami dengan Cinema Gradient Overlay:
 * 1. Poster aspect ratio 3:4.4 dengan flag emoji di kanan atas.
 * 2. Info chapter "Ch.xx" dan waktu "1d|j|h" tertanam di bawah poster lewat gradient overlay.
 * 3. Judul komik bersih 2 baris di bawah cover tanpa badge [UP] yang memakan space.
 */
@Composable
fun ShinigamiUpdateCard(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
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

    val flag = getCountryFlagEmoji(comic.type)
    val chapterText = remember(comic.latestChapter) { formatChapterShort(comic.latestChapter) }
    val timeText = remember(comic.lastUpdated) { formatTimeCompact(comic.lastUpdated) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isBeingMorphed) 0f else 1f
            }
            .noirCombinedClickable(pressedScale = 0.95f, onLongClick = effectiveLongClick, onClick = onClick),
    ) {
        // Poster image container (Ratio 3:4.4 matching prototype)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.4f)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceElevated)
                .border(BorderStroke(StrokeWidth.hairline, DarkOutline), RoundedCornerShape(10.dp))
                .onGloballyPositioned { coordinates ->
                    cardBounds.bounds = coordinates.boundsInRoot()
                },
        ) {
            if (!imgLoaded) {
                ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp))
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize(),
            )

            // Top-right country flag
            if (flag != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PureBlack.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Text(
                        text = flag,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }

            // Cinema Gradient Overlay with Chapter Info & Time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                PureBlack.copy(alpha = 0.40f),
                                PureBlack.copy(alpha = 0.80f),
                                PureBlack.copy(alpha = 0.95f),
                            ),
                        ),
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (chapterText.isNotBlank()) {
                        Text(
                            text = chapterText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }

                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp,
                            ),
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Clean title below cover without [UP] badge
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
