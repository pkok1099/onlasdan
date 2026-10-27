package comic.util

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import comic.theme.CardHalftoneShaderBrush
import comic.theme.ComicVibrantColors
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val SfxEnterPool = listOf(
    "두둥! KUNG!", "번쩍! FLASH!", "슈우욱! DASH!",
    "ズバッ! ZUBAT!", "バァン! BAAN!", "ゴゴゴ! AURA!",
    "轰隆! HONG!", "破! BREAK!", "화아아! BURST!",
)

private val StampTextPaint: AndroidPaint by lazy {
    AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(255, 230, 0)
        textAlign = AndroidPaint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
    }
}

private fun pickCardEnterSfx(index: Int, triggerKey: Any?): String {
    val hash = ((index * 31) + (triggerKey?.hashCode() ?: 0)) and Int.MAX_VALUE
    return SfxEnterPool[hash % SfxEnterPool.size]
}

private suspend fun runHalftoneEntrance(
    progress: Animatable<Float, *>,
    index: Int,
    skipInitial: Boolean,
) {
    if (skipInitial) return
    val staggerMs = index.coerceIn(0, MotionTokens.StaggerCap) * MotionTokens.StaggerStepMs
    if (staggerMs > 0L) delay(staggerMs)
    withContext(NoirForceMotionScale) {
        progress.animateTo(1f, NoirMotion.SpatialSlow.asFloat)
    }
}

/**
 * Halftone Burst, Comic Impact Hentakan (Punchy Pop & Screen Shake),
 * dan SFX Stamp Pop Entrance Modifier sesuai prototipe card.html.
 */
fun Modifier.comicCardHalftoneEntrance(
    index: Int = 0,
    entranceDone: Boolean = false,
    triggerKey: Any? = Unit,
): Modifier = composed {
    val reduced = rememberReducedMotion()
    val skipInitial = remember(triggerKey) { entranceDone || reduced || index >= MotionTokens.EntranceMaxIndex }
    val progress = remember(triggerKey) { Animatable(if (skipInitial) 1f else 0f) }
    val sfxLabel = remember(index, triggerKey) { pickCardEnterSfx(index, triggerKey) }

    LaunchedEffect(triggerKey) {
        runHalftoneEntrance(progress, index, skipInitial)
    }

    this.applyHalftoneLayerAndOverlay(progress, sfxLabel)
}

private fun Modifier.applyHalftoneLayerAndOverlay(
    progress: Animatable<Float, *>,
    sfxLabel: String,
): Modifier = this
    .graphicsLayer {
        val p = progress.value
        alpha = (p * 2.0f).coerceIn(0f, 1f)
        val s = computeCardImpactScale(p)
        val shake = computeCardImpactShake(p)
        scaleX = s
        scaleY = s
        translationX = shake.x
        translationY = shake.y
        rotationZ = shakeShakeRotation(p)
    }
    .drawWithContent {
        drawContent()
        val p = progress.value
        if (p < 0.99f || progress.isRunning) {
            drawHalftoneAndSfxOverlay(p, sfxLabel)
        }
    }

private fun computeCardImpactScale(p: Float): Float = when {
    p < 0.45f -> 0.92f + (p / 0.45f) * 0.125f

    // 0.92 -> 1.045f hentakan pop
    p < 0.80f -> 1.045f - ((p - 0.45f) / 0.35f) * 0.055f

    // 1.045 -> 0.990f
    else -> 0.990f + ((p - 0.80f) / 0.20f) * 0.010f // 0.990 -> 1.0f
}

private fun computeCardImpactShake(p: Float): Offset {
    if (p >= 0.85f) return Offset.Zero
    val decay = (1f - p / 0.85f).coerceIn(0f, 1f)
    val phase = (p * 4.0 * PI).toFloat()
    val dx = sin(phase) * 6.5f * decay
    val dy = -sin(phase * 1.5f) * 4.5f * decay
    return Offset(dx, dy)
}

private fun shakeShakeRotation(p: Float): Float {
    if (p >= 0.75f) return 0f
    val decay = (1f - p / 0.75f).coerceIn(0f, 1f)
    return sin(p * 3.5 * PI).toFloat() * 1.6f * decay
}

private fun DrawScope.drawHalftoneAndSfxOverlay(p: Float, sfxLabel: String) {
    val clamped = p.coerceIn(0f, 1f)
    val flashAlpha = ((1f - clamped) * 0.95f).coerceIn(0f, 0.95f)
    if (flashAlpha > 0.01f) {
        drawRect(brush = CardHalftoneShaderBrush, alpha = flashAlpha)
        drawRect(color = Color.White, alpha = flashAlpha * 0.22f)
    }
    if (clamped in 0.03f..0.97f) {
        drawSfxStampPop(clamped, sfxLabel)
    }
}

private fun DrawScope.drawSfxStampPop(p: Float, sfxLabel: String) {
    val stampAlpha = computeStampAlpha(p)
    if (stampAlpha <= 0.02f) return
    val stampScale = computeStampScale(p)
    val rotDeg = if (p < 0.35f) -25f + (p / 0.35f) * 17f else -10f
    val risePx = if (p > 0.68f) -((p - 0.68f) / 0.32f) * 30f else 0f
    val center = Offset(size.width * 0.5f, size.height * 0.44f + risePx)

    withTransform({
        rotate(degrees = rotDeg, pivot = center)
        scale(scaleX = stampScale, scaleY = stampScale, pivot = center)
    }) {
        drawStampBoxAndText(center, stampAlpha, sfxLabel)
    }
}

private fun computeStampAlpha(p: Float): Float = when {
    p < 0.22f -> p / 0.22f
    p < 0.70f -> 1f
    else -> ((1f - p) / 0.30f).coerceIn(0f, 1f)
}

private fun computeStampScale(p: Float): Float = when {
    p < 0.35f -> 0.20f + (p / 0.35f) * 1.05f

    // 0.20 -> 1.25f (sfxStampPop)
    p < 0.75f -> 1.25f - ((p - 0.35f) / 0.40f) * 0.29f

    // 1.25 -> 0.96f
    else -> 0.96f + ((p - 0.75f) / 0.25f) * 0.14f // 0.96 -> 1.10f
}

private fun DrawScope.drawStampBoxAndText(center: Offset, alpha: Float, label: String) {
    val textSizePx = (size.width * 0.095f).coerceIn(20f, 34f)
    val paint = StampTextPaint.apply {
        textSize = textSizePx
        this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
    }
    val boxW = paint.measureText(label) + textSizePx * 0.9f
    val boxH = textSizePx * 1.55f
    val topLeft = Offset(center.x - boxW / 2f, center.y - boxH / 2f)
    drawStampNeoBrutalistFrame(topLeft, Size(boxW, boxH), alpha)
    drawContext.canvas.nativeCanvas.drawText(label, center.x, center.y + textSizePx * 0.34f, paint)
}

private fun DrawScope.drawStampNeoBrutalistFrame(topLeft: Offset, boxSize: Size, alpha: Float) {
    drawRect(ComicVibrantColors.ManhwaCyan.copy(alpha = 0.85f * alpha), topLeft + Offset(-4f, -4f), boxSize)
    drawRect(ComicVibrantColors.ComicPink.copy(alpha = 0.90f * alpha), topLeft + Offset(5f, 5f), boxSize)
    drawRect(Color(0xEB08060C).copy(alpha = 0.94f * alpha), topLeft, boxSize)
    drawRect(Color.White.copy(alpha = alpha), topLeft, boxSize, style = Stroke(width = 3f))
}
