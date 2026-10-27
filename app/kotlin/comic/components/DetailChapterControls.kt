package comic.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import comic.R
import comic.theme.*

enum class ChapterFilter {
    ALL,
    UNREAD,
    DOWNLOADED,
}

@Composable
internal fun ChapterHeaderControls(
    totalChapterCount: Int,
    unreadChapterCount: Int,
    downloadedChapterCount: Int,
    activeFilter: ChapterFilter,
    onFilterChange: (ChapterFilter) -> Unit,
    selectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onReadFirstChapter: () -> Unit,
    onReadLatestChapter: () -> Unit,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Spacer(modifier = Modifier.height(Spacing.sm))
        HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.regular)

        // Row 1: Title & Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_chapter_count, totalChapterCount),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (totalChapterCount > 0) {
                    AppChip(
                        text = stringResource(R.string.detail_read_first_chapter),
                        onClick = onReadFirstChapter,
                    )
                    AppChip(
                        text = stringResource(R.string.browse_sort_latest),
                        onClick = onReadLatestChapter,
                    )
                }

                IconButton(
                    onClick = onToggleSort,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.detail_sort_chapters),
                        tint = AccentPink,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onToggleSelectionMode,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (selectionMode) Icons.Default.Close else Icons.Outlined.Checklist,
                        contentDescription = stringResource(R.string.detail_select_mode),
                        tint = if (selectionMode) PrimaryPink else TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Row 2: Filter segmented pill (Gestalt: Uniform Connectedness)
        SegmentedFilterPill(
            items = listOf(ChapterFilter.ALL, ChapterFilter.UNREAD, ChapterFilter.DOWNLOADED),
            selectedItem = activeFilter,
            onItemSelected = onFilterChange,
            labelProvider = { filter ->
                when (filter) {
                    ChapterFilter.ALL -> stringResource(R.string.detail_filter_all, totalChapterCount)
                    ChapterFilter.UNREAD -> stringResource(R.string.detail_filter_unread, unreadChapterCount)
                    ChapterFilter.DOWNLOADED -> stringResource(R.string.detail_filter_downloaded, downloadedChapterCount)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )
    }
}

/**
 * Kolom pencarian chapter dengan auto-scroll dan pelacakan fokus keyboard.
 */
@Composable
internal fun ChapterSearchBar(
    chapterQuery: String,
    onChapterQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = chapterQuery,
            onValueChange = onChapterQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.detail_chapter_search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = if (chapterQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onChapterQueryChange("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.detail_clear_search),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                },
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = DarkOutline,
                cursorColor = PrimaryPink,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                },
        )
        HorizontalDivider(color = DarkOutline, thickness = StrokeWidth.regular)
    }
}
