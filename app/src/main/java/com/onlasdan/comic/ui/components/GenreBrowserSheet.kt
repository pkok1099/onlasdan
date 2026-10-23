package com.onlasdan.comic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.source.AppErrorHandler
import com.onlasdan.comic.ui.source.Genre
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GenreBrowserSheet(
    sourceId: String,
    onDismiss: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    anchorXFraction: Float = 0.68f,
) {
    NoirMorphingSheet(
        visible = visible,
        onDismiss = onDismiss,
        anchorXFraction = anchorXFraction,
        maxHeight = 460.dp,
        modifier = modifier,
    ) {
        ShimmerScope {
            GenreBrowserSheetContent(
                sourceId = sourceId,
                onDismiss = onDismiss,
                onGenreClick = onGenreClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun GenreBrowserSheetContent(
    sourceId: String,
    onDismiss: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()
    val source = remember(sourceId) { SourceRegistry.get(sourceId) }

    var allGenres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredGenres = remember(searchQuery, allGenres) {
        val q = searchQuery.trim()
        val distinctGenres = allGenres.distinctBy { it.id }
        if (q.isEmpty()) {
            distinctGenres
        } else {
            distinctGenres.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    // Dismiss keyboard otomatis saat grid di-scroll oleh user
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val scrollDismissConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f && source == NestedScrollSource.UserInput) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    fun loadGenres() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val list = withContext(Dispatchers.IO) {
                    source.allGenres()
                }
                allGenres = list.distinctBy { it.id }
            } catch (e: Exception) {
                errorMessage = AppErrorHandler.toUserMessage(e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sourceId) {
        searchQuery = ""
        loadGenres()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .nestedScroll(scrollDismissConnection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                    },
                )
            },
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.genre_browser_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.genre_browser_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
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
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                },
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = DarkOutline,
                cursorColor = PrimaryPink,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(Spacing.lg),
                ) {
                    PlaceholderGrid()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(
                        message = errorMessage ?: "",
                        onRetry = { loadGenres() },
                    )
                }
            }

            allGenres.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(message = stringResource(R.string.common_no_data_found), actionLabel = stringResource(R.string.common_try_again), onAction = { loadGenres() })
                }
            }

            filteredGenres.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(message = stringResource(R.string.genre_empty_search), actionLabel = stringResource(R.string.search_reset), onAction = { searchQuery = "" })
                }
            }

            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.xs,
                        bottom = Spacing.xl,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(
                        items = filteredGenres,
                        key = { it.id },
                        contentType = { "genre_browser_card" },
                    ) { genre ->
                        GenreBrowserCard(
                            genre = genre,
                            onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                                onGenreClick(genre)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreBrowserCard(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CardShape,
        color = DarkSurfaceElevated,
        border = BorderStroke(StrokeWidth.regular, DarkOutline),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = genre.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val count = genre.count
            if (count != null && count > 0) {
                Text(
                    text = stringResource(R.string.genre_title_count, count),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AccentPink,
                    maxLines = 1,
                )
            }
        }
    }
}
