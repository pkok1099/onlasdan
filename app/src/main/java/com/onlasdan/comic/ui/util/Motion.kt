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
 * Noir Physics — bahasa gerak 100% fisika pegas (lihat MOTION.md).
 * Zero tween, zero milidetik di dalam token.
 */
object NoirMotion {
    /**
     * PENTING: semua spec adalah singleton yang stabil. Jangan membuat instance
     * baru per recomposition — spec dipakai sebagai key LaunchedEffect, instance
     * baru tiap frame me-restart animasi terus-menerus (bug 2026-10-19).
     */
    private val _spatialFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 800f)
    private val _spatialDp: SpringSpec<androidx.compose.ui.unit.Dp> =
        spring(dampingRatio = 0.8f, stiffness = 800f)
    private val _spatialOffset: SpringSpec<IntOffset> =
        spring(dampingRatio = 0.8f, stiffness = 800f)
    private val _effectsFloat: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1000f)
    private val _effectsColor: SpringSpec<Color> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1000f)
    private val _stiffFloat: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 4000f)
    private val _stiffColor: SpringSpec<Color> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 4000f)
    private val _pressFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 1200f)

    /**
     * Pill show: "dilempar dari bawah" — momentum lemparan (spec user 2026-10-19).
     * Pill hide: "jatuh" — drop tanpa pantulan.
     */
    private val _pillShowFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.78f, stiffness = 600f)
    private val _pillHideFloat: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f)

    /**
     * Effects cepat untuk tint ikon (spec user 2026-10-19).
     */
    private val _effectsFastFloat: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1200f)
    private val _effectsFastColor: SpringSpec<Color> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1200f)
    private val _shimmerFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 60f)

    /**
     * Breathing/pulse untuk skeleton slot halaman reader: sangat lambat,
     * nyaris seperti napas. Singleton stabil (lihat peringatan di atas).
     */
    private val _breatheFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 50f)
    // ------------------------------------------------------------------
    // Token generasi 2026-10-19 (spec "rasakan animasinya"): underdamped agar
    // ayunan pegas KASAT MATA, stiffness lebih rendah agar terbaca berbobot.
    // ------------------------------------------------------------------

    /** Tombol, chip, tactile press, icon scale: membal, hidup, responsif. */
    private val _noirSpatialFast: SpringSpec<Float> =
        spring(dampingRatio = 0.72f, stiffness = 900f)

    /** Indikator pill, lemparan dock, card entrance: berbobot + overshoot jelas. */
    private val _noirSpatialMedium: SpringSpec<Float> =
        spring(dampingRatio = 0.75f, stiffness = 550f)

    /** Transisi antar layar & ekspansi besar: berat, mewah, punya massa. */
    private val _noirSpatialHeavy: SpringSpec<Float> =
        spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Semua perubahan warna & alpha: mulus tanpa kedip. */
    private val _noirEffects: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f)
    private val _noirEffectsColor: SpringSpec<Color> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f)

    /** Pill muncul: dilempar dari bawah dengan momentum. */
    private val _pillThrowFloat: SpringSpec<Float> =
        spring(dampingRatio = 0.72f, stiffness = 500f)

    /** Pill sembunyi: jatuh ditarik gravitasi. */
    private val _pillDropFloat: SpringSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 700f)

    /** Varian IntOffset dari noirSpatialHeavy untuk slide transisi layar. */
    private val _noirSpatialHeavyOffset: SpringSpec<IntOffset> =
        spring(dampingRatio = 0.82f, stiffness = 380f)

    /**
     * Spatial spring (posisi, ukuran, scale, bentuk): slightly underdamped —
     * satu ayunan rem magnetik yang sangat halus di akhir gerakan.
     */
    fun spatialSpring(): SpringSpec<Float> = _spatialFloat

    fun spatialSpringDp(): SpringSpec<androidx.compose.ui.unit.Dp> = _spatialDp

    fun spatialSpringOffset(): SpringSpec<IntOffset> = _spatialOffset

    /**
     * Effects spring (alpha/opacity & warna): wajib NoBouncy agar mulus
     * tanpa kedip.
     */
    fun effectsSpring(): SpringSpec<Float> = _effectsFloat

    fun effectsSpringColor(): SpringSpec<Color> = _effectsColor

    /**
     * Reduced motion: tetap 100% spring, dikunci ke pegas super kaku tanpa
     * pantulan. Tanpa snap instan, tanpa crossfade berdurasi ms.
     */
    fun stiffSpring(): SpringSpec<Float> = _stiffFloat

    fun stiffSpringColor(): SpringSpec<Color> = _stiffColor

    /**
     * Press feedback: elemen kecil bergerak dengan stiffness tinggi
     * (hukum massa).
     */
    fun pressSpring(): SpringSpec<Float> = _pressFloat

    fun pillShowSpring(): SpringSpec<Float> = _pillShowFloat

    fun pillHideSpring(): SpringSpec<Float> = _pillHideFloat

    fun effectsFastSpring(): SpringSpec<Float> = _effectsFastFloat

    fun effectsFastSpringColor(): SpringSpec<Color> = _effectsFastColor

    /**
     * Shimmer spec singleton stabil agar LaunchedEffect tidak me-restart
     * animasi per recomposition frame.
     */
    fun shimmerSpring(): SpringSpec<Float> = _shimmerFloat

    /**
     * Breathing spec singleton stabil agar LaunchedEffect tidak me-restart
     * animasi per recomposition frame.
     */
    fun breatheSpring(): SpringSpec<Float> = _breatheFloat

    /** Token 2026-10-19: tactile & elemen kecil (0.72f, 900f). */
    fun noirSpatialFast(): SpringSpec<Float> = _noirSpatialFast

    /** Token 2026-10-19: bobot menengah + overshoot kasat mata (0.75f, 550f). */
    fun noirSpatialMedium(): SpringSpec<Float> = _noirSpatialMedium

    /** Token 2026-10-19: transisi berat & mewah (0.82f, 380f). */
    fun noirSpatialHeavy(): SpringSpec<Float> = _noirSpatialHeavy

    /** Token 2026-10-19: warna & alpha (NoBouncy, 800f). */
    fun noirEffects(): SpringSpec<Float> = _noirEffects

    /** Varian Color dari noirEffects untuk animateNoirColorAsState. */
    fun noirEffectsColor(): SpringSpec<Color> = _noirEffectsColor

    /** Pill dock muncul: dilempar dari bawah (0.72f, 500f). */
    fun pillThrowSpring(): SpringSpec<Float> = _pillThrowFloat

    /** Pill dock sembunyi: jatuh gravitasi (1.0f, 700f). */
    fun pillDropSpring(): SpringSpec<Float> = _pillDropFloat

    /** Varian IntOffset noirSpatialHeavy untuk slideIn/Out transisi layar. */
    fun noirSpatialHeavyOffset(): SpringSpec<IntOffset> = _noirSpatialHeavyOffset
}

