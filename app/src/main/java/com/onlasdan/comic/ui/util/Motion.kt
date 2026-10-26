package com.onlasdan.comic.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Token fisika spasial dengan factory generik untuk varian Float, Dp, dan IntOffset.
 * Mencegah copy-paste literal nilai dampingRatio dan stiffness di banyak tempat.
 */
class SpatialToken(val dampingRatio: Float, val stiffness: Float) {
    val asFloat: SpringSpec<Float> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
    val asDp: SpringSpec<androidx.compose.ui.unit.Dp> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
    val asOffset: SpringSpec<IntOffset> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

    fun <T> spec(visibilityThreshold: T? = null): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
}

/**
 * Noir Physics — sistem token gerak 100% fisika pegas (lihat MOTION.md).
 * Zero tween, zero milidetik di dalam token.
 */
object NoirMotion {
    // ── 1. Spatial Tokens ──────────────────────────────────────────────
    val Spatial = SpatialToken(dampingRatio = 0.8f, stiffness = 800f)
    val SpatialFast = SpatialToken(dampingRatio = 0.72f, stiffness = 900f)
    val SpatialMedium = SpatialToken(dampingRatio = 0.75f, stiffness = 550f)
    val SpatialHeavy = SpatialToken(dampingRatio = 0.82f, stiffness = 380f)

    // ── 2. Effects Tokens (NoBouncy) ───────────────────────────────────
    object Effects {
        val Float: SpringSpec<Float> = spring(Spring.DampingRatioNoBouncy, 1000f)
        val Color: SpringSpec<androidx.compose.ui.graphics.Color> = spring(Spring.DampingRatioNoBouncy, 1000f)
        val Fast: SpringSpec<Float> = spring(Spring.DampingRatioNoBouncy, 1200f)
        val Dismiss: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 1500f,
            visibilityThreshold = 4f,
        )

        fun <T> spec(
            stiffness: Float = 1000f,
            visibilityThreshold: T? = null,
        ): SpringSpec<T> = spring(Spring.DampingRatioNoBouncy, stiffness, visibilityThreshold)
    }

    // ── 3. Press Tokens ───────────────────────────────────────────────
    object Press {
        val Float: SpringSpec<Float> = spring(0.8f, 1200f)
        fun <T> spec(visibilityThreshold: T? = null): SpringSpec<T> = spring(0.8f, 1200f, visibilityThreshold)
    }

    // ── 4. Ambient Tokens ─────────────────────────────────────────────
    object Ambient {
        val Shimmer: SpringSpec<Float> = spring(0.8f, 60f)
        val Breathe: SpringSpec<Float> = spring(0.8f, 50f)
    }

    // ── 5. Khusus: Pill Dock Transitions ──────────────────────────────
    object Pill {
        val Throw: SpringSpec<Float> = spring(0.72f, 500f)
        val Drop: SpringSpec<Float> = spring(1.0f, 700f)
    }

    // ── Reduced Motion Override (bukan token umum) ────────────────────
    private val _stiffFloat: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 4000f)
    private val _stiffColor: SpringSpec<Color> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 4000f)

    fun stiffSpring(): SpringSpec<Float> = _stiffFloat
    fun stiffSpringColor(): SpringSpec<Color> = _stiffColor
}

/**
 * Legacy tokens — dipertahankan untuk konstanta stagger.
 */
object MotionTokens {
    /** Langkah stagger antar item (sequencing, bukan durasi animasi). */
    const val StaggerStepMs = 60L

    /** Stagger hanya untuk beberapa item pertama. */
    const val StaggerCap = 8
}

/**
 * Bypass paksa system animator duration scale (Developer Options 0x).
 * Disediakan di coroutine context tempat animasi kita berjalan — elemen kanan
 * menang atas scale sistem. Terbukti: shimmer tetap hidup saat sistem 0x.
 *
 * Semua animasi aplikasi WAJIB berjalan di dalam withContext(NoirForceMotionScale)
 * (via helper animateNoir*AsState) agar tetap 100% hidup meski HP di-set 0x.
 *
 * Keputusan arsitektur 2026-10-19: custom Recomposer TIDAK dipakai — lifecycle
 * hack-nya rapuh; pola withContext ini terbukti bekerja di aplikasi ini.
 */
object NoirForceMotionScale : MotionDurationScale {
    override val scaleFactor: Float = 1f
    override val key: CoroutineContext.Key<*> = MotionDurationScale.Key
}

