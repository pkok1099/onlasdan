package com.onlasdan.comic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.AppErrorHandler
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.source.Genre
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * Bottom sheet daftar komik untuk satu genre.
 * Sumber data generik: source.search("", {"genre": id}) — tanpa cabang per source.
 */
sealed interface GenreSheetUiState {
    data object Loading : GenreSheetUiState
    data class Content(val items: List<Comic>, val page: Int, val hasNextPage: Boolean, val loadingMore: Boolean) : GenreSheetUiState
    data class Error(val message: String) : GenreSheetUiState
    data object Empty : GenreSheetUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreSheet(
    genre: Genre,
    sourceId: String,
    onDismiss: () -> Unit,
    onComicClick: (comicId: String, sourceId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val source = remember(sourceId) { SourceRegistry.get(sourceId) }
    var uiState by remember(sourceId, genre.id) { mutableStateOf<GenreSheetUiState>(GenreSheetUiState.Loading) }

    fun load(page: Int, append: Boolean) {
        scope.launch {
            if (!append) {
                uiState = GenreSheetUiState.Loading
            } else {
                (uiState as? GenreSheetUiState.Content)?.let {
                    uiState = it.copy(loadingMore = true)
                }
            }
            try {
                val result = source.search(
                    query = "",
                    filters = mapOf("genre" to genre.id),
                    page = page,
                )
                val merged = if (append) {
                    val prev = (uiState as? GenreSheetUiState.Content)?.items.orEmpty()
                    (prev + result.items).distinctBy { it.id }
                } else {
                    result.items
                }
                uiState = if (merged.isEmpty()) {
                    GenreSheetUiState.Empty
                } else {
                    GenreSheetUiState.Content(
                        items = merged,
                        page = page,
                        hasNextPage = result.hasNextPage,
                        loadingMore = false,
                    )
                }
            } catch (e: Exception) {
                if (!append) {
                    uiState = GenreSheetUiState.Error(AppErrorHandler.toUserMessage(e))
                } else {
                    (uiState as? GenreSheetUiState.Content)?.let {
                        uiState = it.copy(loadingMore = false)
                    }
                }
            }
        }
    }

    fun reload() = load(page = 1, append = false)

    LaunchedEffect(genre.id, sourceId) { reload() }

    NoirBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(
                    horizontal = Spacing.lg,
                    vertical = Spacing.md,
                ),
            )
            when (val state = uiState) {
                GenreSheetUiState.Loading -> PlaceholderGrid()

                is GenreSheetUiState.Content -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        overscrollEffect = null,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        comicGridItems(
                            comics = state.items,
                            columns = 3,
                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: sourceId) },
                        )
                        if (state.hasNextPage) {
                            item(key = "genre_load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SecondaryButton(
                                        text = stringResource(R.string.browse_btn_load_more),
                                        onClick = { load(state.page + 1, append = true) },
                                        enabled = !state.loadingMore,
                                    )
                                }
                            }
                        }
                    }
                }

                is GenreSheetUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { reload() },
                )

                GenreSheetUiState.Empty -> EmptyState(
                    message = stringResource(R.string.browse_empty_result),
                    actionLabel = stringResource(R.string.common_try_again),
                    onAction = { reload() },
                )
            }
        }
    }
}
