package comic.chrome

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import comic.R
import comic.theme.ComicVibrantColors
import comic.theme.NoirSurface
import comic.theme.PrimaryPink
import comic.util.NoirForceMotionScale
import comic.util.NoirMotion
import comic.util.animateNoirColorAsState
import comic.util.animateNoirFloatAsState
import comic.util.noirClickable
import comic.util.rememberReducedMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Bottom padding standar untuk layar agar konten tidak tertutup UnifiedMorphingDock. */
val UnifiedDockContentPadding = 100.dp

/**
 * Menghitung offset kenaikan kontainer saat keyboard (IME) muncul,
 * dikurangi area navigation bar yang sudah diperhitungkan oleh navigationBarsPadding().
 */
internal fun calculateImePushUpOffset(
    imeBottom: Int,
    navBarsBottom: Int,
    isSheetExpanded: Boolean = true,
    isModalOverlayActive: Boolean = false,
): Float {
    if (!isSheetExpanded || isModalOverlayActive) return 0f
    return (imeBottom - navBarsBottom).coerceAtLeast(0).toFloat()
}

/**
 * Menghitung batas tinggi sheet secara dinamis agar atap sheet (Search Bar)
 * tidak terdorong keluar layar di atas status bar saat kontainer naik di atas keyboard.
 */
internal fun calculateDynamicMaxSheetHeight(
    screenHeightPx: Float,
    statusBarsTop: Float,
    navBarsBottom: Float,
    imeOffsetPx: Float,
    dockHeightPx: Float,
    bottomMarginPx: Float,
    topGapPx: Float,
    maxSheetHeightPx: Float,
): Float {
    val available = screenHeightPx - statusBarsTop - navBarsBottom - imeOffsetPx - dockHeightPx - bottomMarginPx - topGapPx
    return available.coerceAtLeast(200f).coerceAtMost(maxSheetHeightPx)
}

/**
 * Unified Morphing Dock ("Noir & Blush" — True Unified Pill-to-Sheet).
 *
 * Single Surface di [Alignment.BottomCenter]:
 * - Single source of truth: [sheetProgress] (0f = tertutup, 1f = terbuka penuh).
 * - Saat membuka/menutup, atap sheet naik/turun secara presisi, sementara baris ikon
 *   Pill Dock di dasar TERKUNCI MATI di posisinya (zero teleportation/jump).
 * - Real-time interactive drag 1:1 mengikuti jari via [detectVerticalDragGestures] + [VelocityTracker].
 * - Semua properti visual (width, cornerRadius, alpha, icon dimming) diinterpolasi dari [sheetProgress].
 */