/**
 * True bila pengguna memilih mode "Minim" di Settings > Animasi UI.
 * TIDAK membaca system animator scale — animasi kita tetap hidup meski
 * sistem di-set 0x (lihat MOTION.md §3).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val mode by UiPreferences.animationMode(context).collectAsState(initial = AnimationMode.FULL)
    return mode == AnimationMode.MINIMAL
}

/**
 * animateFloatAsState yang kebal terhadap system 0x: animasi berjalan di
 * context dengan [NoirForceMotionScale] (scaleFactor = 1f), lifecycle-nya
 * tetap terikat ke LaunchedEffect (batal otomatis saat target berubah).
 */
@Composable
fun animateNoirFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = NoirMotion.Spatial.asFloat,
    label: String = "NoirFloatAnimation",
): State<Float> {
    val animatable = remember { Animatable(targetValue) }
    LaunchedEffect(targetValue, animationSpec) {
        withContext(NoirForceMotionScale) {
            animatable.animateTo(targetValue, animationSpec)
        }
    }
    return animatable.asState()
}

/** Varian Dp dari [animateNoirFloatAsState]: kebal terhadap system 0x. */
@Composable
fun animateNoirDpAsState(
    targetValue: androidx.compose.ui.unit.Dp,
    animationSpec: AnimationSpec<androidx.compose.ui.unit.Dp> = NoirMotion.Spatial.asDp,
    label: String = "NoirDpAnimation",
): State<androidx.compose.ui.unit.Dp> {
    val animatable = remember {
        Animatable(targetValue, androidx.compose.ui.unit.Dp.VectorConverter)
    }
    LaunchedEffect(targetValue, animationSpec) {
        withContext(NoirForceMotionScale) {
            animatable.animateTo(targetValue, animationSpec)
        }
    }
    return animatable.asState()
}

/** Converter Color <-> AnimationVector4D (tidak ada bawaan di animation-core). */
private val NoirColorConverter = TwoWayConverter<Color, AnimationVector4D>(
    convertToVector = { AnimationVector4D(it.red, it.green, it.blue, it.alpha) },
    convertFromVector = { Color(it.v1, it.v2, it.v3, it.v4) },
)

/** Varian warna dari [animateNoirFloatAsState]. */
@Composable
fun animateNoirColorAsState(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = NoirMotion.Effects.Color,
    label: String = "NoirColorAnimation",
): State<Color> {
    val animatable = remember<Animatable<Color, AnimationVector4D>> {
        Animatable(targetValue, NoirColorConverter)
    }
    LaunchedEffect(targetValue, animationSpec) {
        withContext(NoirForceMotionScale) {
            animatable.animateTo(targetValue, animationSpec)
        }
    }
    return animatable.asState()
}

/**
 * Pilih spec animasi sesuai mode: FULL pakai [preferred], MINIMAL dikunci ke
 * pegas super kaku (MOTION.md §3). Satu baris di call site.
 */
@Composable
fun rememberNoirSpec(preferred: AnimationSpec<Float>): AnimationSpec<Float> {
    val reducedMotion = rememberReducedMotion()
    return remember(reducedMotion, preferred) {
        if (reducedMotion) NoirMotion.stiffSpring() else preferred
    }
}

