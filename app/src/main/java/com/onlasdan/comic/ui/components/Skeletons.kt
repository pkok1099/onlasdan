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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.BlushTint
import com.onlasdan.comic.ui.theme.CardShape
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.ShimmerBase
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.rememberReducedMotion
import kotlinx.coroutines.withContext

/** Highlight kilau: Blush sangat tipis di atas dasar shimmer theme. */
private val SkeletonHighlight = BlushTint.copy(alpha = 0.07f)

/** Border super halus agar tidak terlihat seperti kotak mati. */
private val SkeletonBorder = Color.White.copy(alpha = 0.04f)

/**
 * Driver shimmer bersama (keputusan rombak 2026-10-21): satu Animatable untuk
 * seluruh skeleton di dalam satu [ShimmerScope], supaya kilau bergerak sebagai
 * satu permukaan (common fate), bukan berdenyut per-blok.
 */
val LocalShimmerTranslate = compositionLocalOf<Animatable<Float, AnimationVector1D>?> { null }

@Composable
fun ShimmerScope(content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val shimmerSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.shimmerSpring()
    val translateAnim = remember { Animatable(0f) }
    LaunchedEffect(shimmerSpec) {
        withContext(NoirForceMotionScale) {
            while (true) {
                translateAnim.animateTo(1400f, shimmerSpec)
                translateAnim.snapTo(0f)
            }
        }
    }
    CompositionLocalProvider(LocalShimmerTranslate provides translateAnim) {
        content()
    }
}

/**
 * Kotak shimmer dasar dengan visual Noir & Blush.
 * Di dalam [ShimmerScope] memakai driver bersama; di luarnya menggerakkan
 * dirinya sendiri (fallback). Animasi spring (bypass 0x).
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = CardShape) {
    val sharedAnim = LocalShimmerTranslate.current
    val ownAnim = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()
    val shimmerSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.shimmerSpring()
    LaunchedEffect(shimmerSpec, sharedAnim) {
        if (sharedAnim == null) {
            withContext(NoirForceMotionScale) {
                while (true) {
                    ownAnim.animateTo(1400f, shimmerSpec)
                    ownAnim.snapTo(0f)
                }
            }
        }
    }

    val translate = sharedAnim?.value ?: ownAnim.value
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, SkeletonHighlight, ShimmerBase),
        start = Offset(translate - 450f, translate - 450f),
        end = Offset(translate, translate),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(1.dp, SkeletonBorder, shape),
    )
}

/**
 * Skeleton kartu komik: poster 2:3 + 2 bar teks judul.
 */
@Composable
fun CardPlaceholder(modifier: Modifier = Modifier, width: Dp = 130.dp) {
    Column(
        modifier = modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = CardShape,
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(14.dp),
            shape = RoundedCornerShape(4.dp),
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
            shape = RoundedCornerShape(4.dp),
        )
    }
}

@Composable
fun PlaceholderRow(modifier: Modifier = Modifier, itemCount: Int = 4, cardWidth: Dp = 130.dp) = ShimmerScope { PlaceholderRowContent(modifier, itemCount, cardWidth) }

@Composable
private fun PlaceholderRowContent(modifier: Modifier = Modifier, itemCount: Int = 4, cardWidth: Dp = 130.dp) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .width(160.dp)
                .height(20.dp),
            shape = RoundedCornerShape(4.dp),
        )
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
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp),
            )
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp),
            )
        }
        ShimmerBox(
            modifier = Modifier
                .width(20.dp)
                .height(20.dp),
            shape = RoundedCornerShape(4.dp),
        )
    }
}

/**
 * Grid katalog (HANYA untuk Home/Browse/Search).
 * 3 kolom, rasio poster 2:3, + 2 bar teks di bawah tiap kotak.
 */
@Composable
fun PlaceholderGrid(modifier: Modifier = Modifier, columns: Int = 3, rows: Int = 4) = ShimmerScope { PlaceholderGridContent(modifier, columns, rows) }

@Composable
private fun PlaceholderGridContent(modifier: Modifier = Modifier, columns: Int = 3, rows: Int = 4) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                repeat(columns) {
                    CardPlaceholder(
                        modifier = Modifier.weight(1f),
                        width = Dp.Unspecified,
                    )
                }
            }
        }
    }
}

/**
 * Skeleton halaman baca (Reader): strip vertikal full-width,
 * mencerminkan halaman komik memanjang. TANPA grid card.
 * Indikator minimalis di tengah, tidak menutupi TopBar.
 */
@Composable
fun ReaderCanvasPlaceholder(modifier: Modifier = Modifier, stripCount: Int = 3) = ShimmerScope { ReaderCanvasPlaceholderContent(modifier, stripCount) }

