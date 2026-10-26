package com.onlasdan.comic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.BlushTint
import com.onlasdan.comic.ui.theme.CardShape
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.SkeletonHalftoneShaderBrush
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.rememberReducedMotion
import kotlinx.coroutines.withContext

/** Warna dasar skeleton: permukaan gelap terdefinisi di atas PureBlack. */
private val SkeletonBase = Color(0xFF16161F)

/** Highlight kilau: pita shimmer lembut dengan sentuhan Blush di tengah. */
private val SkeletonHighlightEdge = Color(0xFF222230)
private val SkeletonHighlightPeak = Color(0xFF2E2A3A)

/** Border super halus agar siluet placeholder bersih tanpa outline tebal yang berisik. */
private val SkeletonBorder = Color.White.copy(alpha = 0.07f)

/**
 * Driver shimmer bersama (keputusan rombak 2026-10-21): satu Animatable untuk
 * seluruh skeleton di dalam satu [ShimmerScope], supaya kilau bergerak sebagai
 * satu permukaan (common fate), bukan berdenyut per-blok.
 * Jika sudah berada di dalam [ShimmerScope] induk, langsung gunakan driver induk.
 */
val LocalShimmerTranslate = compositionLocalOf<Animatable<Float, AnimationVector1D>?> { null }

@Composable
fun ShimmerScope(content: @Composable () -> Unit) {
    val existing = LocalShimmerTranslate.current
    val reducedMotion = rememberReducedMotion()
    val shimmerSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.Ambient.Shimmer
    val translateAnim = remember { Animatable(-600f) }
    LaunchedEffect(shimmerSpec, existing == null) {
        if (existing == null) {
            withContext(NoirForceMotionScale) {
                while (true) {
                    translateAnim.snapTo(-600f)
                    translateAnim.animateTo(1800f, shimmerSpec)
                }
            }
        }
    }
    CompositionLocalProvider(LocalShimmerTranslate provides (existing ?: translateAnim)) {
        content()
    }
}

@Composable
private fun rememberShimmerTranslate(shared: Animatable<Float, AnimationVector1D>?): Float {
    if (shared != null) return shared.value
    val ownAnim = remember { Animatable(-600f) }
    val reducedMotion = rememberReducedMotion()
    val shimmerSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.Ambient.Shimmer
    LaunchedEffect(shimmerSpec) {
        withContext(NoirForceMotionScale) {
            while (true) {
                ownAnim.snapTo(-600f)
                ownAnim.animateTo(1800f, shimmerSpec)
            }
        }
    }
    return ownAnim.value
}

/**
 * Kotak shimmer dasar dengan visual Noir & Blush serta tekstur screentone tipis.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = CardShape) {
    val translate = rememberShimmerTranslate(LocalShimmerTranslate.current)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SkeletonBase,
            SkeletonHighlightEdge,
            SkeletonHighlightPeak,
            SkeletonHighlightEdge,
            SkeletonBase,
        ),
        start = Offset(translate - 520f, translate - 520f),
        end = Offset(translate, translate),
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
            .drawWithContent {
                drawContent()
                drawRect(SkeletonHalftoneShaderBrush)
            }
            .border(0.75.dp, SkeletonBorder, shape),
    )
}

/**
 * Skeleton kartu komik: poster (default 2:3) + opsional 2 bar teks judul.
 */
@Composable
fun CardPlaceholder(
    modifier: Modifier = Modifier,
    width: Dp = 130.dp,
    posterRatio: Float = 2f / 3f,
    posterShape: Shape = CardShape,
    showTextLines: Boolean = true,
) {
    val sizeMod = if (width != Dp.Unspecified) modifier.width(width) else modifier
    Column(
        modifier = sizeMod,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(posterRatio),
            shape = posterShape,
        )
        if (showTextLines) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(13.dp),
                shape = RoundedCornerShape(4.dp),
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(11.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
    }
}

/**
 * Header section placeholder: aksen bar vertikal + judul + opsional aksi "Lihat Semua" —
 * meniru [SectionHeader] asli.
 */
@Composable
fun SectionTitlePlaceholder(
    modifier: Modifier = Modifier,
    showAction: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ShimmerBox(modifier = Modifier.size(width = 4.dp, height = 18.dp), shape = RoundedCornerShape(2.dp))
            ShimmerBox(modifier = Modifier.size(width = 136.dp, height = 18.dp), shape = RoundedCornerShape(4.dp))
        }
        if (showAction) {
            ShimmerBox(modifier = Modifier.size(width = 68.dp, height = 14.dp), shape = RoundedCornerShape(4.dp))
        }
    }
}

@Composable
fun PlaceholderRow(modifier: Modifier = Modifier, itemCount: Int = 4, cardWidth: Dp = 130.dp) = ShimmerScope {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SectionTitlePlaceholder(showAction = true)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(
                count = itemCount,
                key = { it },
                contentType = { "card_skeleton" },
            ) {
                CardPlaceholder(width = cardWidth)
            }
        }
    }
}

@Composable
fun ChapterRowPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ShimmerBox(
                modifier = Modifier
                    .width(148.dp)
                    .height(15.dp),
                shape = RoundedCornerShape(4.dp),
            )
            ShimmerBox(
                modifier = Modifier
                    .width(84.dp)
                    .height(11.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
        ShimmerBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
        )
    }
}

/**
 * Grid katalog (untuk Home/Browse/Search/Genre/Updates/Projects).
 * Mendukung jumlah kolom dinamis (default 3), rasio poster 2:3, + 2 bar teks di bawah tiap kotak.
 */
@Composable
fun PlaceholderGrid(
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 3,
    showHeader: Boolean = false,
) = ShimmerScope { PlaceholderGridContent(modifier, columns, rows, showHeader) }

@Composable
private fun PlaceholderGridContent(
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 3,
    showHeader: Boolean = false,
) {
    val safeColumns = columns.coerceAtLeast(2)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (showHeader) SectionTitlePlaceholder(showAction = true)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    repeat(safeColumns) {
                        CardPlaceholder(modifier = Modifier.weight(1f), width = Dp.Unspecified)
                    }
                }
            }
        }
    }
}

/**
 * Skeleton halaman baca (Reader): strip vertikal full-width,
 * mencerminkan halaman komik memanjang. TANPA grid card.
 */
@Composable
fun ReaderCanvasPlaceholder(modifier: Modifier = Modifier, stripCount: Int = 3) = ShimmerScope { ReaderCanvasPlaceholderContent(modifier, stripCount) }

@Composable
private fun ReaderCanvasPlaceholderContent(modifier: Modifier = Modifier, stripCount: Int = 3) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        repeat(stripCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                )
                if (index == 0) {
                    ReaderLoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun ReaderLoadingIndicator() {
    val reducedMotion = rememberReducedMotion()
    val pulseSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.Ambient.Shimmer
    val pulse = remember { Animatable(0.4f) }
    LaunchedEffect(pulseSpec) {
        withContext(NoirForceMotionScale) {
            while (true) {
                pulse.animateTo(1f, pulseSpec)
                pulse.animateTo(0.4f, pulseSpec)
            }
        }
    }
    val alpha by animateNoirFloatAsState(
        targetValue = pulse.value,
        animationSpec = NoirMotion.Effects.Float,
        label = "readerPulse",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(PrimaryPink.copy(alpha = 0.15f * alpha))
            .border(1.dp, BlushTint.copy(alpha = 0.25f * alpha), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.6f * alpha)),
        )
    }
}
