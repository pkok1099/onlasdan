package comic.reader

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import comic.R
import comic.db.ComicDatabase
import comic.theme.*
import comic.util.NoirMotion
import comic.util.extractSlug
import comic.util.noirClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@Suppress("kotlin:S3776")
fun ReaderChaptersSheetContent(
    comicDetails: ReaderComicDetail?,
    sourceId: String,
    slug: String,
    chapterSlug: String,
    readVersion: Int,
    context: Context,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readSet by produceState(initialValue = emptySet<String>(), slug, sourceId, readVersion) {
        if (slug.isBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            ComicDatabase.get(context).readDao().getReadChapters(slug, sourceId).toSet()
        }
    }
    val chapters = comicDetails?.chapterList.orEmpty()
    val listState = rememberLazyListState()

    val currentIdx = remember(chapters, chapterSlug) {
        chapters.indexOfFirst { isChapterSlugMatch(it, chapterSlug) }
    }
    LaunchedEffect(currentIdx) {
        if (currentIdx >= 0) {
            listState.scrollToItem((currentIdx - 2).coerceAtLeast(0))
        }
    }

    if (chapters.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.detail_empty_chapters),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
        ) {
            itemsIndexed(
                items = chapters,
                key = { i, ch -> ch.url ?: "${i}_${ch.title}" },
                contentType = { _, _ -> "reader_chapter_row" },
            ) { _, chapter ->
                val currentSlug = chapter.id.substringAfterLast(':')
                val isCurrent = currentSlug == chapterSlug
                val isRead = readSet.contains(currentSlug)

                Surface(
                    color = if (isCurrent) DarkSurfaceElevated else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = NoirMotion.Effects.Float,
                            fadeOutSpec = NoirMotion.Effects.Float,
                            placementSpec = NoirMotion.Spatial.asOffset,
                        )
                        .noirClickable(pressedScale = 0.96f) { onNavigate(currentSlug) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chapter.title,
                            color = when {
                                isCurrent -> PrimaryPink
                                isRead -> AccentPink
                                else -> TextPrimary
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isCurrent) {
                            Text(
                                text = stringResource(R.string.reader_chapter_active),
                                color = PrimaryPink,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = Spacing.xs),
                            )
                        } else if (isRead) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentPink,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = DarkOutline.copy(alpha = 0.5f), thickness = StrokeWidth.hairline)
            }
        }
    }
}
