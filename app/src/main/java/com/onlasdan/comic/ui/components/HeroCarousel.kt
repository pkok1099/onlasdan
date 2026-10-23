package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.pressScale
import kotlinx.coroutines.delay

private val HeroBottomGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        PureBlack.copy(alpha = 0.55f),
        PureBlack.copy(alpha = 0.98f),
    ),
    startY = 60f,
)

@Composable
fun HeroCarousel(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (comics.isEmpty()) return
    val carouselItems = remember(comics) { comics.take(8) }
    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    // Auto-scroll every 5 seconds using pure spring physics (NO tween, NO timer ms)
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
            key = { pageIndex -> carouselItems.getOrNull(pageIndex)?.id ?: pageIndex },
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            val comic = carouselItems.getOrNull(pageIndex) ?: return@HorizontalPager
            HeroSlide(
                comic = comic,
                onClick = { onComicClick(comic) },
            )
        }

        // Dot indicator with spring transitions
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
                        label = "dotWidth_$dotIdx",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .layout { measurable, constraints ->
                                val wPx = dotWidth.dp.roundToPx()
                                val placeable = measurable.measure(
                                    constraints.copy(minWidth = wPx, maxWidth = wPx),
                                )
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .clip(CircleShape)
                            .background(if (isActive) PrimaryPink else DarkOutlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context)
            .data(comic.cover)
            .crossfade(true)
            .build()
    }
    var imgLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .aspectRatio(16f / 9f)
            .clip(CardShape)
            .background(DarkSurfaceElevated)
            .border(BorderStroke(StrokeWidth.hairline, DarkOutline), CardShape)
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    ) {
        // Full width cover image (16:9 crop) with bottom gradient
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
                .drawBehind {
                    drawRect(HeroBottomGradient)
                },
        )

        // Overlay content: Top badges + Bottom text & CTA button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top badges: Category badge + Star rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(type = comic.type)

                if (!comic.rating.isNullOrBlank()) {
                    Surface(
                        shape = ChipShape,
                        color = DarkSurface.copy(alpha = 0.85f),
                        border = BorderStroke(StrokeWidth.regular, PrimaryPink.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(10.dp),
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
            }

            // Bottom title and "Baca Sekarang" button
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = comic.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                PrimaryButton(
                    text = stringResource(R.string.home_hero_read_now),
                    icon = Icons.Default.PlayArrow,
                    onClick = onClick,
                    modifier = Modifier.height(36.dp),
                )
            }
        }
    }
}
