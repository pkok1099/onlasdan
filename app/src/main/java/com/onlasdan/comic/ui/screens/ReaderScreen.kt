package com.onlasdan.comic.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.onlasdan.comic.R
import com.onlasdan.comic.data.db.ComicDatabase
import com.onlasdan.comic.ui.LocalErrorBackAction
import com.onlasdan.comic.ui.blueprint.BlueprintProvider
import com.onlasdan.comic.ui.components.ComicDataScreen
import com.onlasdan.comic.ui.components.ReaderCanvasPlaceholder
import com.onlasdan.comic.ui.screens.reader.ReaderContent
import com.onlasdan.comic.ui.screens.reader.ReaderExpandedTab
import com.onlasdan.comic.ui.source.SourceRegistry
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.rememberSelectedSource
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun clampAutoScrollSpeed(v: Float): Float = v.coerceIn(10f, 4000f)

@Composable
@Suppress("kotlin:S3776")
fun ReaderScreen(
    sourceId: String,
    slug: String,
    chapterSlug: String,
    onBack: () -> Unit,
    onNavigateChapter: (String) -> Unit,
) {
    val fallbackSource = rememberSelectedSource()
    val source = sourceId.ifBlank { fallbackSource }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("comic_prefs", Context.MODE_PRIVATE) }
    // Tanpa Saver: satu-satunya pemulih posisi adalah ReaderProgressTracker
    // (bug B16) — dua pemulih yang balapan membuat resume tak bisa ditebak.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val blueprint = remember(source) { BlueprintProvider.forSource(source)?.reader?.blueprint }
    if (blueprint == null) {
        BlueprintMissingScreen(source = source, onBack = onBack, onChooseSource = onBack)
        return
    }

    var showControls by rememberSaveable { mutableStateOf(true) }
    var activeExpandedTab by remember { mutableStateOf<ReaderExpandedTab?>(null) }
    val sheetProgress = remember { Animatable(0f) }
    var autoScrollActive by rememberSaveable(slug) { mutableStateOf(prefs.getBoolean("reader_autoscroll_enabled", false)) }
    var autoScrollSpeed by rememberSaveable(slug) { mutableFloatStateOf(prefs.getFloat("reader_autoscroll_speed", 100f)) }
    var autoNextEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("reader_autonext_enabled", true)) }
    var autoNextDelaySeconds by rememberSaveable { mutableFloatStateOf(prefs.getFloat("reader_autonext_delay_seconds", 2.5f)) }
    var autoScrollPausedUntil by rememberSaveable(slug, chapterSlug) { mutableLongStateOf(0L) }
    var immersiveFullscreen by rememberSaveable { mutableStateOf(prefs.getBoolean("reader_immersive_fullscreen", true)) }
    var keepScreenOnEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("reader_keep_screen_on", true)) }
    var seamlessChaptersEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("reader_seamless_chapters_enabled", true)) }
    var nightDimmerAlpha by rememberSaveable { mutableFloatStateOf(prefs.getFloat("reader_night_dimmer", 0f)) }

    LaunchedEffect(seamlessChaptersEnabled) {
        prefs.edit { putBoolean("reader_seamless_chapters_enabled", seamlessChaptersEnabled) }
    }

    LaunchedEffect(nightDimmerAlpha) {
        prefs.edit { putFloat("reader_night_dimmer", nightDimmerAlpha) }
    }

    LaunchedEffect(keepScreenOnEnabled) {
        prefs.edit { putBoolean("reader_keep_screen_on", keepScreenOnEnabled) }
    }

    LaunchedEffect(activeExpandedTab) {
        withContext(NoirForceMotionScale) {
            if (activeExpandedTab != null) {
                sheetProgress.animateTo(1f, NoirMotion.noirSpatialHeavy())
            } else {
                sheetProgress.animateTo(0f, NoirMotion.noirSpatialHeavy())
            }
        }
    }

    BackHandler(enabled = activeExpandedTab != null) {
        activeExpandedTab = null
    }

    LaunchedEffect(showControls) {
        if (!showControls && activeExpandedTab != null) {
            activeExpandedTab = null
        }
    }

    // Halaman yang sudah selesai dimuat untuk sinkronisasi auto-scroll
    val loadedPageSet = remember(slug) { mutableStateMapOf<Int, Boolean>() }
    var settledImageCount by remember(slug) { mutableIntStateOf(0) }
    var isBookmarked by remember { mutableStateOf(false) }

    LaunchedEffect(slug, source) {
        isBookmarked = withContext(Dispatchers.IO) {
            ComicDatabase.get(context).bookmarkDao().exists(slug, source) > 0
        }
    }

    val currentView = LocalView.current
    DisposableEffect(autoScrollActive, keepScreenOnEnabled) {
        val window = context.findActivity()?.window
        val shouldKeepOn = autoScrollActive || keepScreenOnEnabled
        if (shouldKeepOn) {
            currentView.keepScreenOn = true
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            currentView.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            currentView.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val activity = context.findActivity()
    DisposableEffect(immersiveFullscreen, showControls, activeExpandedTab) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (immersiveFullscreen) {
            val shouldShowBars = showControls || activeExpandedTab != null
            if (shouldShowBars) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(activeExpandedTab) {
        if (activeExpandedTab != null) {
            showControls = true
        }
    }

    // Dock HANYA hide otomatis saat auto scroll aktif, dan JANGAN hide jika setting atau chapter terbuka
    LaunchedEffect(showControls, autoScrollActive, activeExpandedTab) {
        if (showControls && autoScrollActive && activeExpandedTab == null) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(autoScrollActive) {
        if (autoScrollActive && activeExpandedTab == null) {
            showControls = false
        }
    }

    LaunchedEffect(autoScrollActive, autoScrollSpeed, slug, activeExpandedTab) {
        if (autoScrollActive) {
            val gateDeadlineMs = System.currentTimeMillis() + 12_000L
            while (autoScrollActive && settledImageCount < 1 &&
                System.currentTimeMillis() < gateDeadlineMs
            ) {
                delay(250)
            }
            var lastFrameTimeNanos = 0L
            while (autoScrollActive) {
                val deltaPx = withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = frameTimeNanos
                        return@withFrameNanos 0f
                    }
                    val elapsedNanos = frameTimeNanos - lastFrameTimeNanos
                    lastFrameTimeNanos = frameTimeNanos

                    val now = System.currentTimeMillis()
                    val isSheetActive = activeExpandedTab != null || sheetProgress.value > 0.01f
                    if (now < autoScrollPausedUntil || listState.isScrollInProgress || isSheetActive) {
                        return@withFrameNanos 0f
                    }

                    // Smart Auto-Scroll Gating
                    val currentVisibleIdx = listState.firstVisibleItemIndex
                    if (loadedPageSet[currentVisibleIdx] != true) {
                        return@withFrameNanos 0f
                    }

                    val safeSpeed = clampAutoScrollSpeed(autoScrollSpeed)
                    (safeSpeed * elapsedNanos / 1_000_000_000f).coerceAtMost(120f)
                }

                if (deltaPx > 0f) {
                    try {
                        listState.scrollBy(deltaPx)
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        autoScrollPausedUntil = System.currentTimeMillis() + 1500
                        autoScrollSpeed = clampAutoScrollSpeed(autoScrollSpeed).coerceAtMost(1500f)
                    }
                }
            }
        }
    }

    LaunchedEffect(autoScrollActive) {
        prefs.edit { putBoolean("reader_autoscroll_enabled", autoScrollActive) }
    }

    LaunchedEffect(autoScrollSpeed) {
        prefs.edit { putFloat("reader_autoscroll_speed", clampAutoScrollSpeed(autoScrollSpeed)) }
    }

    LaunchedEffect(autoNextEnabled) {
        prefs.edit { putBoolean("reader_autonext_enabled", autoNextEnabled) }
    }

    LaunchedEffect(autoNextDelaySeconds) {
        prefs.edit { putFloat("reader_autonext_delay_seconds", autoNextDelaySeconds) }
    }

    CompositionLocalProvider(LocalErrorBackAction provides onBack) {
        ComicDataScreen(
            title = stringResource(R.string.reader_title),
            cacheKey = "reader:$slug:$chapterSlug",
            cacheTtlMs = 30 * 60 * 1000L,
            fetchData = {
                val db = ComicDatabase.get(context)
                val downloadedPages = withContext(Dispatchers.IO) {
                    db.downloadDao().getDownloadedPages(slug, source.lowercase().trim(), chapterSlug)
                }
                if (downloadedPages.isNotEmpty() && downloadedPages.all { File(it.localFilePath).exists() }) {
                    downloadedPages.sortedBy { it.pageIndex }.map { "file://${it.localFilePath}" }
                } else {
                    SourceRegistry.get(source).pages("$slug:$chapterSlug").map { it.imageUrl }
                }
            },
            loadingContent = { ReaderCanvasPlaceholder(modifier = Modifier.fillMaxSize()) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { data ->
            ReaderContent(
                source = source,
                slug = slug,
                chapterSlug = chapterSlug,
                images = data,
                listState = listState,
                context = context,
                scope = scope,
                focusManager = focusManager,
                blueprint = blueprint,
                showControls = showControls,
                onUpdateShowControls = { showControls = it },
                activeExpandedTab = activeExpandedTab,
                onUpdateActiveExpandedTab = { activeExpandedTab = it },
                sheetProgress = sheetProgress,
                autoScrollActive = autoScrollActive,
                onAutoScrollToggle = { autoScrollActive = !autoScrollActive },
                autoScrollSpeed = autoScrollSpeed,
                onAutoScrollSpeedChange = { autoScrollSpeed = clampAutoScrollSpeed(it) },
                autoNextEnabled = autoNextEnabled,
                onAutoNextToggle = {
                    if (!seamlessChaptersEnabled) {
                        autoNextEnabled = !autoNextEnabled
                    }
                },
                autoNextDelaySeconds = autoNextDelaySeconds,
                onAutoNextDelayChange = { autoNextDelaySeconds = it },
                immersiveFullscreen = immersiveFullscreen,
                onToggleFullscreen = { enabled ->
                    immersiveFullscreen = enabled
                    prefs.edit { putBoolean("reader_immersive_fullscreen", enabled) }
                },
                keepScreenOnEnabled = keepScreenOnEnabled,
                onKeepScreenOnChange = { keepScreenOnEnabled = it },
                seamlessChaptersEnabled = seamlessChaptersEnabled,
                onSeamlessChange = { seamlessChaptersEnabled = it },
                nightDimmerAlpha = nightDimmerAlpha,
                onNightDimmerChange = { nightDimmerAlpha = it },
                loadedPageSet = loadedPageSet,
                onSettledImageIncrement = { settledImageCount++ },
                onUpdateAutoScrollPausedUntil = { autoScrollPausedUntil = it },
                isBookmarked = isBookmarked,
                onToggleBookmarkState = { isBookmarked = it },
                onBack = onBack,
                onNavigateChapter = onNavigateChapter,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
