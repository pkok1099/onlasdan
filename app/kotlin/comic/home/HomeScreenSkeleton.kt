package comic.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comic.components.BannerGridPlaceholder
import comic.components.HeroCarouselPlaceholder
import comic.components.PlaceholderGrid
import comic.components.PlaceholderRow
import comic.components.RankedPlaceholder
import comic.components.RecommendationsPlaceholder
import comic.components.SectionTitlePlaceholder
import comic.components.ShimmerBox
import comic.components.ShimmerScope
import comic.components.ShinigamiHeroPlaceholder
import comic.components.ShinigamiUpdatePlaceholder
import comic.source.Capability
import comic.source.ComicSource
import comic.source.SectionType
import comic.theme.CardShape
import comic.theme.Spacing

@Composable
fun HomeScreenSkeleton(
    source: ComicSource,
    modifier: Modifier = Modifier,
) {
    val firstRowIndex = source.homeLayout.indexOf(SectionType.ROW)
    ShimmerScope {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            source.homeLayout.forEachIndexed { index, type ->
                item(key = "home_skel_${index}_$type") {
                    HomeSkeletonItem(source = source, type = type, isFirstRow = index == firstRowIndex)
                }
            }
            if (source.featuredGenres.isNotEmpty()) {
                item(key = "home_skel_genre_explorer") {
                    HomeGenreExplorerSkeleton(
                        showAction = source.capabilities.contains(Capability.GENRE_BROWSER),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSkeletonItem(
    source: ComicSource,
    type: SectionType,
    isFirstRow: Boolean,
) {
    when (type) {
        SectionType.HERO -> HomeHeroSkeleton(source)
        SectionType.ROW -> HomeRowSkeleton(source, isFirstRow)
        SectionType.RANKED -> RankedPlaceholder()
        SectionType.GRID -> HomeGridSkeleton(source)
    }
}

@Composable
private fun HomeHeroSkeleton(source: ComicSource) {
    when {
        source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) -> ShinigamiHeroPlaceholder()

        source.capabilities.contains(Capability.HERO_CAROUSEL) -> HeroCarouselPlaceholder()

        else -> ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            shape = CardShape,
        )
    }
}

@Composable
private fun HomeRowSkeleton(source: ComicSource, isFirstRow: Boolean) {
    if (source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) && isFirstRow) {
        RecommendationsPlaceholder()
    } else {
        PlaceholderRow(cardWidth = 130.dp)
    }
}

@Composable
private fun HomeGridSkeleton(source: ComicSource) {
    when {
        source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) -> ShinigamiUpdatePlaceholder()
        source.capabilities.contains(Capability.SECTION_BANNERS) -> BannerGridPlaceholder()
        else -> PlaceholderGrid(rows = 2, showHeader = true)
    }
}

@Composable
private fun HomeGenreExplorerSkeleton(showAction: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionTitlePlaceholder(showAction = showAction)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = CardShape,
                        )
                    }
                }
            }
        }
    }
}