/**
 * Legacy alias — dipertahankan agar file lama tetap kompilasi selama migrasi.
 * JANGAN dipakai di kode baru; pakai [NoirMotion].
 */
object MotionTokens {
    /** Langkah stagger antar item (sequencing, bukan durasi animasi). */
    const val StaggerStepMs = 40L

    /** Stagger hanya untuk beberapa item pertama. */
    const val StaggerCap = 8

    fun pressSpring() = NoirMotion.pressSpring()
    fun gentleSpring() = NoirMotion.spatialSpring()
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
    animationSpec: AnimationSpec<Float> = NoirMotion.spatialSpring(),
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
    animationSpec: AnimationSpec<androidx.compose.ui.unit.Dp> = NoirMotion.spatialSpringDp(),
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
    animationSpec: AnimationSpec<Color> = NoirMotion.effectsSpringColor(),
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
                    scaleAnim.animateTo(target, NoirMotion.noirSpatialFast())
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
): Modifier = composed {
    if (entranceDone) return@composed this
    val reducedMotion = rememberReducedMotion()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(MotionTokens.StaggerCap) * MotionTokens.StaggerStepMs)
        visible = true
    }
    val alphaSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.noirEffects()
    val moveSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.noirSpatialMedium()
    val alpha by animateNoirFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = alphaSpec,
        label = "staggerAlpha",
    )
    val rise by animateNoirFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = moveSpec,
        label = "staggerRise",
    )
    val scale by animateNoirFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = moveSpec,
        label = "staggerScale",
    )
    graphicsLayer {
        this.alpha = alpha
        translationY = rise
        scaleX = scale
        scaleY = scale
    }
}

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
    enterSpec: AnimationSpec<Float> = NoirMotion.pillThrowSpring(),
    exitSpec: AnimationSpec<Float> = NoirMotion.pillDropSpring(),
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
                launch { alpha.animateTo(1f, NoirMotion.noirEffects()) }
                offset.animateTo(0f, enterSpec)
            } else {
                launch { alpha.animateTo(0f, NoirMotion.noirEffects()) }
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
        ) {
            content()
        }
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