@Composable
private fun ReaderCanvasPlaceholderContent(modifier: Modifier = Modifier, stripCount: Int = 3) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
                // Indikator minimalis di tengah strip pertama saja.
                if (index == 0) {
                    ReaderLoadingIndicator()
                }
            }
        }
    }
}

/**
 * Indikator loading minimalis Noir & Blush: dot berdenyut
 * dengan spring (bypass 0x), tanpa tween/ms.
 */
@Composable
private fun ReaderLoadingIndicator() {
    val reducedMotion = rememberReducedMotion()
    val pulseSpec = if (reducedMotion) {
        NoirMotion.stiffSpring()
    } else {
        NoirMotion.shimmerSpring()
    }
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
        animationSpec = NoirMotion.effectsSpring(),
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

/** Garis judul section — elemen placeholder untuk header section. */
@Composable
fun SectionTitlePlaceholder(modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier
            .padding(horizontal = Spacing.lg)
            .width(120.dp)
            .height(16.dp),
        shape = RoundedCornerShape(Spacing.xs),
    )
}

/** Satu baris daftar horizontal (thumbnail potret + dua garis teks), opsional nomor peringkat. */
@Composable
fun ListRowPlaceholder(modifier: Modifier = Modifier, rank: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank) {
            ShimmerBox(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(Spacing.xs),
            )
        }
        ShimmerBox(
            modifier = Modifier.size(width = 56.dp, height = 76.dp),
            shape = CardShape,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp),
                shape = RoundedCornerShape(Spacing.xs),
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp),
                shape = RoundedCornerShape(Spacing.xs),
            )
        }
    }
}

/** Blok section berperingkat (Komikindo): judul + baris bernomor. */
@Composable
fun RankedPlaceholder(modifier: Modifier = Modifier, rows: Int = 4) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SectionTitlePlaceholder()
        repeat(rows) { ListRowPlaceholder(rank = true) }
    }
}

/** Chip filter (gaya chip section Shinigami: tinggi ~26dp, radius 8dp). */
@Composable
fun ChipPlaceholder(modifier: Modifier = Modifier, width: Dp = 64.dp) {
    ShimmerBox(
        modifier = modifier
            .width(width)
            .height(26.dp),
        shape = RoundedCornerShape(8.dp),
    )
}

/**
 * Blok hero Shinigami: slide rasio 4:3.4 + baris dots — meniru
 * ShinigamiHeroCarousel (bukan slab generik).
 */
@Composable
fun ShinigamiHeroPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .aspectRatio(4f / 3.4f),
            shape = RoundedCornerShape(14.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(18.dp)
                    .height(6.dp),
                shape = CircleShape,
            )
            repeat(4) {
                ShimmerBox(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(6.dp),
                    shape = CircleShape,
                )
            }
        }
    }
}

/**
 * Blok update Shinigami: header (judul + lihat-semua) + baris kontrol
 * (chip Project/Mirror + toggle grid/list) + grid kartu 3 kolom + pagination
 * bernomor — meniru ShinigamiUpdateSection.
 */
@Composable
fun ShinigamiUpdatePlaceholder(modifier: Modifier = Modifier, rows: Int = 2) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(Spacing.xs),
            )
            ShimmerBox(
                modifier = Modifier
                    .width(64.dp)
                    .height(14.dp),
                shape = RoundedCornerShape(Spacing.xs),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ChipPlaceholder(width = 72.dp)
                ChipPlaceholder(width = 64.dp)
            }
            ShimmerBox(
                modifier = Modifier
                    .width(60.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(rows) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(5) { i ->
                ShimmerBox(
                    modifier = Modifier
                        .padding(start = if (i == 0) 0.dp else Spacing.xs)
                        .size(width = 28.dp, height = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

/**
 * Blok rekomendasi Shinigami: judul + chip format (Manhwa/Manga/Manhua) +
 * baris horizontal kartu 140dp — meniru ShinigamiRecommendationsSection.
 */
@Composable
fun RecommendationsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SectionTitlePlaceholder()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ChipPlaceholder(width = 72.dp)
            ChipPlaceholder(width = 64.dp)
            ChipPlaceholder(width = 68.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(3) {
                CardPlaceholder(width = 140.dp)
            }
        }
    }
}

/** Blok grid DoujinDesu: bar banner + grid kartu 3 kolom. */
@Composable
fun BannerGridPlaceholder(modifier: Modifier = Modifier, rows: Int = 2) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .height(44.dp),
            shape = CardShape,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
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
}