@Composable
fun UnifiedMorphingDock(
    visible: Boolean,
    selectedIndex: Int,
    expandedTabIndex: Int?,
    onExpandTab: (index: Int) -> Unit,
    onCollapseSheet: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateBrowse: () -> Unit,
    sheetProgress: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    maxSheetHeight: Dp = 480.dp,
    isModalOverlayActive: Boolean = false,
    sheetContent: @Composable (tabIndex: Int) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val frostedGlass = LocalFrostedGlass.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val windowInfo = LocalWindowInfo.current

    // ── Real-time IME / Keyboard push-up calculation ────────────────────
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val rawImeOffset = calculateImePushUpOffset(
        imeBottom = imeBottom,
        navBarsBottom = navBarsBottom,
        isSheetExpanded = expandedTabIndex != null,
        isModalOverlayActive = isModalOverlayActive,
    )

    val imeOffsetAnim = remember { Animatable(0f) }
    val imeSpringSpec = NoirMotion.SpatialHeavy.asFloat

    LaunchedEffect(rawImeOffset) {
        withContext(NoirForceMotionScale) {
            imeOffsetAnim.animateTo(
                targetValue = rawImeOffset,
                animationSpec = imeSpringSpec,
            )
        }
    }

    val screenHeightPx = windowInfo.containerSize.height.toFloat()
    val statusBarsTop = WindowInsets.statusBars.getTop(density).toFloat()
    val dockHeightPx = with(density) { 56.dp.toPx() }
    val bottomMarginPx = with(density) { 20.dp.toPx() }
    val topGapPx = with(density) { 16.dp.toPx() }
    val maxSheetHeightPx = with(density) { maxSheetHeight.toPx() }

    val dynamicMaxSheetHeightDp = with(density) {
        calculateDynamicMaxSheetHeight(
            screenHeightPx = screenHeightPx,
            statusBarsTop = statusBarsTop,
            navBarsBottom = navBarsBottom.toFloat(),
            imeOffsetPx = rawImeOffset,
            dockHeightPx = dockHeightPx,
            bottomMarginPx = bottomMarginPx,
            topGapPx = topGapPx,
            maxSheetHeightPx = maxSheetHeightPx,
        ).toDp()
    }

    val isExpanded = expandedTabIndex != null

    // ── Simpan tab terakhir agar konten tidak hilang mendadak saat collapse ──
    // Penulisan state hanya di LaunchedEffect (bug BUG-8): menulis saat
    // komposisi memicu recomposition ekstra di pass yang sama.
    var lastActiveTabIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(expandedTabIndex) {
        if (expandedTabIndex != null) {
            lastActiveTabIndex = expandedTabIndex
        }
    }
    val activeTabToRender = expandedTabIndex ?: lastActiveTabIndex

    // ── Pill throw/drop animation (show/hide the whole dock) ────────────
    val throwSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.Pill.Throw
    val dropSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.Pill.Drop
    val pillY = remember { Animatable(160f) }
    val pillScale = remember { Animatable(0.9f) }
    val pillAlpha = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        withContext(NoirForceMotionScale) {
            if (visible) {
                launch { pillAlpha.animateTo(1f, NoirMotion.Effects.Float) }
                launch { pillScale.animateTo(1f, NoirMotion.SpatialMedium.asFloat) }
                pillY.animateTo(0f, throwSpec, initialVelocity = -2200f)
            } else {
                launch { pillAlpha.animateTo(0f, NoirMotion.Effects.Float) }
                launch { pillScale.animateTo(0.88f, NoirMotion.SpatialFast.asFloat) }
                pillY.animateTo(320f, dropSpec, initialVelocity = 1800f)
            }
        }
    }

    // ── Single Source of Truth: sheetProgress (0f..1f) ──────────────────
    var isDragging by remember { mutableStateOf(false) }
    val heavySpringSpec = NoirMotion.SpatialHeavy.asFloat

    LaunchedEffect(expandedTabIndex) {
        if (expandedTabIndex == null) {
            focusManager.clearFocus()
        }
        if (isDragging) return@LaunchedEffect
        withContext(NoirForceMotionScale) {
            if (expandedTabIndex != null) {
                sheetProgress.animateTo(1f, animationSpec = heavySpringSpec)
            } else {
                sheetProgress.animateTo(0f, animationSpec = heavySpringSpec)
            }
        }
    }

    var sheetContentHeightPx by remember { mutableFloatStateOf(1f) }
    val velocityTracker = remember { VelocityTracker() }

    // ── Sliding / Anchor Indicator ──────────────────────────────────────
    val isSheetActive by remember(expandedTabIndex) {
        derivedStateOf { sheetProgress.value > 0.005f || expandedTabIndex != null }
    }
    val effectiveTargetIndex = if (isSheetActive) {
        activeTabToRender ?: selectedIndex
    } else {
        selectedIndex
    }

    val indicatorSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.SpatialMedium.asFloat
    val indicatorTabFraction = remember { Animatable(selectedIndex.toFloat()) }

    LaunchedEffect(effectiveTargetIndex, isDragging) {
        if (isDragging) {
            // Selama gestur drag: kunci mati seketika tanpa spring yang bisa tertinggal/mental
            indicatorTabFraction.snapTo(effectiveTargetIndex.toFloat())
        } else {
            withContext(NoirForceMotionScale) {
                indicatorTabFraction.animateTo(
                    targetValue = effectiveTargetIndex.toFloat(),
                    animationSpec = indicatorSpec,
                )
            }
        }
    }

    var dockRowWidthPx by remember { mutableFloatStateOf(0f) }
    val indicatorSize = 40.dp
    val indicatorHalfPx = with(density) { (indicatorSize / 2).toPx() }

    // ── Dock items ──────────────────────────────────────────────────────
    val dockItems = remember {
        listOf(
            UnifiedDockItem(Icons.Default.Search, R.string.dock_search), // 0
            UnifiedDockItem(Icons.Outlined.Home, R.string.dock_home), // 1
            UnifiedDockItem(Icons.AutoMirrored.Outlined.MenuBook, R.string.dock_browse), // 2
            UnifiedDockItem(Icons.Outlined.Sell, R.string.dock_genres), // 3
            UnifiedDockItem(Icons.Outlined.Person, R.string.dock_profile), // 4
        )
    }
    val sheetTabIndices = setOf(0, 3, 4) // Search, Genre, Profile

    // ── Back handler ────────────────────────────────────────────────────
    val canHandleBack by remember(isExpanded, isModalOverlayActive) {
        derivedStateOf { !isModalOverlayActive && (isExpanded || sheetProgress.value > 0.01f) }
    }
    BackHandler(enabled = canHandleBack) {
        onCollapseSheet()
    }

    val isContentVisible by remember(activeTabToRender, expandedTabIndex) {
        derivedStateOf {
            val currentTab = activeTabToRender ?: expandedTabIndex ?: lastActiveTabIndex
            sheetProgress.value > 0.005f && currentTab != null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .offset { IntOffset(0, -imeOffsetAnim.value.roundToInt()) }
            .layout { measurable, constraints ->
                val pVal = sheetProgress.value.coerceIn(0f, 1f)
                val padPx = lerp(44.dp.toPx(), 18.dp.toPx(), pVal).roundToInt()
                val innerConstraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = (constraints.maxWidth - padPx * 2).coerceAtLeast(0),
                )
                val placeable = measurable.measure(innerConstraints)
                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(padPx, 0)
                }
            }
            .padding(bottom = 20.dp)
            .graphicsLayer {
                alpha = if (isModalOverlayActive) 0f else pillAlpha.value
                translationY = pillY.value
                scaleX = pillScale.value
                scaleY = pillScale.value
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shadowElevation = 12.dp,
            color = if (frostedGlass != null) Color.Transparent else NoirSurface,
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        PrimaryPink.copy(alpha = 0.55f),
                        ComicVibrantColors.ManhwaCyan.copy(alpha = 0.45f),
                        PrimaryPink.copy(alpha = 0.35f),
                    ),
                ),
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .wrapContentHeight(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                // Node frost terpisah: Modifier.blur dipasang SEBELUM drawBehind di rantai
                // modifier node ini (resep StackOverflow 78740780) — salinan sumber + scrim
                // digambar DI DALAM node ber-renderEffect; konten dock (node saudara) tetap
                // tajam karena di luar layer ber-efek.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .frostedGlassEffect(frostedGlass),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    // ═══════════════════════════════════════════════════════
                    // BAGIAN ATAS: Sheet Content (Handle + konten Search/Genre/Profile)
                    // Dikendalikan oleh layout modifier berbasis sheetProgress.value
                    // ═══════════════════════════════════════════════════════
                    if (isContentVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = dynamicMaxSheetHeightDp)
                                .clipToBounds()
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    sheetContentHeightPx = placeable.height.toFloat().coerceAtLeast(1f)
                                    val currentHeight = (placeable.height * sheetProgress.value.coerceIn(0f, 1.08f)).roundToInt().coerceAtLeast(0)
                                    layout(placeable.width, currentHeight) {
                                        // Letakkan dari atas: atap container turun ke bawah mendekati Pill Dock
                                        placeable.placeRelative(0, 0)
                                    }
                                }
                                .graphicsLayer {
                                    alpha = (sheetProgress.value * 1.25f).coerceIn(0f, 1f)
                                },
                        ) {
                            // M3 Handle Bar (Real-time interactive drag 1:1)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .pointerInput(isModalOverlayActive) {
                                        if (isModalOverlayActive) return@pointerInput
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                isDragging = true
                                                focusManager.clearFocus()
                                                velocityTracker.resetTracking()
                                                scope.launch {
                                                    indicatorTabFraction.snapTo(effectiveTargetIndex.toFloat())
                                                }
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                val deltaProgress = -dragAmount / sheetContentHeightPx.coerceAtLeast(1f)
                                                scope.launch {
                                                    val newProgress = (sheetProgress.value + deltaProgress).coerceIn(0f, 1.08f)
                                                    sheetProgress.snapTo(newProgress)
                                                    indicatorTabFraction.snapTo(effectiveTargetIndex.toFloat())
                                                }
                                            },
                                            onDragEnd = {
                                                val velocityY = velocityTracker.calculateVelocity().y
                                                val velocityProgress = (-velocityY / sheetContentHeightPx.coerceAtLeast(1f)).coerceIn(-15f, 15f)
                                                isDragging = false
                                                scope.launch {
                                                    withContext(NoirForceMotionScale) {
                                                        if (sheetProgress.value < 0.55f || velocityY > 600f) {
                                                            onCollapseSheet()
                                                            sheetProgress.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = heavySpringSpec,
                                                                initialVelocity = velocityProgress,
                                                            )
                                                        } else {
                                                            sheetProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = heavySpringSpec,
                                                                initialVelocity = velocityProgress,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                scope.launch {
                                                    withContext(NoirForceMotionScale) {
                                                        if (sheetProgress.value < 0.55f) {
                                                            onCollapseSheet()
                                                            sheetProgress.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = heavySpringSpec,
                                                            )
                                                        } else {
                                                            sheetProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = heavySpringSpec,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                )
                            }

                            // Konten sheet (Search / Genre / Profile) dengan animasi transisi antar sheet
                            (activeTabToRender ?: expandedTabIndex ?: lastActiveTabIndex)?.let { tabIndex ->
                                MorphingSheetTabTransition(currentTab = tabIndex) { tab ->
                                    sheetContent(tab)
                                }
                            }
                        }

                        // Garis pemisah tipis antara konten sheet dan baris ikon dock
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.06f),
                        )
                    }

                    // ═══════════════════════════════════════════════════════
                    // BAGIAN DASAR: Baris 5 Ikon Pill Dock (SELALU ADA & TERKUNCI MATI)
                    // ═══════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        // Sliding / Anchor indicator: bulatan latar belakang aktif
                        // Koordinat X, Skala, dan Alpha terisolasi di graphicsLayer (Zero Recomposition pada 120Hz)
                        Box(
                            modifier = Modifier
                                .size(indicatorSize)
                                .align(Alignment.CenterStart)
                                .graphicsLayer {
                                    val pVal = sheetProgress.value.coerceIn(0f, 1f)
                                    val activeScale = lerp(1.0f, 1.16f, pVal)
                                    if (dockRowWidthPx > 0f) {
                                        val itemWidth = dockRowWidthPx / 5f
                                        val targetCenterX = (indicatorTabFraction.value + 0.5f) * itemWidth
                                        translationX = targetCenterX - indicatorHalfPx
                                        scaleX = activeScale
                                        scaleY = activeScale
                                        alpha = lerp(0.18f, 0.35f, pVal) / 0.35f
                                    } else {
                                        alpha = 0f
                                    }
                                }
                                .clip(CircleShape)
                                .background(PrimaryPink.copy(alpha = 0.35f)),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { size ->
                                    dockRowWidthPx = size.width.toFloat()
                                },
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            dockItems.forEachIndexed { index, item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    UnifiedDockIconButton(
                                        item = item,
                                        selected = index == effectiveTargetIndex,
                                        enabled = !isModalOverlayActive,
                                        dimAlpha = {
                                            val pVal = sheetProgress.value.coerceIn(0f, 1f)
                                            val isThisActive = (activeTabToRender == index) && pVal > 0.01f
                                            val isDimmed = pVal > 0.01f && !isThisActive
                                            if (isDimmed) lerp(1.0f, 0.22f, pVal) else 1.0f
                                        },
                                        dimScale = {
                                            val pVal = sheetProgress.value.coerceIn(0f, 1f)
                                            val isThisActive = (activeTabToRender == index) && pVal > 0.01f
                                            val isDimmed = pVal > 0.01f && !isThisActive
                                            if (isDimmed) lerp(1.0f, 0.80f, pVal) else 1.0f
                                        },
                                        onClick = {
                                            focusManager.clearFocus()
                                            if (index in sheetTabIndices) {
                                                if (expandedTabIndex == index) {
                                                    // Tap ikon aktif → tutup sheet
                                                    onCollapseSheet()
                                                } else {
                                                    // Tap ikon sheet lain → cross-tab switch
                                                    onExpandTab(index)
                                                }
                                            } else {
                                                // Home / Browse → tutup sheet, navigate
                                                onCollapseSheet()
                                                when (index) {
                                                    1 -> onNavigateHome()
                                                    2 -> onNavigateBrowse()
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Private helpers
// ═══════════════════════════════════════════════════════════════════════════

private data class UnifiedDockItem(
    val icon: ImageVector,
    @param:StringRes val descriptionRes: Int,
)

@Composable
private fun UnifiedDockIconButton(
    item: UnifiedDockItem,
    selected: Boolean,
    dimAlpha: () -> Float,
    dimScale: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val reducedMotion = rememberReducedMotion()
    val popSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.SpatialFast.asFloat

    val iconScale by animateNoirFloatAsState(
        targetValue = if (selected) 1.12f else 0.92f,
        animationSpec = popSpec,
        label = "uDockIconScale",
    )
    val iconTint by animateNoirColorAsState(
        targetValue = if (selected) PrimaryPink else Color.White.copy(alpha = 0.55f),
        animationSpec = if (reducedMotion) NoirMotion.stiffSpringColor() else NoirMotion.Effects.Color,
        label = "uDockIconTint",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .noirClickable(
                pressedScale = 0.90f,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.descriptionRes),
            tint = iconTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    val dScale = dimScale()
                    scaleX = iconScale * dScale
                    scaleY = iconScale * dScale
                    alpha = dimAlpha()
                },
        )
    }
}
