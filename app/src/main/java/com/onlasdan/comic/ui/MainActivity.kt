package com.onlasdan.comic.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.chrome.ComicSearchSheet
import com.onlasdan.comic.ui.chrome.ComicSearchSheetContent
import com.onlasdan.comic.ui.chrome.LocalChromeContext
import com.onlasdan.comic.ui.chrome.UnifiedMorphingDock
import com.onlasdan.comic.ui.components.*
import com.onlasdan.comic.ui.screens.*
import com.onlasdan.comic.ui.screens.browse.BrowseScreen
import com.onlasdan.comic.ui.screens.sourcebrowse.SourceBrowseScreen
import com.onlasdan.comic.ui.source.Genre
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.BackupSummary
import com.onlasdan.comic.ui.util.DatabaseBackupStore
import com.onlasdan.comic.ui.util.MotionTokens
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalBottomBarVisible = staticCompositionLocalOf<MutableState<Boolean>> {
    error("LocalBottomBarVisible not provided")
}

val LocalCardHoldHandler = staticCompositionLocalOf<(CardHoldTarget) -> Unit> { {} }
val LocalActiveCardHoldTarget = staticCompositionLocalOf<CardHoldTarget?> { null }

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Browse : Screen("browse?mode=browse", "Browse", Icons.Default.Search)
    object Genres : Screen("genres", "Genres", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile?tab=bookmark", "Profile", Icons.Default.Person)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.attributes.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ComicReaderTheme { AppRoot() }
        }
    }
}

