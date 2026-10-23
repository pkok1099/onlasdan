package com.onlasdan.comic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.reflect.TypeToken
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.LocalBottomBarVisible
import com.onlasdan.comic.ui.LocalOnOpenDownloads
import com.onlasdan.comic.ui.components.*
import com.onlasdan.comic.ui.source.*
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.bindAutoHideOnScroll
import com.onlasdan.comic.ui.util.pressScale
import com.onlasdan.comic.ui.util.staggeredEntrance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    sourceId: String,
    onComicClick: (comicId: String, sourceId: String) -> Unit,
    onBrowseClick: (filters: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    onGenreClick: (Genre, String) -> Unit = { _, _ -> },
    onGenreBrowse: (genreSlug: String, sourceId: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = remember(sourceId) { SourceRegistry.get(sourceId) }
    // Latest source for currency checks inside in-flight loads: a fetch that
    // finishes after the source changed must never write its (stale) sections.
    val currentSource by rememberUpdatedState(source)
    val bottomBarVisible = LocalBottomBarVisible.current
    val listState = rememberLazyListState()
    bindAutoHideOnScroll(listState = listState, bottomBarVisible = bottomBarVisible)

    var sections by remember { mutableStateOf<List<Section>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDebugMessage by remember { mutableStateOf<String?>(null) }
    var errorIsOffline by remember { mutableStateOf(false) }
    var entranceDone by remember { mutableStateOf(false) }
    var showGenreBrowserSheet by remember { mutableStateOf(false) }
    var sectionLayoutMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loadGeneration by remember { mutableIntStateOf(0) }

    // Runs in the caller's scope: LaunchedEffect cancels the previous load when
    // sourceId changes, so a slow stale fetch can no longer land last and
    // overwrite the new source's sections (the post-update Frankenstein home).
    suspend fun loadHome() {
        val requestSource = source
        val generation = ++loadGeneration // token generasi (BUG-5): finally muatan batal tak mematikan isLoading muatan baru
        isLoading = true
        errorMessage = null
        errorDebugMessage = null
        try {
            val result = SourceCache.getOrFetch(
                key = "${requestSource.id}:home_sections",
                // getParameterized: builds the type programmatically instead of relying on
                // generic signatures, which R8 strips from anonymous TypeToken subclasses.
                type = TypeToken.getParameterized(List::class.java, Section::class.java).type,
                ttlMs = 5 * 60 * 1000L,
            ) { requestSource.home() }
            if (requestSource.id != currentSource.id) return
            sections = result
            delay(300)
            entranceDone = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = AppErrorHandler.toUserMessage(e)
            errorDebugMessage = e.message
            errorIsOffline = AppErrorHandler.isOfflineError(e)
            android.util.Log.e("HomeScreen", "loadHome gagal (${requestSource.id})", e)
        } finally {
            if (generation == loadGeneration) isLoading = false
        }
    }

    LaunchedEffect(sourceId) {
        entranceDone = false
        sections = emptyList()
        loadHome()
    }

    // Degradasi anggun (Fase C): refresh gagal -> konten dipertahankan + toast.
    LaunchedEffect(errorMessage) {
        if (errorMessage != null && sections.isNotEmpty()) AppToast.error(checkNotNull(errorMessage))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack),
    ) {
        if (isLoading && sections.isEmpty()) {
            // Full-block skeleton shaped by THIS source's home layout (homeLayout
            // data): hero slab, then one placeholder block per section in the
            // source's real order and form — every source's layout differs.
            val firstRowIndex = source.homeLayout.indexOf(SectionType.ROW)
            ShimmerScope {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    source.homeLayout.forEachIndexed { index, type ->
                        item(key = "home_skel_${index}_$type") {
                            when (type) {
                                SectionType.HERO ->
                                    if (source.capabilities.contains(Capability.MANGA_FEED_LAYOUT)) {
                                        ShinigamiHeroPlaceholder()
                                    } else {
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                            shape = CardShape,
                                        )
                                    }

                                SectionType.ROW ->
                                    if (source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) && index == firstRowIndex) {
                                        RecommendationsPlaceholder()
                                    } else {
                                        PlaceholderRow(cardWidth = 130.dp)
                                    }

                                SectionType.RANKED -> RankedPlaceholder()

                                SectionType.GRID ->
                                    when {
                                        source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) -> ShinigamiUpdatePlaceholder()
                                        source.capabilities.contains(Capability.SECTION_BANNERS) -> BannerGridPlaceholder()
                                        else -> PlaceholderGrid(rows = 2)
                                    }
                            }
                        }
                    }
                }
            }
        } else if (errorMessage != null && sections.isEmpty()) {
            ErrorState(
                message = errorMessage ?: "Gagal memuat feed beranda",
                onRetry = { scope.launch { loadHome() } },
                modifier = Modifier.align(Alignment.Center),
                debugMessage = errorDebugMessage,
                actionLabel = if (errorIsOffline) stringResource(R.string.common_open_downloads) else null,
                onAction = if (errorIsOffline) LocalOnOpenDownloads.current else null,
            )
        } else if (sections.isEmpty() && !isLoading) {
            EmptyState(
                message = stringResource(R.string.home_empty_message),
                actionLabel = stringResource(R.string.common_try_again),
                onAction = { scope.launch { loadHome() } },
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                sections.forEach { section ->
                    when (section.type) {
                        SectionType.HERO -> {
                            if (source.capabilities.contains(Capability.MANGA_FEED_LAYOUT)) {
                                if (section.items.isNotEmpty()) {
                                    item(key = "shinigami_hero_${section.id}") {
                                        ShinigamiHeroCarousel(
                                            comics = section.items,
                                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                        )
                                    }
                                }
                            } else if (source.capabilities.contains(Capability.HERO_CAROUSEL)) {
                                if (section.items.isNotEmpty()) {
                                    item(key = "hero_carousel_${section.id}") {
                                        HeroCarousel(
                                            comics = section.items,
                                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                        )
                                    }
                                }
                            } else {
                                val heroComic = section.items.firstOrNull()
                                if (heroComic != null) {
                                    item(key = "hero_${heroComic.id}") {
                                        HomeHeroBanner(
                                            comic = heroComic,
                                            onClick = { onComicClick(heroComic.id, heroComic.sourceId ?: source.id) },
                                        )
                                    }
                                }
                            }
                        }

                        SectionType.ROW -> {
                            if (section.items.isNotEmpty()) {
                                if (source.capabilities.contains(Capability.MANGA_FEED_LAYOUT) && section.id == "recommendations") {
                                    item(key = "shinigami_recs_${section.id}") {
                                        ShinigamiRecommendationsSection(
                                            comics = section.items,
                                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                            onLoadFormat = { fmt ->
                                                source.sectionFeed("recommendations", fmt, 1).items
                                            },
                                        )
                                    }
                                } else {
                                    item(key = "row_${section.id}") {
                                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                            SectionHeader(
                                                title = section.title ?: "Series Populer",
                                                actionText = if (section.hasMore) stringResource(R.string.home_action_see_all) else null,
                                                onActionClick = { onBrowseClick(section.browseFilters) },
                                            )
                                            ComicRow(
                                                comics = section.items,
                                                onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                                cardWidth = 130.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SectionType.RANKED -> {
                            if (section.items.isNotEmpty()) {
                                item(key = "ranked_${section.id}") {
                                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                        SectionHeader(
                                            title = section.title ?: "Peringkat",
                                            actionText = if (section.hasMore) stringResource(R.string.home_action_see_all) else null,
                                            onActionClick = { onBrowseClick(section.browseFilters) },
                                        )
                                        RankedComicList(
                                            comics = section.items,
                                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                        )
                                    }
                                }
                            }
                        }

                        SectionType.GRID -> {
                            if (section.items.isNotEmpty()) {
                                val useMangaFeed = source.capabilities.contains(Capability.MANGA_FEED_LAYOUT)
                                val useDoujinBanner = source.capabilities.contains(Capability.SECTION_BANNERS)
                                if (useMangaFeed) {
                                    item(key = "shinigami_grid_${section.id}") {
                                        ShinigamiUpdateSection(
                                            comics = section.items,
                                            onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                            onSeeAllClick = { onBrowseClick(section.browseFilters) },
                                            onLoadUpdates = { type, page ->
                                                source.sectionFeed("latest", type, page)
                                            },
                                            entranceDone = entranceDone,
                                        )
                                    }
                                } else {
                                    item(key = "grid_${section.id}") {
                                        val isGrid = sectionLayoutMap[section.id] ?: true
                                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                            if (useDoujinBanner) {
                                                DoujinSectionBanner(
                                                    title = section.title ?: "Update Terbaru",
                                                    isGrid = isGrid,
                                                    onToggleLayout = {
                                                        sectionLayoutMap = sectionLayoutMap + (section.id to !isGrid)
                                                    },
                                                )
                                                if (isGrid) {
                                                    val rows = remember(section.items) { section.items.chunked(3) }
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = Spacing.lg),
                                                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                                                    ) {
                                                        rows.forEachIndexed { rIdx, rowItems ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                                            ) {
                                                                rowItems.forEachIndexed { cIdx, comic ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .staggeredEntrance(0, entranceDone),
                                                                    ) {
                                                                        DoujinComicCard(
                                                                            comic = comic,
                                                                            onClick = { onComicClick(comic.id, comic.sourceId ?: source.id) },
                                                                        )
                                                                    }
                                                                }
                                                                val remainder = 3 - rowItems.size
                                                                if (remainder > 0) {
                                                                    repeat(remainder) {
                                                                        Spacer(modifier = Modifier.weight(1f))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = Spacing.lg),
                                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                                    ) {
                                                        section.items.forEachIndexed { idx, comic ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .staggeredEntrance(0, entranceDone),
                                                            ) {
                                                                DoujinComicListItem(
                                                                    comic = comic,
                                                                    onClick = { onComicClick(comic.id, comic.sourceId ?: source.id) },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                // Tombol "Lihat Semua" ala web doujin.desu.xxx
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = Spacing.sm),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    TextButton(onClick = { onBrowseClick(section.browseFilters) }) {
                                                        Text(
                                                            text = stringResource(R.string.home_action_see_all),
                                                            color = AccentPink,
                                                            style = MaterialTheme.typography.labelLarge,
                                                        )
                                                    }
                                                }
                                            } else {
                                                SectionHeader(
                                                    title = section.title ?: "Update Terbaru",
                                                    actionText = if (section.hasMore) stringResource(R.string.home_action_see_all) else null,
                                                    onActionClick = { onBrowseClick(section.browseFilters) },
                                                )
                                                ComicGrid(
                                                    comics = section.items,
                                                    onComicClick = { c -> onComicClick(c.id, c.sourceId ?: source.id) },
                                                    columns = 3,
                                                    entranceDone = entranceDone,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section "Jelajahi Genre" ala web — muncul hanya bila source
                // mendeklarasikan featuredGenres (data-driven, tanpa cabang per source).
                val featuredGenres = source.featuredGenres
                if (featuredGenres.isNotEmpty()) {
                    item(key = "genre_explorer") {
                        val hasGenreBrowser = source.capabilities.contains(Capability.GENRE_BROWSER)
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            SectionHeader(
                                title = stringResource(R.string.home_genre_explorer),
                                actionText = if (hasGenreBrowser) stringResource(R.string.home_action_see_all) else null,
                                onActionClick = {
                                    if (hasGenreBrowser) {
                                        showGenreBrowserSheet = true
                                    } else {
                                        onBrowseClick(emptyMap())
                                    }
                                },
                            )
                            GenreExplorerGrid(
                                genres = featuredGenres,
                                onGenreClick = { g ->
                                    if (hasGenreBrowser) {
                                        onGenreBrowse(g.id, source.id)
                                    } else {
                                        onGenreClick(g, source.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (showGenreBrowserSheet) {
            GenreBrowserSheet(
                sourceId = source.id,
                onDismiss = { showGenreBrowserSheet = false },
                onGenreClick = { genre ->
                    showGenreBrowserSheet = false
                    onGenreBrowse(genre.id, source.id)
                },
            )
        }
    }
}

@Composable
fun HomeHeroBanner(
    comic: Comic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val imageRequest = remember(comic.cover, context) {
        ImageRequest.Builder(context)
            .data(comic.cover)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .clip(CardShape)
            .background(DarkSurfaceElevated)
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = comic.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Elegant Noir Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeHeroGradient),
        )

        // Bottom text and CTA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!comic.type.isNullOrBlank()) {
                    AppChip(text = comic.type)
                }
                if (!comic.rating.isNullOrBlank()) {
                    Surface(
                        shape = ChipShape,
                        color = DarkSurface.copy(alpha = 0.85f),
                        border = BorderStroke(StrokeWidth.regular, PrimaryPink.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(10.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Text(
                                text = comic.rating,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentPink,
                            )
                        }
                    }
                }
                if (!comic.latestChapter.isNullOrBlank()) {
                    AppChip(text = comic.latestChapter)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = comic.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!comic.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = comic.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            PrimaryButton(
                text = "Baca Sekarang",
                icon = Icons.Default.PlayArrow,
                onClick = onClick,
                modifier = Modifier.height(42.dp),
            )
        }
    }
}
