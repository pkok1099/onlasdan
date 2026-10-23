package com.onlasdan.comic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.onlasdan.comic.R
import com.onlasdan.comic.api.*
import com.onlasdan.comic.ui.LocalErrorBackAction
import com.onlasdan.comic.ui.LocalOnOpenDownloads
import com.onlasdan.comic.ui.source.AppErrorHandler
import com.onlasdan.comic.ui.source.Comic
import com.onlasdan.comic.ui.theme.BlushTint
import com.onlasdan.comic.ui.theme.CardShape
import com.onlasdan.comic.ui.theme.DarkOutline
import com.onlasdan.comic.ui.theme.NoirInputBg
import com.onlasdan.comic.ui.theme.NoirSurface
import com.onlasdan.comic.ui.theme.NoirText
import com.onlasdan.comic.ui.theme.SoftBlush
import com.onlasdan.comic.ui.theme.StrokeWidth
import com.onlasdan.comic.ui.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun <T> ComicDataScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    fetchData: suspend (String) -> T,
    cacheKey: String? = null,
    cacheTtlMs: Long = 10 * 60 * 1000L,
    loadingContent: @Composable () -> Unit = { PlaceholderGrid() },
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (T) -> Unit,
) {
    val source = rememberSelectedSource()
    val key = remember(cacheKey, source) { cacheKey?.let { "$it:$source" } }
    var dataState by remember(key) { mutableStateOf(key?.let { com.onlasdan.comic.ui.util.UiMemoryCache.peek<T>(it) }) }
    var isLoading by remember { mutableStateOf(dataState == null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorDebug by remember { mutableStateOf<String?>(null) }
    var errorIsOffline by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey, source) {
        val hasData = dataState != null
        isLoading = !hasData
        isRefreshing = hasData

        // If cached and still fresh, skip fetching.
        if (reloadKey == 0 && key != null && com.onlasdan.comic.ui.util.UiMemoryCache.isFresh(key) && dataState != null) {
            isLoading = false
            isRefreshing = false
            return@LaunchedEffect
        }

        val result = runCatching { fetchData(source) }
        result.onSuccess { value ->
            dataState = value
            error = null
            if (key != null) {
                com.onlasdan.comic.ui.util.UiMemoryCache.put(key, value as Any, cacheTtlMs)
            }
        }
        result.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Tanpa data: state error ramah; data lama ada: toast saja (Fase C).
            if (dataState == null) {
                error = AppErrorHandler.toUserMessage(e)
                errorDebug = e.message
                errorIsOffline = AppErrorHandler.isOfflineError(e)
            } else {
                AppToast.error(AppErrorHandler.toUserMessage(e))
            }
        }
        isLoading = false
        isRefreshing = false
    }

    Scaffold(
        topBar = { onBack?.let { SimpleTopBar(title, it) } },
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            when {
                isLoading && dataState == null -> {
                    loadingContent()
                }

                dataState == null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    ErrorState(
                        message = error ?: stringResource(R.string.common_failed_load),
                        onRetry = { reloadKey++ },
                        debugMessage = errorDebug,
                        actionLabel = if (errorIsOffline) stringResource(R.string.common_open_downloads) else null,
                        onAction = if (errorIsOffline) LocalOnOpenDownloads.current else null,
                    )
                    val errorBack = LocalErrorBackAction.current
                    if (errorBack != null) {
                        TextButton(onClick = errorBack) { Text(stringResource(R.string.common_back)) }
                    }
                }

                else -> {
                    var shown by remember { mutableStateOf(false) }
                    SideEffect { shown = true }
                    val contentAlpha by animateNoirFloatAsState(
                        targetValue = if (shown) 1f else 0f,
                        animationSpec = NoirMotion.effectsSpring(),
                        label = "contentFade",
                    )
                    val contentScale by animateNoirFloatAsState(
                        targetValue = if (shown) 1f else 0.98f,
                        animationSpec = NoirMotion.spatialSpring(),
                        label = "contentScale",
                    )
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        },
                    ) {
                        dataState?.let { content(it) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    compact: Boolean = false,
) {
    TopAppBar(
        modifier = Modifier
            .statusBarsPadding()
            .then(if (compact) Modifier.height(56.dp) else Modifier),
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = if (onBack != null) {
            {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        } else {
            {}
        },
        actions = actions,
    )
}

@Composable
fun ComicGrid(comics: List<com.onlasdan.comic.api.HomeComicItem>, onComicClick: (String) -> Unit, columns: Int, isHd: Boolean) {
    LatestGrid(comics, onComicClick, columns, isHd)
}

@Composable
fun HomeComicCard(
    title: String,
    cover: String?,
    type: String?,
    latestChapter: String?,
    sharedKey: String? = null,
    slug: String? = null,
    sourceId: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val cardHoldHandler = com.onlasdan.comic.ui.LocalCardHoldHandler.current
    val activeCardHold = com.onlasdan.comic.ui.LocalActiveCardHoldTarget.current
    val cardBounds = remember { CardBoundsHolder() }
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width
    val fallbackSource = rememberSelectedSource()
    val effectiveSourceId = if (!sourceId.isNullOrBlank()) sourceId else fallbackSource
    val resolvedSlug = slug ?: sharedKey?.removePrefix("cover-")?.takeIf { it.isNotBlank() }

    val effectiveLongClick = onLongClick ?: if (!resolvedSlug.isNullOrBlank()) {
        {
            val col = when {
                cardBounds.bounds.center.x < screenWidthPx * 0.38f -> 0
                cardBounds.bounds.center.x < screenWidthPx * 0.70f -> 1
                else -> 2
            }
            cardHoldHandler(
                CardHoldTarget(
                    slug = resolvedSlug,
                    sourceId = effectiveSourceId,
                    title = title,
                    coverUrl = cover,
                    originBounds = cardBounds.bounds,
                    columnIndex = col,
                ),
            )
        }
    } else {
        null
    }

    val isBeingMorphed = activeCardHold != null &&
        !resolvedSlug.isNullOrBlank() &&
        activeCardHold.slug == resolvedSlug &&
        (activeCardHold.sourceId == effectiveSourceId || activeCardHold.sourceId.isBlank())

    val context = LocalContext.current
    val imageRequest = remember(cover) {
        ImageRequest.Builder(context).data(cover).crossfade(true).build()
    }
    var imgLoaded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isBeingMorphed) 0f else 1f
            }
            .noirCombinedClickable(
                pressedScale = 0.93f,
                onLongClick = effectiveLongClick,
                onClick = {
                    if (sharedKey != null) CoverTransitionState.activeKey = sharedKey
                    onClick()
                },
            ),
    ) {
        Box(
            Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(BorderStroke(StrokeWidth.hairline, DarkOutline), RoundedCornerShape(12.dp))
                .coverSharedElement(sharedKey)
                .onGloballyPositioned { coordinates ->
                    cardBounds.bounds = coordinates.boundsInRoot()
                },
        ) {
            if (!imgLoaded) {
                ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp))
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                onState = { imgLoaded = it is coil.compose.AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            type?.let {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(0.9f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground, lineHeight = 16.sp)
        latestChapter?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun PopularRow(comics: List<HomeComicItem>, isHd: Boolean, onComicClick: (String) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(
            items = comics,
            key = { it.url ?: it.title },
            contentType = { "popular_item" },
        ) { comic ->
            Box(Modifier.width(160.dp).animateItem()) {
                HomeComicCard(
                    comic.title,
                    getImageUrl(comic.cover, isHd),
                    comic.type,
                    comic.latestChapter?.title,
                    sharedKey = "cover-${extractSlug(comic.url)}",
                ) { onComicClick(extractSlug(comic.url)) }
            }
        }
    }
}

@Composable
fun LatestGrid(comics: List<HomeComicItem>, onComicClick: (String) -> Unit, columns: Int, isHd: Boolean) {
    var entranceDone by remember(comics) { mutableStateOf(false) }
    LaunchedEffect(comics) {
        delay(MotionTokens.StaggerCap * MotionTokens.StaggerStepMs)
        entranceDone = true
    }
    val rows = remember(comics, columns) { comics.chunked(columns) }
    Column {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { comic ->
                    Box(Modifier.weight(1f).staggeredEntrance(0, entranceDone)) {
                        HomeComicCard(
                            comic.title,
                            getImageUrl(comic.cover, isHd),
                            comic.type,
                            comic.latestChapter?.title,
                            sharedKey = "cover-${extractSlug(comic.url)}",
                        ) { onComicClick(extractSlug(comic.url)) }
                    }
                }
                if (row.size < columns) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Extension LazyListScope untuk merender grid Comic (model ui.source) secara virtualized.
 */
fun androidx.compose.foundation.lazy.LazyListScope.homeComicGridItems(
    comics: List<Comic>,
    columns: Int,
    isHd: Boolean,
    onComicClick: (String) -> Unit,
) {
    val rows = comics.chunked(columns)
    itemsIndexed(
        items = rows,
        key = { idx, row -> "${idx}_${row.firstOrNull()?.id ?: row.firstOrNull()?.title.orEmpty()}" },
        contentType = { _, _ -> "home_comic_row" },
    ) { _, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { comic ->
                Box(Modifier.weight(1f)) {
                    HomeComicCard(
                        comic.title,
                        getImageUrl(comic.cover, isHd),
                        comic.type,
                        comic.latestChapter,
                        sharedKey = "cover-${comic.id}",
                    ) { onComicClick(comic.id) }
                }
            }
            if (row.size < columns) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun AnimatedVisibilityScope.ScreenTransition(content: @Composable () -> Unit) {
    // Publish this destination's AnimatedVisibilityScope so descendants can
    // participate in shared-element transitions. Called inside navigation
    // composable() lambdas, whose receiver is the AnimatedVisibilityScope.
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutAndHdRow(isHd: Boolean, onToggleHd: () -> Unit, columns: Int, onColumnsChange: (Int) -> Unit, onViewAll: (() -> Unit)? = null) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(true) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = isHd,
                    onClick = onToggleHd,
                    label = { Text(stringResource(R.string.common_hd_mode), style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.height(32.dp),
                )
                IconButton(
                    onClick = {
                        focusManager.clearFocus(true)
                        onColumnsChange(if (columns < 4) columns + 1 else 2)
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    val icon = when (columns) {
                        2 -> Icons.Default.ViewComfy
                        3 -> Icons.Default.ViewModule
                        else -> Icons.Default.GridView
                    }
                    Icon(icon, contentDescription = stringResource(R.string.common_change_grid))
                }
            }
            onViewAll?.let {
                Spacer(Modifier.width(6.dp))
                TextButton(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), onClick = it) {
                    Text(stringResource(R.string.common_view_all), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ExploreGenresCard(onBrowseGenres: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.genres_explore_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.genres_explore_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBrowseGenres) { Text(stringResource(R.string.genres_browse_all)) }
        }
    }
}
