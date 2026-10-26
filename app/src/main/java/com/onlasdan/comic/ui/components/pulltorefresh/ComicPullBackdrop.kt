package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.ui.theme.ComicSfxCyan
import com.onlasdan.comic.ui.theme.ComicSfxCyanLight
import com.onlasdan.comic.ui.theme.ComicSfxPink
import com.onlasdan.comic.ui.theme.ComicSfxYellow
import com.onlasdan.comic.ui.theme.ComicSfxYellowLight
import com.onlasdan.comic.ui.theme.ComicStampShape
import com.onlasdan.comic.ui.theme.ComicStampSurface
import com.onlasdan.comic.ui.theme.InterFontFamily
import com.onlasdan.comic.ui.theme.LiquidArchBottom
import com.onlasdan.comic.ui.theme.LiquidArchTop
import com.onlasdan.comic.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class SpeedlineSpec(val endX: Float, val endY: Float, val strokeDp: Float, val color: Color)

private val PrototypeSpeedlines = listOf(
    SpeedlineSpec(10f, 20f, 2.2f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(230f, 20f, 1.8f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(10f, 220f, 2.0f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(230f, 220f, 2.2f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(120f, 0f, 1.8f, ComicSfxPink.copy(alpha = 0.55f)),
    SpeedlineSpec(120f, 240f, 1.8f, ComicSfxPink.copy(alpha = 0.55f)),
    SpeedlineSpec(0f, 120f, 2.2f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(240f, 120f, 2.2f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(35f, 65f, 1.6f, ComicSfxCyan.copy(alpha = 0.50f)),
    SpeedlineSpec(205f, 65f, 1.6f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(45f, 185f, 1.6f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(195f, 185f, 1.8f, ComicSfxCyan.copy(alpha = 0.50f)),
    SpeedlineSpec(75f, 15f, 1.4f, ComicSfxYellow.copy(alpha = 0.45f)),
    SpeedlineSpec(165f, 15f, 1.4f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(75f, 225f, 1.4f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(165f, 225f, 1.4f, ComicSfxYellow.copy(alpha = 0.45f)),
    SpeedlineSpec(15f, 90f, 1.3f, ComicSfxPink.copy(alpha = 0.40f)),
    SpeedlineSpec(225f, 90f, 1.3f, ComicSfxCyan.copy(alpha = 0.40f)),
    SpeedlineSpec(15f, 150f, 1.3f, TextPrimary.copy(alpha = 0.28f)),
    SpeedlineSpec(225f, 150f, 1.3f, ComicSfxYellow.copy(alpha = 0.40f)),
)

private data class StampSlotSpec(
    val alignment: Alignment,
    val padH: Dp,
    val padV: Dp,
    val borderColor: Color,
    val shadowColor: Color,
    val textColor: Color,
    val baseTilt: Float,
)

private val StampSlots = listOf(
    StampSlotSpec(Alignment.TopStart, 22.dp, 12.dp, TextPrimary.copy(alpha = 0.28f), ComicSfxPink, TextPrimary, -12f),
    StampSlotSpec(Alignment.TopEnd, 20.dp, 14.dp, ComicSfxCyan, ComicSfxCyan, ComicSfxCyanLight, 12f),
    StampSlotSpec(Alignment.BottomStart, 32.dp, 14.dp, ComicSfxYellow, ComicSfxYellow, ComicSfxYellowLight, -12f),
    StampSlotSpec(Alignment.BottomEnd, 28.dp, 12.dp, TextPrimary.copy(alpha = 0.28f), ComicSfxPink, TextPrimary, 12f),
)

private val LiquidArchBrush = Brush.verticalGradient(
    colors = listOf(LiquidArchTop.copy(alpha = 0.95f), LiquidArchBottom.copy(alpha = 0.98f)),
)

@Composable
internal fun BoxScope.ComicPullBackdrop(state: ComicPullState) {
    LiquidElasticArchCanvas(
        offsetDp = state.springPosDp,
        pointerXNorm = state.pointerXNorm,
        accentColor = state.currentSfx.accent.color,
    )
    HalftoneAuraCanvas(
        progress = state.progress,
        timeTick = state.timeTick,
        accentColor = state.currentSfx.accent.color,
        modifier = Modifier.align(Alignment.Center),
    )
    MangaSpeedlinesCanvas(state = state, modifier = Modifier.align(Alignment.Center))
    AmbientFloatingStampsLayer(state = state)
}

@Composable
private fun LiquidElasticArchCanvas(offsetDp: Float, pointerXNorm: Float, accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val offsetPx = offsetDp.dp.toPx()
        val archEdge = (offsetPx * 0.72f).coerceAtLeast(0f)
        val archControlY = (offsetPx * 1.18f).coerceAtMost(size.height)
        val controlX = (w * pointerXNorm).coerceIn(w * 0.15f, w * 0.85f)

        val fillPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, archEdge)
            quadraticTo(controlX, archControlY, 0f, archEdge)
            close()
        }
        drawPath(path = fillPath, brush = LiquidArchBrush)

        val glowPath = Path().apply {
            moveTo(0f, archEdge)
            quadraticTo(controlX, archControlY, w, archEdge)
        }
        drawPath(
            path = glowPath,
            color = accentColor.copy(alpha = 0.65f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun HalftoneAuraCanvas(
    progress: Float,
    timeTick: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val scaleVal = 0.4f + progress * 0.85f
    Canvas(
        modifier = modifier
            .size(130.dp)
            .graphicsLayer {
                alpha = (progress * 0.9f).coerceIn(0f, 0.9f)
                scaleX = scaleVal
                scaleY = scaleVal
                rotationZ = timeTick * 24f
            },
    ) {
        drawHalftoneDots(accentColor)
    }
}

private fun DrawScope.drawHalftoneDots(accentColor: Color) {
    val step = 7.dp.toPx()
    val count = (size.width / step).toInt()
    for (row in 0..count) {
        for (col in 0..count) {
            drawHalftonePoint(Offset(col * step, row * step), accentColor)
        }
    }
}

private fun DrawScope.drawHalftonePoint(point: Offset, accentColor: Color) {
    val maxR = size.width * 0.36f
    val innerR = size.width * 0.175f
    val dist = hypot(point.x - size.width / 2f, point.y - size.height / 2f)
    if (dist > maxR) return
    val mask = if (dist <= innerR) 1f else (1f - (dist - innerR) / (maxR - innerR))
    drawCircle(accentColor.copy(alpha = 0.42f * mask), 1.5.dp.toPx(), point)
}

@Composable
private fun MangaSpeedlinesCanvas(state: ComicPullState, modifier: Modifier = Modifier) {
    val isRefreshing = state.isRefreshingVisual
    val timeTick = state.timeTick
    val progress = state.progress
    val speedRot = if (isRefreshing) timeTick * 55f else state.springPosDp * 0.42f
    val speedPulse = if (isRefreshing) 0.96f + sin(timeTick * 14f) * 0.1f else 0.3f + progress.coerceAtMost(1f) * 0.8f
    val alphaVal = if (isRefreshing) 0.78f else (progress * 0.98f).coerceIn(0f, 0.98f)

    Canvas(
        modifier = modifier
            .size(260.dp)
            .graphicsLayer {
                alpha = alphaVal
                scaleX = speedPulse
                scaleY = speedPulse
                rotationZ = speedRot
            },
    ) {
        val sx = size.width / 240f
        val sy = size.height / 240f
        val cx = 120f * sx
        val cy = 120f * sy
        PrototypeSpeedlines.forEach { spec ->
            val ex = spec.endX * sx
            val ey = spec.endY * sy
            val start = Offset(cx + (ex - cx) * 0.38f, cy + (ey - cy) * 0.38f)
            drawLine(spec.color, start, Offset(ex, ey), spec.strokeDp.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun AmbientFloatingStampsLayer(state: ComicPullState) {
    val stamps = state.currentSfx.stamps
    Box(modifier = Modifier.fillMaxSize()) {
        StampSlots.forEachIndexed { i, slot ->
            val label = stamps.getOrNull(i) ?: return@forEachIndexed
            val thresh = 0.2f + i * 0.14f
            val fp = if (state.isRefreshingVisual) 1f else ((state.progress - thresh) / 0.4f).coerceIn(0f, 1f)
            if (fp > 0.01f) {
                AmbientStampBadge(
                    label = label,
                    slot = slot,
                    fp = fp,
                    timeTick = state.timeTick,
                    index = i,
                    modifier = Modifier
                        .align(slot.alignment)
                        .padding(horizontal = slot.padH, vertical = slot.padV),
                )
            }
        }
    }
}

@Composable
private fun AmbientStampBadge(
    label: String,
    slot: StampSlotSpec,
    fp: Float,
    timeTick: Float,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.stampFloatingTransform(slot.baseTilt, fp, timeTick, index)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(2.dp.roundToPx(), 2.dp.roundToPx()) }
                .background(slot.shadowColor, ComicStampShape),
        )
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = slot.textColor,
            modifier = Modifier
                .background(ComicStampSurface.copy(alpha = 0.88f), ComicStampShape)
                .border(1.5.dp, slot.borderColor, ComicStampShape)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

private fun Modifier.stampFloatingTransform(
    baseTilt: Float,
    fp: Float,
    timeTick: Float,
    index: Int,
): Modifier = graphicsLayer {
    val bobY = sin(timeTick * 6f + index * 1.7f) * 5f
    val bobX = cos(timeTick * 4.5f + index * 2.1f) * 4f
    val scaleVal = 0.5f + fp * 0.55f
    alpha = (fp * 0.92f).coerceIn(0f, 1f)
    translationX = bobX.dp.toPx()
    translationY = ((1f - fp) * 20f + bobY).dp.toPx()
    scaleX = scaleVal
    scaleY = scaleVal
    rotationZ = baseTilt + sin(timeTick * 5f + index) * 6f
}