@Composable
@Suppress("kotlin:S3776")
internal fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val selectedSource by mainViewModel.selectedSource.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val restoreSuccessText = stringResource(R.string.storage_restore_success)
    val restoreFailedText = stringResource(R.string.storage_restore_failed)
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("comic_prefs", android.content.Context.MODE_PRIVATE) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("storage_onboarding_done", false)) }
    // Cek "sudah ada data" berjalan async (bug BUG-3): membuka SQLite di komposisi
    // memblokir main thread; gagal baca (null) dianggap ada data — aman.
    LaunchedEffect(Unit) {
        if (!onboardingDone && withContext(Dispatchers.IO) { DatabaseBackupStore.isLogicallyEmpty(context) } != true) {
            onboardingDone = true
        }
    }
    var showRestorePrompt by remember { mutableStateOf(false) }
    var backupSummaryToRestore by remember { mutableStateOf<BackupSummary?>(null) }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            DatabaseBackupStore.setCustomTreeUri(context, uri)
            DatabaseBackupStore.setBackupEnabled(context, true)
            onboardingDone = true
            prefs.edit { putBoolean("storage_onboarding_done", true) }

            scope.launch {
                val summary = DatabaseBackupStore.inspectBackup(context, uri)
                if (summary != null && (summary.bookmarksCount > 0L || summary.historyCount > 0L || summary.readCount > 0L)) {
                    backupSummaryToRestore = summary
                    showRestorePrompt = true
                } else {
                    DatabaseBackupStore.exportDb(context)
                }
            }
        }
    }

    var isHd by rememberSaveable { mutableStateOf(false) }
    var layoutColumns by rememberSaveable { mutableIntStateOf(3) }
    var sheetComicId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var genreSheet by remember { mutableStateOf<Pair<Genre, String>?>(null) }
    var showSourceSwitcherSheet by remember { mutableStateOf(false) }
    var cardHoldTarget by remember { mutableStateOf<CardHoldTarget?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PureBlack,
        contentColor = TextPrimary,
    ) {
        when {
            !onboardingDone -> {
                StorageOnboardingPage(
                    onEnable = { pickFolderLauncher.launch(null) },
                    onSkip = {
                        DatabaseBackupStore.setBackupEnabled(context, false)
                        onboardingDone = true
                        prefs.edit { putBoolean("storage_onboarding_done", true) }
                    },
                )
                return@Surface
            }

            showRestorePrompt -> {
                StorageRestorePage(
                    summary = backupSummaryToRestore,
                    onRestore = {
                        showRestorePrompt = false
                        scope.launch {
                            val result = DatabaseBackupStore.restoreDb(context)
                            if (result.isSuccess) {
                                AppToast.success(restoreSuccessText)
                            } else {
                                AppToast.error(restoreFailedText)
                            }
                        }
                    },
                    onSkip = {
                        showRestorePrompt = false
                        scope.launch { DatabaseBackupStore.exportDb(context) }
                    },
                )
                return@Surface
            }
        }

        val navController = rememberNavController()
        val bottomBarVisible = rememberSaveable { mutableStateOf(true) }

        BackHandler {
            val popped = navController.popBackStack()
            if (!popped) (context as? ComponentActivity)?.finish()
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val items = remember { listOf(Screen.Home, Screen.Browse, Screen.Genres, Screen.Profile) }

        LaunchedEffect(currentDestination?.route) {
            val route = currentDestination?.route
            if (route != null && (items.any { it.route == route } || route == "settings" || route.startsWith("genre") || route.startsWith("profile"))) {
                bottomBarVisible.value = true
            }
        }

        CompositionLocalProvider(
            LocalBottomBarVisible provides bottomBarVisible,
            LocalCardHoldHandler provides { cardHoldTarget = it },
            LocalActiveCardHoldTarget provides cardHoldTarget,
        ) {
            val chromeContextState = remember { mutableStateOf<com.onlasdan.comic.ui.chrome.ChromeContext?>(null) }
            CompositionLocalProvider(LocalChromeContext provides chromeContextState) {
                val windowInfo = LocalWindowInfo.current
                val density = LocalDensity.current
                val showSidebar = remember(windowInfo.containerSize.width, density) {
                    with(density) { windowInfo.containerSize.width.toDp() >= 700.dp }
                }

                val chromeRoutes = remember {
                    setOf(
                        "home",
                        "browse?mode={mode}",
                        "search",
                        "genres",
                        "genre/{slug}",
                        "profile?tab={tab}",
                        "settings",
                        "projects",
                    )
                }
                val shouldShowChrome = remember(currentDestination?.route) {
                    val route = currentDestination?.route
                    route != null && chromeRoutes.contains(route)
                }
                val isReaderDestination = currentDestination?.route?.startsWith("read/") == true
                var searchOpen by rememberSaveable { mutableStateOf(false) }
                var genreBrowserOpen by rememberSaveable { mutableStateOf(false) }
                var profileSheetOpen by rememberSaveable { mutableStateOf(false) }

                var searchAnchorX by remember { mutableFloatStateOf(0.15f) }
                var genreAnchorX by remember { mutableFloatStateOf(0.68f) }
                var profileAnchorX by remember { mutableFloatStateOf(0.88f) }

                fun isSelected(screen: Screen): Boolean {
                    val route = currentDestination?.route ?: return false
                    return when (screen) {
                        Screen.Browse -> route.startsWith("browse")
                        Screen.Genres -> route.startsWith("genre")
                        Screen.Profile -> route.startsWith("profile") || route == "settings"
                        else -> currentDestination.hierarchy.any { it.route == screen.route }
                    }
                }

                val currentSourceObj = remember(selectedSource) { SourceRegistry.get(selectedSource) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top bar on Home & main screens
                        if (shouldShowChrome && !isReaderDestination && currentDestination?.route == "home") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SourceSwitcherTrigger(
                                    currentSource = currentSourceObj,
                                    onClick = { showSourceSwitcherSheet = true },
                                )
                            }
                        }

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (showSidebar && shouldShowChrome) {
                                NavigationRail(
                                    containerColor = DarkSurface,
                                    contentColor = TextPrimary,
                                ) {
                                    Spacer(Modifier.height(Spacing.sm))
                                    items.forEach { screen ->
                                        val selected = isSelected(screen)
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(screen.icon, contentDescription = null, tint = if (selected) PrimaryPink else TextSecondary) },
                                            label = { Text(screen.label) },
                                            alwaysShowLabel = false,
                                        )
                                    }
                                    Spacer(Modifier.height(Spacing.sm))
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                val contentModifier = remember(isReaderDestination, currentDestination?.route) {
                                    if (isReaderDestination || currentDestination?.route == "home") Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding()
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = "home",
                                    modifier = contentModifier,
                                    enterTransition = {
                                        // Depth transition: layar baru muncul dari depan dengan
                                        // bobot (scale + slide vertikal + fade), pegas berat.
                                        fadeIn(NoirMotion.noirEffects()) +
                                            scaleIn(
                                                NoirMotion.noirSpatialHeavy(),
                                                initialScale = 0.92f,
                                            ) +
                                            slideInVertically(NoirMotion.noirSpatialHeavyOffset()) { it / 6 }
                                    },
                                    exitTransition = {
                                        // Layar lama tenggelam ke belakang: mengecil + meredup.
                                        scaleOut(
                                            NoirMotion.noirSpatialHeavy(),
                                            targetScale = 0.94f,
                                        ) +
                                            fadeOut(NoirMotion.noirEffects(), targetAlpha = 0.3f)
                                    },
                                    popEnterTransition = {
                                        fadeIn(NoirMotion.noirEffects()) +
                                            scaleIn(
                                                NoirMotion.noirSpatialHeavy(),
                                                initialScale = 0.94f,
                                            )
                                    },
                                    popExitTransition = {
                                        // Layar atas jatuh/mengecil keluar.
                                        scaleOut(
                                            NoirMotion.noirSpatialHeavy(),
                                            targetScale = 0.90f,
                                        ) +
                                            slideOutVertically(NoirMotion.noirSpatialHeavyOffset()) { it / 5 } +
                                            fadeOut(NoirMotion.noirEffects())
                                    },
                                ) {
                                    composable("home") {
                                        ShimmerScope {
                                            HomeScreen(
                                                sourceId = selectedSource,
                                                onComicClick = { comicId, srcId ->
                                                    sheetComicId = comicId
                                                    sheetSourceId = srcId
                                                },
                                                onBrowseClick = { filters ->
                                                    val query = filters.entries.joinToString("&") { (k, v) -> "$k=$v" }
                                                    val route = if (query.isNotEmpty()) "sourceBrowse/$selectedSource?$query" else "sourceBrowse/$selectedSource"
                                                    navController.navigate(route)
                                                },
                                                onGenreClick = { genre, srcId ->
                                                    genreSheet = genre to srcId
                                                },
                                                onGenreBrowse = { genreSlug, srcId ->
                                                    navController.navigate("sourceBrowse/$srcId?genre=$genreSlug")
                                                },
                                            )
                                        }
                                    }

                                    composable(
                                        "sourceBrowse/{sourceId}?genre={genre}&type={type}",
                                        arguments = listOf(
                                            navArgument("sourceId") { type = NavType.StringType },
                                            navArgument("genre") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                            navArgument("type") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                        ),
                                    ) { backStackEntry ->
                                        val browseSourceId = backStackEntry.arguments?.getString("sourceId")
                                            ?: selectedSource
                                        val genreParam = backStackEntry.arguments?.getString("genre")
                                        val typeParam = backStackEntry.arguments?.getString("type")
                                        val initialFilters = buildMap {
                                            if (!genreParam.isNullOrBlank()) put("genre", genreParam)
                                            if (!typeParam.isNullOrBlank()) put("type", typeParam)
                                        }
                                        ShimmerScope {
                                            SourceBrowseScreen(
                                                sourceId = browseSourceId,
                                                initialFilters = initialFilters,
                                                onComicClick = { comicId, srcId ->
                                                    sheetComicId = comicId
                                                    sheetSourceId = srcId
                                                },
                                                onBack = { navController.popBackStack() },
                                            )
                                        }
                                    }

                                    composable(
                                        "browse?mode={mode}",
                                        arguments = listOf(
                                            navArgument("mode") {
                                                type = NavType.StringType
                                                defaultValue = "browse"
                                            },
                                        ),
                                    ) { backStackEntry ->
                                        val mode = backStackEntry.arguments?.getString("mode") ?: "browse"
                                        if (mode == "updates") {
                                            UpdatesScreen(
                                                source = selectedSource,
                                                onComicClick = { slug ->
                                                    sheetComicId = slug
                                                    sheetSourceId = selectedSource
                                                },
                                                onBack = { navController.popBackStack() },
                                                isHd = isHd,
                                                onToggleHd = { isHd = !isHd },
                                                columns = layoutColumns,
                                                onColumnsChange = { layoutColumns = it },
                                            )
                                        } else {
                                            BrowseScreen(
                                                source = selectedSource,
                                                onComicClick = { slug ->
                                                    sheetComicId = slug
                                                    sheetSourceId = selectedSource
                                                },
                                                onBack = null,
                                                isHd = isHd,
                                                onToggleHd = { isHd = !isHd },
                                                columns = layoutColumns,
                                                onColumnsChange = { layoutColumns = it },
                                                onChooseSource = { showSourceSwitcherSheet = true },
                                            )
                                        }
                                    }

                                    composable("genres") {
                                        GenresListScreen(
                                            onGenreClick = { slug -> navController.navigate("genre/$slug") },
                                            onBack = null,
                                        )
                                    }

                                    composable(
                                        "genre/{slug}",
                                        arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                                    ) { backStackEntry ->
                                        val genreSlug = backStackEntry.arguments?.getString("slug") ?: ""
                                        GenreDetailScreen(
                                            genreSlug = genreSlug,
                                            onComicClick = { slug ->
                                                sheetComicId = slug
                                                sheetSourceId = selectedSource
                                            },
                                            onBack = { navController.popBackStack() },
                                            isHd = isHd,
                                            columns = layoutColumns,
                                        )
                                    }

                                    composable(
                                        "profile?tab={tab}",
                                        arguments = listOf(
                                            navArgument("tab") {
                                                type = NavType.StringType
                                                defaultValue = "bookmark"
                                            },
                                        ),
                                    ) { backStackEntry ->
                                        val tab = backStackEntry.arguments?.getString("tab") ?: "bookmark"
                                        CompositionLocalProvider(
                                            LocalOnExploreHome provides {
                                                navController.navigate("home") {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        ) {
                                            ProfileScreen(
                                                onComicClick = { slug ->
                                                    sheetComicId = slug
                                                    sheetSourceId = selectedSource
                                                },
                                                onHistoryClick = { comicSlug, src, chapterSlug ->
                                                    navController.navigate("read/$src/$comicSlug/$chapterSlug")
                                                },
                                                onSettingsClick = { navController.navigate("settings") },
                                                initialTab = tab,
                                            )
                                        }
                                    }

                                    composable("settings") {
                                        SettingsScreen(onBack = { navController.popBackStack() })
                                    }

                                    composable("projects") {
                                        ProjectsScreen(
                                            onComicClick = { slug ->
                                                sheetComicId = slug
                                                sheetSourceId = selectedSource
                                            },
                                            onBack = { navController.popBackStack() },
                                            isHd = isHd,
                                            columns = layoutColumns,
                                        )
                                    }

                                    val uri = "https://comic-reader.pages.dev"

                                    composable(
                                        "detail/{sourceId}/{slug}",
                                        arguments = listOf(
                                            navArgument("sourceId") { type = NavType.StringType },
                                            navArgument("slug") { type = NavType.StringType },
                                        ),
                                        deepLinks = listOf(navDeepLink { uriPattern = "$uri/{sourceId}/comic/{slug}" }),
                                    ) { backStackEntry ->
                                        val linkSourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
                                        val slug = backStackEntry.arguments?.getString("slug") ?: ""
                                        LaunchedEffect(slug, linkSourceId) {
                                            sheetComicId = slug
                                            // Source dari tautan, bukan source aktif (bug BUG-2); segmen tak dikenal jatuh ke source aktif.
                                            sheetSourceId = linkSourceId.takeIf { SourceRegistry.contains(it) } ?: selectedSource
                                            // Destination ini hanya pemicu deep link: sheet tampil
                                            // di atas layar sebelumnya, bukan layar kosong sendiri.
                                            if (!navController.popBackStack()) {
                                                navController.navigate("home") {
                                                    popUpTo("detail/{sourceId}/{slug}") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    }

                                    composable(
                                        "read/{sourceId}/{slug}/{chapterSlug}",
                                        arguments = listOf(
                                            navArgument("sourceId") { type = NavType.StringType },
                                            navArgument("slug") { type = NavType.StringType },
                                            navArgument("chapterSlug") { type = NavType.StringType },
                                        ),
                                        deepLinks = listOf(
                                            navDeepLink { uriPattern = "$uri/read/{sourceId}/{slug}/{chapterSlug}" },
                                        ),
                                    ) { backStackEntry ->
                                        val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
                                        val slug = backStackEntry.arguments?.getString("slug") ?: ""
                                        val chapterSlug = backStackEntry.arguments?.getString("chapterSlug") ?: ""
                                        ReaderScreen(
                                            sourceId = sourceId,
                                            slug = slug,
                                            chapterSlug = chapterSlug,
                                            onBack = { navController.popBackStack() },
                                            onNavigateChapter = { nextSlug ->
                                                navController.navigate("read/$sourceId/$slug/$nextSlug") {
                                                    popUpTo("read/$sourceId/$slug/$chapterSlug") { inclusive = true }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val isModalOverlayActive = !isReaderDestination && (sheetComicId != null || genreSheet != null || showSourceSwitcherSheet)

                    // ====================================================================
                    // LAYER 2: Scrim / Backdrop Gelap untuk Unified Morphing Sheet
                    // Menembus penuh hingga ke tepi atas dan bawah layar (fillMaxSize)
                    // ====================================================================
                    val sheetProgress = remember { Animatable(0f) }
                    val isScrimVisible by remember(isModalOverlayActive) {
                        derivedStateOf { !isModalOverlayActive && sheetProgress.value > 0.005f }
                    }
                    if (isScrimVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = sheetProgress.value.coerceIn(0f, 1f) }
                                .background(Color.Black.copy(alpha = 0.68f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        searchOpen = false
                                        genreBrowserOpen = false
                                        profileSheetOpen = false
                                    },
                                ),
                        )
                    }

                    // ====================================================================
                    // LAYER 3: UNIFIED MORPHING DOCK (Single Unified Pill-to-Sheet Container)
                    // Pill Dock dan Sheet menyatu dalam satu Surface di Alignment.BottomCenter
                    // ====================================================================
                    if (shouldShowChrome && !isReaderDestination) {
                        val chromeContext = chromeContextState.value
                        val dockSelectedIndex = when {
                            searchOpen || currentDestination?.route == "search" -> 0
                            genreBrowserOpen || isSelected(Screen.Genres) -> 3
                            profileSheetOpen || isSelected(Screen.Profile) -> 4
                            isSelected(Screen.Browse) -> 2
                            isSelected(Screen.Home) -> 1
                            else -> 1
                        }
                        val expandedTabIndex: Int? = when {
                            searchOpen -> 0
                            genreBrowserOpen -> 3
                            profileSheetOpen -> 4
                            else -> null
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isModalOverlayActive) 0f else 1f
                                },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            UnifiedMorphingDock(
                                visible = bottomBarVisible.value,
                                selectedIndex = dockSelectedIndex,
                                expandedTabIndex = expandedTabIndex,
                                isModalOverlayActive = isModalOverlayActive,
                                sheetProgress = sheetProgress,
                                onExpandTab = { index ->
                                    when (index) {
                                        0 -> {
                                            genreBrowserOpen = false
                                            profileSheetOpen = false
                                            searchOpen = true
                                        }

                                        3 -> {
                                            searchOpen = false
                                            profileSheetOpen = false
                                            genreBrowserOpen = true
                                        }

                                        4 -> {
                                            searchOpen = false
                                            genreBrowserOpen = false
                                            profileSheetOpen = true
                                        }
                                    }
                                },
                                onCollapseSheet = {
                                    searchOpen = false
                                    genreBrowserOpen = false
                                    profileSheetOpen = false
                                },
                                onNavigateHome = {
                                    searchOpen = false
                                    genreBrowserOpen = false
                                    profileSheetOpen = false
                                    navController.navigate("home") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateBrowse = {
                                    searchOpen = false
                                    genreBrowserOpen = false
                                    profileSheetOpen = false
                                    navController.navigate("browse?mode=browse") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                maxSheetHeight = 480.dp,
                                sheetContent = { tabIndex ->
                                    when (tabIndex) {
                                        0 -> {
                                            ShimmerScope {
                                                ComicSearchSheetContent(
                                                    source = selectedSource,
                                                    onDismiss = { searchOpen = false },
                                                    onSelectComic = { comicId, srcId ->
                                                        sheetComicId = comicId
                                                        sheetSourceId = srcId
                                                    },
                                                )
                                            }
                                        }

                                        3 -> {
                                            ShimmerScope {
                                                GenreBrowserSheetContent(
                                                    sourceId = selectedSource,
                                                    onDismiss = { genreBrowserOpen = false },
                                                    onGenreClick = { genre ->
                                                        genreBrowserOpen = false
                                                        genreSheet = genre to selectedSource
                                                    },
                                                )
                                            }
                                        }

                                        4 -> {
                                            val openDownloadsAction: () -> Unit = {
                                                navController.navigate("profile?tab=downloads") {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            ProfileBackupSheetContent(
                                                chromeContext = chromeContext?.copy(
                                                    isHd = isHd,
                                                    columns = layoutColumns,
                                                    toggleHd = { isHd = !isHd },
                                                    setColumns = { layoutColumns = it.coerceIn(2, 4) },
                                                    openDownloads = openDownloadsAction,
                                                ) ?: com.onlasdan.comic.ui.chrome.ChromeContext(
                                                    routeTag = com.onlasdan.comic.ui.chrome.ChromeRouteTag.Profile,
                                                    openDownloads = openDownloadsAction,
                                                ),
                                                onDismiss = { profileSheetOpen = false },
                                                onNavigateBookmarks = {
                                                    navController.navigate("profile?tab=bookmark") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                onNavigateHistory = {
                                                    navController.navigate("profile?tab=history") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                onNavigateProfile = {
                                                    navController.navigate("profile?tab=bookmark") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                onNavigateSettings = {
                                                    navController.navigate("settings") { launchSingleTop = true }
                                                },
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }

                    // ====================================================================
                    // LAYER 4: MODAL OVERLAYS (ComicDetailSheet, GenreSheet, SourceSwitcherSheet)
                    // Dirender setelah UnifiedMorphingDock agar z-index modal selalu di atas.
                    // Disembunyikan saat ReaderScreen aktif tanpa mereset/menghapus state datanya.
                    // ====================================================================
                    if (!isReaderDestination) {
                        val currentComicId = sheetComicId
                        val genreTarget = genreSheet

                        // 1. Comic Detail Drawer Sheet — tampil eksklusif di atas layar.
                        // Jika detail sedang terbuka, genre sheet disembunyikan dari layar tetapi datanya tetap disimpan.
                        if (currentComicId != null) {
                            ShimmerScope {
                                ComicDetailSheet(
                                    comicId = currentComicId,
                                    sourceId = sheetSourceId ?: selectedSource,
                                    onDismiss = { sheetComicId = null },
                                    onGenreClick = { genre ->
                                        sheetComicId = null
                                        genreSheet = genre to (sheetSourceId ?: selectedSource)
                                    },
                                    onReadChapter = { comicId, chapterSlug ->
                                        val src = sheetSourceId ?: selectedSource
                                        // Pertahankan sheetComicId agar saat pengguna Back dari Reader,
                                        // detail komik tetap terbuka dan tidak reload ke Home.
                                        navController.navigate("read/$src/$comicId/$chapterSlug")
                                    },
                                )
                            }
                        } else if (genreTarget != null) {
                            // 2. Genre Sheet — tampil HANYA jika ComicDetailSheet TIDAK sedang terbuka.
                            ShimmerScope {
                                GenreSheet(
                                    genre = genreTarget.first,
                                    sourceId = genreTarget.second,
                                    onDismiss = { genreSheet = null },
                                    onComicClick = { comicId, srcId ->
                                        // Tutup genre sheet dari layar, tapi simpan datanya di genreSheet
                                        // agar saat ComicDetailSheet ditutup, pengguna kembali ke GenreSheet ini.
                                        sheetComicId = comicId
                                        sheetSourceId = srcId
                                    },
                                )
                            }
                        }

                        // 3. Source Switcher Sheet
                        if (showSourceSwitcherSheet) {
                            SourceSwitcherSheet(
                                selectedSourceId = selectedSource,
                                onSourceSelect = { newSource ->
                                    mainViewModel.selectSource(newSource.id)
                                },
                                onDismiss = { showSourceSwitcherSheet = false },
                            )
                        }
                    }

                    // In-App Noir Toast Host
                    NoirToastHost()

                    // Card Hold Contextual In-Place Morphing
                    ComicCardMorphingOverlay(
                        target = cardHoldTarget,
                        onDismiss = { cardHoldTarget = null },
                        onLaunchReader = { slug, chapterSlug, sourceId ->
                            navController.navigate("read/$sourceId/$slug/$chapterSlug")
                        },
                        onOpenDetail = { slug, sourceId ->
                            sheetComicId = slug
                            sheetSourceId = sourceId
                        },
                    )
                }
            }
        }
    }
}
