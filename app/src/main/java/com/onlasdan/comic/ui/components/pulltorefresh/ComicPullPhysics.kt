package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import com.onlasdan.comic.ui.theme.ComicSfxCyan
import com.onlasdan.comic.ui.theme.ComicSfxRose
import com.onlasdan.comic.ui.theme.ComicSfxYellow
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal const val TriggerThresholdDp = 92f
internal const val RefreshingHeightDp = 86f
private const val MinBurstHoldSec = 0.95f
private const val SparkCount = 18

val LocalComicPullState = compositionLocalOf<ComicPullState?> { null }

internal class CharSpringState(
    val threshold: Float,
    val stiffness: Float,
    val damping: Float,
    val rotAngle: Float,
) {
    var pos: Float = 0f
    var vel: Float = 0f

    fun step(dt: Float, progress: Float, isRefreshing: Boolean) {
        val active = !isRefreshing && progress >= threshold
        val target = if (active) ((progress - threshold) / 0.35f).coerceAtMost(1.12f) else 0f
        val force = -stiffness * (pos - target) - damping * vel
        vel += force * dt
        pos += vel * dt
    }

    fun popImpulse() {
        vel += 3.2f
    }

    fun reset() {
        pos = 0f
        vel = 0f
    }
}

internal data class ComicSparkParticle(
    val symbol: String,
    val color: Color,
    val targetX: Float,
    val targetY: Float,
    val targetScale: Float,
    val targetRotation: Float,
)

@Stable
class ComicPullState internal constructor(private val densityScale: Float) {
    var currentSfx by mutableStateOf(ComicSfxCatalog.MANGA_VARIANTS.first())
        private set
    var isDragging by mutableStateOf(false)
        private set
    var externalRefreshing by mutableStateOf(false)
    var burstHoldTimer by mutableFloatStateOf(0f)
        private set
    var pointerXNorm by mutableFloatStateOf(0.5f)
        private set
    var rawDragDp by mutableFloatStateOf(0f)
        private set
    var currentPullDp by mutableFloatStateOf(0f)
        private set
    var springPosDp by mutableFloatStateOf(0f)
        private set
    private var springVelDp = 0f

    var burstPos by mutableFloatStateOf(0f)
        private set
    private var burstVel = 0f

    var timeTick by mutableFloatStateOf(0f)
        private set
    var impactElapsed by mutableFloatStateOf(10f)
        private set
    var freshWaveElapsed by mutableFloatStateOf(10f)
        private set
    internal var sparks by mutableStateOf<List<ComicSparkParticle>>(emptyList())
        private set

    private var thresholdCrossed = false
    private var wasRefreshingVisual = false

    internal val charSprings = listOf(
        CharSpringState(threshold = 0.10f, stiffness = 260f, damping = 16f, rotAngle = -10f),
        CharSpringState(threshold = 0.34f, stiffness = 210f, damping = 14f, rotAngle = 6f),
        CharSpringState(threshold = 0.58f, stiffness = 175f, damping = 12f, rotAngle = -5f),
    )

    val isRefreshingVisual: Boolean
        get() = externalRefreshing || burstHoldTimer > 0f

    val isSimulationActive: Boolean
        get() = isDragging ||
            isRefreshingVisual ||
            wasRefreshingVisual ||
            springPosDp > 0.15f ||
            abs(burstPos) > 0.01f ||
            impactElapsed < 1.15f ||
            freshWaveElapsed < 1.05f

    val progress: Float
        get() = (springPosDp / TriggerThresholdDp).coerceIn(0f, 1.35f)

    internal fun updatePointerX(xPx: Float, widthPx: Float) {
        if (widthPx > 0f) {
            pointerXNorm = (xPx / widthPx).coerceIn(0.15f, 0.85f)
        }
    }

    internal fun stepPhysics(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.032f)
        timeTick += clampedDt
        if (burstHoldTimer > 0f) burstHoldTimer = (burstHoldTimer - clampedDt).coerceAtLeast(0f)
        if (impactElapsed < 2.5f) impactElapsed += clampedDt
        if (freshWaveElapsed < 2.5f) freshWaveElapsed += clampedDt

        val activeRefresh = isRefreshingVisual
        if (wasRefreshingVisual && !activeRefresh) {
            freshWaveElapsed = 0f
        }
        wasRefreshingVisual = activeRefresh

