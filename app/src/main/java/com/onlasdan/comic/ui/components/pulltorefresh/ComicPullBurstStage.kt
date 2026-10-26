package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.ui.theme.BurstBubbleEnd
import com.onlasdan.comic.ui.theme.BurstBubbleStart
import com.onlasdan.comic.ui.theme.ComicBurstBubbleShape
import com.onlasdan.comic.ui.theme.ComicSfxCyan
import com.onlasdan.comic.ui.theme.ComicSfxYellow
import com.onlasdan.comic.ui.theme.InterFontFamily
import com.onlasdan.comic.ui.theme.PureBlack
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private val BurstSurfaceBrush = Brush.linearGradient(
    colors = listOf(BurstBubbleStart, BurstBubbleEnd),
)

@Composable
internal fun BoxScope.ComicPullBurstStage(state: ComicPullState) {
    ShockwaveRingOverlay(state = state)
    SparksParticlesOverlay(state = state)
    ComicBurstBubble(
        state = state,
        modifier = Modifier.align(Alignment.Center),
    )
}

@Composable
private fun BoxScope.ShockwaveRingOverlay(state: ComicPullState) {
    val ringT = (state.impactElapsed / 0.55f).coerceIn(0f, 1f)
    if (ringT >= 1f) return
    val ringEase = 1f - (1f - ringT).pow(3f)
    val accent = state.currentSfx.accent.color

    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .size(90.dp)
            .graphicsLayer {
                val s = 0.2f + ringEase * 2.4f
                scaleX = s
                scaleY = s
                alpha = (1f - ringEase) * 0.95f
            },
    ) {
        val strokeW = (6f - ringEase * 5f).dp.toPx()
        drawCircle(color = ComicSfxCyan.copy(alpha = 0.45f), style = Stroke(width = strokeW * 1.6f))
        drawCircle(color = accent, style = Stroke(width = strokeW))
    }
}

@Composable
private fun BoxScope.SparksParticlesOverlay(state: ComicPullState) {
    val elapsed = state.impactElapsed
    if (elapsed >= 0.85f) return
    state.sparks.forEachIndexed { idx, spark ->
        val duration = 0.55f + (idx % 5) * 0.055f
        val t = (elapsed / duration).coerceIn(0f, 1f)
        if (t < 1f) {
            val ease = 1f - (1f - t).pow(3f)
            SparkParticleView(
                spark = spark,
                ease = ease,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SparkParticleView(
    spark: ComicSparkParticle,
    ease: Float,
    modifier: Modifier = Modifier,
) {
    val scaleVal = 0.2f + (spark.targetScale - 0.2f) * ease
    Text(
        text = spark.symbol,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        color = spark.color,
        style = TextStyle(shadow = Shadow(color = PureBlack, offset = Offset(2f, 2f), blurRadius = 4f)),
        modifier = modifier.graphicsLayer {
            translationX = (spark.targetX * ease).dp.toPx()
            translationY = (spark.targetY * ease).dp.toPx()
            scaleX = scaleVal
            scaleY = scaleVal
            rotationZ = spark.targetRotation * ease
            alpha = (1f - ease).coerceIn(0f, 1f)
        },
    )
}

@Composable
private fun ComicBurstBubble(
    state: ComicPullState,
    modifier: Modifier = Modifier,
) {
    val sfx = state.currentSfx
    Box(
        modifier = modifier.burstWobbleTransform(state.burstPos, state.timeTick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset((-3).dp.roundToPx(), (-3).dp.roundToPx()) }
                .background(ComicSfxCyan, ComicBurstBubbleShape),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(4.dp.roundToPx(), 5.dp.roundToPx()) }
                .background(sfx.accent.color, ComicBurstBubbleShape),
        )
        BurstBubbleBody(sfx = sfx, timeTick = state.timeTick)
    }
}

private fun Modifier.burstWobbleTransform(burstPos: Float, timeTick: Float): Modifier = graphicsLayer {
    val bp = burstPos.coerceAtLeast(0f)
    val breathe = sin(timeTick * 8f) * 0.05f
    val finalScale = (bp + breathe).coerceAtLeast(0.01f)
    scaleX = finalScale
    scaleY = finalScale
    rotationZ = -3f + cos(timeTick * 6f) * 2.5f
}

@Composable
private fun BurstBubbleBody(sfx: ComicSfx, timeTick: Float) {
    Box(
        modifier = Modifier
            .background(BurstSurfaceBrush, ComicBurstBubbleShape)
            .border(2.5.dp, TextPrimary, ComicBurstBubbleShape)
            .padding(horizontal = 22.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        BurstBubbleContent(sfx = sfx)
        BurstCornerStar(
            color = ComicSfxYellow,
            rotation = timeTick * 180f,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-28).dp, y = (-14).dp),
        )
        BurstCornerStar(
            color = ComicSfxCyan,
            rotation = -timeTick * 144f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 26.dp, y = 14.dp),
        )
    }
}

private val BurstTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Black,
    fontStyle = FontStyle.Italic,
    fontSize = 23.sp,
    shadow = Shadow(PureBlack, Offset(4f, 4f), 2f),
)

private val BurstMeaningStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 9.5.sp,
    color = TextSecondary,
    letterSpacing = 1.1.sp,
)

@Composable
private fun BurstBubbleContent(sfx: ComicSfx) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "⚡", fontSize = 18.sp)
            Text(text = sfx.burstTitle, color = sfx.accent.color, style = BurstTitleStyle)
            Text(text = "⚡", fontSize = 18.sp)
        }
        Text(text = "${sfx.origin} • ${sfx.burstMeaning}", style = BurstMeaningStyle)
    }
}

@Composable
private fun BurstCornerStar(
    color: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "✦",
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        color = color,
        style = TextStyle(shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = 12f)),
        modifier = modifier.graphicsLayer { rotationZ = rotation },
    )
}