/**
 * Subtle scale-down press feedback. Share the [interactionSource] with the
 * clickable so presses are observed:
 *
 * ```
 * val interactionSource = remember { MutableInteractionSource() }
 * Card(
 *     Modifier
 *         .pressScale(interactionSource)
 *         .clickable(interactionSource = interactionSource, indication = null) { ... }
 * )
 * ```
 *
 * Uses the lambda form of graphicsLayer so animation ticks don't recompose.
 * Reduced motion = pegas super kaku, bukan snap instan.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: MutableInteractionSource,
    val pressedScale: Float,
) : ModifierNodeElement<PressScaleNode>() {
    override fun create(): PressScaleNode = PressScaleNode(interactionSource, pressedScale)

    override fun update(node: PressScaleNode) {
        node.update(interactionSource, pressedScale)
    }
}

private class PressScaleNode(
    private var interactionSource: MutableInteractionSource,
    private var pressedScale: Float,
) : Modifier.Node(),
    DrawModifierNode {
    private val scaleAnim = Animatable(1f)
    private var collectJob: Job? = null

    override fun onAttach() {
        startCollecting()
    }

    override fun onDetach() {
        collectJob?.cancel()
        collectJob = null
    }

    fun update(newInteractionSource: MutableInteractionSource, newPressedScale: Float) {
        val sourceChanged = interactionSource != newInteractionSource
        interactionSource = newInteractionSource
        pressedScale = newPressedScale
        if (sourceChanged && isAttached) {
            collectJob?.cancel()
            startCollecting()
        }
    }

    private fun startCollecting() {
        collectJob = coroutineScope.launch {
            withContext(NoirForceMotionScale) {
                var isPressed = false
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                    }
                    val target = if (isPressed) pressedScale else 1f
                    scaleAnim.animateTo(target, NoirMotion.Press.Float)
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scaleAnim.value
        if (s != 1f) {
            scale(s, s) {
                this@draw.drawContent()
            }
        } else {
            drawContent()
        }
    }
}

/**
 * One-time staggered entrance untuk item grid/list (spec 2026-10-19 §5):
 * alpha 0f->1f (NoirEffects), translationY 60f->0f + scale 0.92f->1f
 * (NoirSpatialMedium) — kartu "mendarat" dengan bobot, bukan muncul instan.
 * Delay stagger adalah sequencing (pengecualian ms yang disengaja, MOTION.md).
 */
fun Modifier.staggeredEntrance(
    index: Int,
    entranceDone: Boolean,
    triggerKey: Any? = Unit,
): Modifier = comicCardHalftoneEntrance(index = index, entranceDone = entranceDone, triggerKey = triggerKey)

/**
 * Pengganti AnimatedVisibility bawaan yang mati saat system 0x.
 * Alpha + slide dikendalikan Animatable di dalam withContext(NoirForceMotionScale),
 * keluar dari composition hanya setelah animasi exit selesai.
 */
@Composable
fun NoirAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    slideDistance: androidx.compose.ui.unit.Dp = 28.dp,
    enterSpec: AnimationSpec<Float> = NoirMotion.Pill.Throw,
    exitSpec: AnimationSpec<Float> = NoirMotion.Pill.Drop,
    content: @Composable () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slidePx = with(density) { slideDistance.toPx() }
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(slidePx) }
    var render by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        withContext(NoirForceMotionScale) {
            if (visible) {
                render = true
                launch { alpha.animateTo(1f, NoirMotion.Effects.Float) }
                offset.animateTo(0f, enterSpec)
            } else {
                launch { alpha.animateTo(0f, NoirMotion.Effects.Float) }
                offset.animateTo(slidePx, exitSpec)
                render = false
            }
        }
    }
    if (render) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.graphicsLayer {
                this.alpha = alpha.value
                translationY = offset.value
            },
        ) { content() }
    }
}

/**
 * Tactile press standar Noir (spec 2026-10-19 §4): tekan -> scale mengecil
 * (0.93f card/tombol, 0.88f icon/tab), lepas -> membal kembali via
 * NoirSpatialFast. Kebal terhadap system 0x. Tanpa ripple (indication null).
 * Menggunakan ModifierNodeElement (PressScaleNode) di draw phase untuk nol
 * recomposition overhead per frame.
 */
fun Modifier.noirClickable(
    pressedScale: Float = 0.93f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .pressScale(
            interactionSource = interactionSource,
            pressedScale = if (enabled) pressedScale else 1f,
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.noirCombinedClickable(
    pressedScale: Float = 0.93f,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .pressScale(
            interactionSource = interactionSource,
            pressedScale = if (enabled) pressedScale else 1f,
        )
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/**
 * The last tapped cover key. The shared-element transition only runs for the
 * card the user actually tapped — this avoids duplicate-key matches when the
 * same comic appears in two lists at once.
 */
object CoverTransitionState {
    var activeKey by mutableStateOf<String?>(null)
}

/** Provided once around the NavHost, inside SharedTransitionLayout. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provided per navigation destination (via ScreenTransition). */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Shared-element transition for a comic cover between a card and the detail
 * screen. Both sides call this with the same key ("cover-$slug"). The
 * transition only applies when the key matches the last tapped card.
 */
@Composable
fun Modifier.coverSharedElement(key: String?): Modifier {
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    if (key == null || transitionScope == null || visibilityScope == null) return this
    if (CoverTransitionState.activeKey != key) return this
    val state = with(transitionScope) { rememberSharedContentState(key) }
    return with(transitionScope) {
        this@coverSharedElement.sharedElement(state, visibilityScope)
    }
}