        stepMainSpring(clampedDt)
        val currentProg = progress
        charSprings.forEach { it.step(clampedDt, currentProg, activeRefresh) }
        stepBurstSpring(clampedDt)
    }

    private fun stepMainSpring(dt: Float) {
        if (isDragging) {
            springPosDp += (currentPullDp - springPosDp) * 0.45f
            springVelDp = 0f
            return
        }
        pointerXNorm += (0.5f - pointerXNorm) * (dt * 10f).coerceAtMost(1f)
        val dest = if (isRefreshingVisual) RefreshingHeightDp else 0f
        val stiffness = if (isRefreshingVisual) 210f else 240f
        val damping = if (isRefreshingVisual) 19f else 22f
        val force = -stiffness * (springPosDp - dest) - damping * springVelDp
        springVelDp += force * dt
        springPosDp += springVelDp * dt
        if (abs(springPosDp - dest) < 0.15f && abs(springVelDp) < 0.15f) {
            springPosDp = dest
            springVelDp = 0f
        }
    }

    private fun stepBurstSpring(dt: Float) {
        val target = if (isRefreshingVisual) 1f else 0f
        val force = -280f * (burstPos - target) - 15f * burstVel
        burstVel += force * dt
        burstPos += burstVel * dt
        if (abs(burstPos - target) < 0.005f && abs(burstVel) < 0.01f) {
            burstPos = target
            burstVel = 0f
        }
    }

    internal fun accumulateDragPx(deltaY: Float): Float {
        if (isRefreshingVisual) return 0f
        val deltaDp = deltaY / densityScale
        val nextRaw = (rawDragDp + deltaDp).coerceAtLeast(0f)
        val consumedDp = nextRaw - rawDragDp
        if (!isDragging && nextRaw > 0.5f) {
            isDragging = true
            thresholdCrossed = false
            currentSfx = ComicSfxCatalog.pickNext(currentSfx.id)
        }
        rawDragDp = nextRaw
        currentPullDp = if (nextRaw > 0f) nextRaw.pow(0.82f) * 1.95f else 0f
        checkThresholdPop()
        return consumedDp * densityScale
    }

    private fun checkThresholdPop() {
        val over = currentPullDp >= TriggerThresholdDp
        if (over && !thresholdCrossed) {
            charSprings.forEach { it.popImpulse() }
        }
        thresholdCrossed = over
    }

    internal fun onRelease(onRefresh: () -> Unit) {
        if (!isDragging && rawDragDp <= 0f) return
        val shouldTrigger = currentPullDp >= TriggerThresholdDp && !isRefreshingVisual
        isDragging = false
        rawDragDp = 0f
        if (shouldTrigger) {
            triggerImpact(onRefresh)
        } else {
            currentPullDp = 0f
        }
    }

    private fun triggerImpact(onRefresh: () -> Unit) {
        burstHoldTimer = MinBurstHoldSec
        currentPullDp = RefreshingHeightDp
        burstPos = 0.2f
        burstVel = 6.5f
        impactElapsed = 0f
        sparks = buildComicSparks(currentSfx)
        onRefresh()
    }
}

private fun buildComicSparks(sfx: ComicSfx): List<ComicSparkParticle> {
    val symbols = listOf("⚡", "✦", "★", "!", "💥", "🔥") + sfx.chars
    val palette = listOf(sfx.accent.color, ComicSfxCyan, ComicSfxYellow, TextPrimary, ComicSfxRose)
    return List(SparkCount) { i ->
        val angle = (2.0 * PI * i / SparkCount) + Random.nextDouble(-0.12, 0.12)
        val distance = 52f + Random.nextFloat() * 82f
        ComicSparkParticle(
            symbol = symbols[i % symbols.size],
            color = palette[i % palette.size],
            targetX = (cos(angle) * distance).toFloat(),
            targetY = (sin(angle) * distance * 0.65).toFloat(),
            targetScale = 1.1f + Random.nextFloat() * 0.45f,
            targetRotation = Random.nextFloat() * 220f - 110f,
        )
    }
}

@Composable
fun rememberComicPullState(isRefreshing: Boolean): ComicPullState {
    val density = LocalDensity.current.density
    val state = remember(density) { ComicPullState(density) }
    state.externalRefreshing = isRefreshing
    val active = state.isSimulationActive

    LaunchedEffect(state, active) {
        if (!active) return@LaunchedEffect
        withContext(NoirForceMotionScale) {
            var lastNanos = 0L
            while (isActive && state.isSimulationActive) {
                withFrameNanos { nanos ->
                    val dt = if (lastNanos == 0L) 0.016f else (nanos - lastNanos) / 1_000_000_000f
                    lastNanos = nanos
                    state.stepPhysics(dt)
                }
            }
        }
    }
    return state
}

internal fun createComicNestedScrollConnection(
    state: ComicPullState,
    onRefresh: () -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y < 0f && state.rawDragDp > 0f) {
            return Offset(0f, state.accumulateDragPx(available.y))
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput && available.y > 0f) {
            return Offset(0f, state.accumulateDragPx(available.y))
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val hadPull = state.rawDragDp > 0f
        state.onRelease(onRefresh)
        return if (hadPull) available else Velocity.Zero
    }
}
