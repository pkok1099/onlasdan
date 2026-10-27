package comic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import comic.theme.CardShape
import comic.theme.Spacing

/** Satu baris daftar horizontal (thumbnail potret 52x78dp + dua garis teks), opsional nomor peringkat 36dp. */
@Composable
fun ListRowPlaceholder(modifier: Modifier = Modifier, rank: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank) {
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                ShimmerBox(
                    modifier = Modifier.size(width = 22.dp, height = 24.dp),
                    shape = RoundedCornerShape(Spacing.xs),
                )
            }
        }
        ShimmerBox(
            modifier = Modifier
                .width(52.dp)
                .aspectRatio(2f / 3f),
            shape = CardShape,
        )
        ListRowTextPlaceholder(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ListRowTextPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(14.dp), shape = RoundedCornerShape(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(width = 48.dp, height = 18.dp), shape = RoundedCornerShape(6.dp))
            ShimmerBox(modifier = Modifier.size(width = 64.dp, height = 12.dp), shape = RoundedCornerShape(Spacing.xs))
        }
    }
}

/** Blok section berperingkat (Komikindo): judul + baris bernomor. */
@Composable
fun RankedPlaceholder(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SectionTitlePlaceholder(showAction = true)
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

/** Blok hero Carousel 16:9 + baris dots — meniru [HeroCarousel] (DoujinDesu). */
@Composable
fun HeroCarouselPlaceholder(modifier: Modifier = Modifier) {
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
                .aspectRatio(16f / 9f),
            shape = CardShape,
        )
        HeroDotsPlaceholder(dotCount = 5)
    }
}

/** Blok hero Shinigami: slide rasio 4:3.4 + baris dots — meniru [ShinigamiHeroCarousel]. */
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
        HeroDotsPlaceholder(dotCount = 5)
    }
}

@Composable
private fun HeroDotsPlaceholder(dotCount: Int = 5) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(18.dp)
                .height(6.dp),
            shape = CircleShape,
        )
        repeat((dotCount - 1).coerceAtLeast(1)) {
            ShimmerBox(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(6.dp),
                shape = CircleShape,
            )
        }
    }
}

/**
 * Blok update Shinigami: header + baris kontrol + grid kartu 3 kolom (rasio 3:4.4) +
 * pagination bernomor — meniru [ShinigamiUpdateSection].
 */
@Composable
fun ShinigamiUpdatePlaceholder(modifier: Modifier = Modifier, rows: Int = 2) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ShinigamiUpdateHeaderSkeleton()
        ShinigamiUpdateControlsSkeleton()
        ShinigamiUpdateGridSkeleton(rows = rows)
        ShinigamiPaginationSkeleton()
    }
}

@Composable
private fun ShinigamiUpdateHeaderSkeleton() {
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
                .height(18.dp),
            shape = RoundedCornerShape(Spacing.xs),
        )
        ShimmerBox(
            modifier = Modifier
                .width(64.dp)
                .height(14.dp),
            shape = RoundedCornerShape(Spacing.xs),
        )
    }
}

@Composable
private fun ShinigamiUpdateControlsSkeleton() {
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
}

@Composable
private fun ShinigamiUpdateGridSkeleton(rows: Int) {
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
                        posterRatio = 3f / 4.4f,
                        posterShape = RoundedCornerShape(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShinigamiPaginationSkeleton() {
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

/**
 * Blok rekomendasi Shinigami: judul + chip format + baris horizontal kartu 140dp
 * (poster murni 2:3 tanpa baris teks di bawah) — meniru [ShinigamiRecommendationsSection].
 */
@Composable
fun RecommendationsPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ShimmerBox(modifier = Modifier.padding(horizontal = Spacing.lg).size(width = 120.dp, height = 18.dp), shape = RoundedCornerShape(Spacing.xs))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ChipPlaceholder(width = 72.dp)
            ChipPlaceholder(width = 64.dp)
            ChipPlaceholder(width = 68.dp)
        }
        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items(count = 4, key = { it }, contentType = { "rec_skeleton" }) {
                CardPlaceholder(width = 140.dp, posterShape = RoundedCornerShape(10.dp), showTextLines = false)
            }
        }
    }
}

/** Blok grid DoujinDesu: bar banner + grid kartu 3 kolom. */
@Composable
fun BannerGridPlaceholder(modifier: Modifier = Modifier, rows: Int = 2) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).height(48.dp), shape = CardShape)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    repeat(3) {
                        CardPlaceholder(modifier = Modifier.weight(1f), width = Dp.Unspecified)
                    }
                }
            }
        }
    }
}

/**
 * Skeleton untuk Bottom Sheet Jelajahi Genre (2 kolom kapsul/kartu pendek, BUKAN poster 2:3)
 * — meniru [GenreBrowserSheetContent].
 */
@Composable
fun GenreBrowserPlaceholder(modifier: Modifier = Modifier, rows: Int = 6) = ShimmerScope {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                repeat(2) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = CardShape,
                    )
                }
            }
        }
    }
}

/**
 * Skeleton untuk layar Daftar Genre / Tema / Demografis (chip FlowRow per seksi, BUKAN poster 2:3)
 * — meniru [GenresListScreen].
 */
@Composable
fun GenresListPlaceholder(modifier: Modifier = Modifier) = ShimmerScope {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        repeat(3) { sectionIdx ->
            GenresSectionSkeleton(sectionIdx = sectionIdx)
        }
    }
}

@Composable
private fun GenresSectionSkeleton(sectionIdx: Int) {
    val chipWidths = listOf(76.dp, 94.dp, 68.dp, 88.dp, 72.dp, 104.dp, 80.dp, 64.dp)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ShimmerBox(
            modifier = Modifier
                .width(if (sectionIdx == 0) 96.dp else 124.dp)
                .height(18.dp),
            shape = RoundedCornerShape(Spacing.xs),
        )
        repeat(2) { rowIdx ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(4) { colIdx ->
                    val w = chipWidths[(sectionIdx * 3 + rowIdx * 2 + colIdx) % chipWidths.size]
                    ShimmerBox(
                        modifier = Modifier
                            .width(w)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }
    }
}
