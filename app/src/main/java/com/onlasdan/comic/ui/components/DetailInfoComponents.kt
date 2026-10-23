package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.Genre
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.pressScale

@Composable
internal fun RatingCard(
    rating: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = rating,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    text = "/ 10",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = stringResource(R.string.detail_rating_score),
                style = MaterialTheme.typography.labelSmall,
                color = AccentPink,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeriesInformationPanel(
    comic: Comic,
    onGenreClick: ((Genre) -> Unit)?,
    modifier: Modifier = Modifier,
    genreFilterSupported: Boolean = true,
) {
    val hasAuthors = !comic.author.isNullOrBlank()
    val hasGroups = comic.groups.isNotEmpty()
    val hasSeries = comic.series.isNotEmpty()
    val hasCharacters = comic.characters.isNotEmpty()
    val hasViews = !comic.views.isNullOrBlank()
    val hasGenres = comic.genres.isNotEmpty()

    if (!hasAuthors && !hasGroups && !hasSeries && !hasCharacters && !hasViews && !hasGenres) {
        return
    }

    Surface(
        shape = CardShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.detail_series_info),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentPink,
            )

            HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.hairline)

            val textRows = listOfNotNull(
                comic.author?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.detail_authors) to it },
                comic.groups.takeIf { it.isNotEmpty() }
                    ?.let { stringResource(R.string.detail_groups) to it.joinToString(", ") },
                comic.series.takeIf { it.isNotEmpty() }
                    ?.let { stringResource(R.string.detail_series) to it.joinToString(", ") },
                comic.characters.takeIf { it.isNotEmpty() }
                    ?.let { stringResource(R.string.detail_characters) to it.joinToString(", ") },
                comic.views?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.detail_views) to it },
            )
            textRows.forEach { (label, value) -> SeriesInfoRow(label = label, value = value) }

            if (hasGenres) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.screen_genres),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        comic.genres.forEach { g ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val chipModifier = if (genreFilterSupported) {
                                Modifier
                                    .pressScale(interactionSource)
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        onGenreClick?.invoke(g)
                                    }
                            } else {
                                Modifier
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryPink.copy(alpha = 0.12f),
                                border = BorderStroke(StrokeWidth.hairline, PrimaryPink.copy(alpha = 0.4f)),
                                modifier = chipModifier,
                            ) {
                                Text(
                                    text = g.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = AccentPink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextSecondary,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Skeleton header saat loading — FIXED seperti header aslinya. */
@Composable
internal fun DetailHeaderPlaceholder(modifier: Modifier = Modifier) = ShimmerScope { DetailHeaderPlaceholderContent(modifier) }

@Composable
internal fun DetailHeaderPlaceholderContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f),
                shape = CardShape,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    shape = RoundedCornerShape(4.dp),
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp),
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        }
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = ButtonShape,
        )
    }
}
