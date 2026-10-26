package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.ui.components.CardBoundsHolder
import com.onlasdan.comic.ui.theme.ComicSfxCyan
import com.onlasdan.comic.ui.theme.ComicSfxPink
import com.onlasdan.comic.ui.theme.InterFontFamily
import com.onlasdan.comic.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val ThumbSfxByOrigin = mapOf(
    "manhwa" to listOf("화아아", "두둥", "번쩍", "콰아아"),
    "manhua" to listOf("轰隆", "破!", "斩!", "爆!"),
    "manga" to listOf("ズバッ", "ゴゴゴ", "ドドド", "ドン!"),
)

private val DefaultThumbSfx = listOf("ズバッ", "화아아", "轰隆", "두둥", "ゴゴゴ", "破!")

private data class CardWaveSample(
    val offsetYDp: Float = 0f,
    val scale: Float = 1f,
    val rotationZ: Float = 0f,
    val glowAlpha: Float = 0f,
    val glowColor: Color = ComicSfxPink,
)

private val IdleWaveSample = CardWaveSample()

fun Modifier.comicShockwaveCard(
    boundsHolder: CardBoundsHolder,
    cornerRadius: Dp = 16.dp,
): Modifier = composed {
    val pullState = LocalComicPullState.current ?: return@composed this
    this
        .graphicsLayer {
            if (!pullState.isSimulationActive) {
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                rotationZ = 0f
                return@graphicsLayer
            }
            val sample = evaluateCardWave(pullState, boundsHolder.bounds.top, boundsHolder.bounds.center.x)
            translationY = sample.offsetYDp.dp.toPx()
            scaleX = sample.scale
            scaleY = sample.scale
            rotationZ = sample.rotationZ
        }
        .drawWithContent {
            drawContent()
            if (pullState.isSimulationActive) {
                drawShockwaveCardGlow(pullState, boundsHolder.bounds.top, boundsHolder.bounds.center.x, cornerRadius)
            }
        }
}

fun Modifier.comicShockwaveHero(cornerRadius: Dp = 16.dp): Modifier = composed {
    val holder = remember { CardBoundsHolder() }
    this
        .onGloballyPositioned { holder.bounds = it.boundsInRoot() }
        .comicShockwaveCard(boundsHolder = holder, cornerRadius = cornerRadius)
}

private fun ContentDrawScope.drawShockwaveCardGlow(
    pullState: ComicPullState,
    topPx: Float,
    centerX: Float,
    cornerRadius: Dp,
) {
    val sample = evaluateCardWave(pullState, topPx, centerX)
    if (sample.glowAlpha <= 0.02f) return
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = sample.glowColor.copy(alpha = (sample.glowAlpha * 0.78f).coerceIn(0f, 0.85f)),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(width = 1.8.dp.toPx()),
    )
}

private fun evaluateCardWave(
    state: ComicPullState,
    topPx: Float,
    centerX: Float,
): CardWaveSample {
    val normY = (topPx / 1800f).coerceIn(0f, 1.2f)
    val normX = ((centerX / 1080f) - 0.5f).coerceIn(-0.5f, 0.5f)
    val impactDelay = normY * 0.36f + abs(normX) * 0.07f
    val localImpact = state.impactElapsed - impactDelay
    val localFresh = state.freshWaveElapsed - (normY * 0.32f + (normX + 0.5f) * 0.08f)

    return when {
        localImpact in 0f..0.64f -> computeImpactWave(state, localImpact / 0.64f, normX)
        localFresh in 0f..0.55f -> computeFreshPulseWave(localFresh / 0.55f)
        state.springPosDp > 1f && !state.isRefreshingVisual -> computePullTensionWave(state, normY)
        else -> IdleWaveSample
    }
}

private fun computeImpactWave(
    state: ComicPullState,
    wt: Float,
    normX: Float,
): CardWaveSample {
    val env = (1f - wt) * (1f - wt)
    val phase = (wt * 2.0 * PI).toFloat()
    val waveSin = sin(phase)
    val tiltDir = if (normX < 0f) -2.6f else 2.6f
    val color = if (wt < 0.45f) state.currentSfx.accent.color else ComicSfxCyan
    return CardWaveSample(
        offsetYDp = waveSin * 15f * env,
        scale = 1f - waveSin * 0.075f * env,
        rotationZ = cos(phase) * tiltDir * env,
        glowAlpha = (1f - wt),
        glowColor = color,
    )
}

private fun computeFreshPulseWave(ft: Float): CardWaveSample {
    val env = 1f - ft
    val phase = (ft * 2.0 * PI).toFloat()
    val scaleDelta = if (ft < 0.4f) -0.06f * (1f - ft / 0.4f) else sin(phase) * 0.035f * env
    val color = if (ft < 0.5f) ComicSfxPink else ComicSfxCyan
    return CardWaveSample(
        offsetYDp = -sin(phase) * 6f * env,
        scale = 1f + scaleDelta,
        rotationZ = 0f,
        glowAlpha = env * 0.9f,
        glowColor = color,
    )
}

private fun computePullTensionWave(
    state: ComicPullState,
    normY: Float,
): CardWaveSample {
    val pullFactor = state.progress.coerceIn(0f, 1.25f) * (1f - (normY * 0.6f).coerceIn(0f, 0.75f))
    val waveY = sin(state.timeTick * 5.5f - normY * 4.2f) * 3.5f * pullFactor
    val tilt = (state.pointerXNorm - 0.5f) * 2.4f * pullFactor
    return CardWaveSample(
        offsetYDp = waveY,
        scale = 1f - pullFactor * 0.02f,
        rotationZ = tilt,
        glowAlpha = (pullFactor * 0.42f).coerceIn(0f, 0.45f),
        glowColor = state.currentSfx.accent.color,
    )
}

@Composable
fun BoxScope.ComicThumbSfxStamp(
    seed: String,
    type: String? = null,
    isHero: Boolean = false,
) {
    if (!isHero) return
    val label = remember(seed, type) { pickThumbSfx(seed, type) }
    val alignment = Alignment.TopEnd
    val pad = 12.dp
    val fSize = 26.sp
    val rot = 8f
    val baseAlpha = 0.12f

    Text(
        text = label,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = fSize,
        color = TextPrimary.copy(alpha = baseAlpha),
        modifier = Modifier
            .align(alignment)
            .padding(pad)
            .graphicsLayer { rotationZ = rot },
    )
}

private fun pickThumbSfx(seed: String, type: String?): String {
    val key = type?.trim()?.lowercase() ?: ""
    val pool = ThumbSfxByOrigin[key] ?: DefaultThumbSfx
    val idx = (seed.hashCode() and Int.MAX_VALUE) % pool.size
    return pool[idx]
}

@Composable
fun ElasticNoirAmbientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ComicSfxPink.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(w * 0.5f, 0f),
                radius = (w * 0.85f).coerceAtLeast(1f),
            ),
            radius = (w * 0.85f).coerceAtLeast(1f),
            center = Offset(w * 0.5f, 0f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ComicSfxCyan.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.88f),
                radius = (w * 0.65f).coerceAtLeast(1f),
            ),
            radius = (w * 0.65f).coerceAtLeast(1f),
            center = Offset(w * 0.85f, h * 0.88f),
        )
    }
}
