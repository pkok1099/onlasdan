package comic.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import comic.util.NoirForceMotionScale
import comic.util.NoirMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hard offset shadow komik (rombak komik 2026-10-27): blok warna padat TANPA blur di
 * belakang konten, digeser (offsetX, offsetY) — kesan cetakan/risograph dua lapis.
 *
 * Digambar via drawBehind sehingga NOL pengaruh ke layout (ukuran & posisi node tetap).
 * Pemakaian: pasang SEBELUM background/border pada node yang sama atau di luar Surface
 * (Surface meng-clip ke shape-nya, jadi shadow di dalam Surface akan terpotong):
 *
 * ```
 * Surface(
 *     modifier = Modifier
 *         .comicHardShadow(ComicSfxYellow, 3.dp, 3.dp)
 *         .comicPress(interactionSource),
 *     ...
 * )
 * ```
 *
 * Dipasang bersama [comicPress]: tekan → konten bergeser mengikuti arah bayangan dan
 * "tenggelam" ke dalamnya (mekanik tombol komik klasik), lepas → membal via pegas.
 */
fun Modifier.comicHardShadow(
    color: Color,
    offsetX: Dp = 3.dp,
    offsetY: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
): Modifier = composed {
    val density = LocalDensity.current
    val dx = with(density) { offsetX.toPx() }
    val dy = with(density) { offsetY.toPx() }
    val r = with(density) { cornerRadius.toPx() }
    drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(dx, dy),
            size = size,
            cornerRadius = CornerRadius(r, r),
        )
    }
}

/**
 * Micro-interaction tekan ala komik: node bergeser ke arah (offsetX, offsetY) saat
 * ditekan — mengikuti arah [comicHardShadow] — plus opsional scale ringan. Draw-phase
 * (ModifierNodeElement) seperti [comic.util.pressScale]: nol recomposition
 * per frame, kebal Developer Options 0x via withContext(NoirForceMotionScale), dan
 * reduced-motion tetap lewat pegas kaku sistem Noir (NoirMotion.Press).
 */
fun Modifier.comicPress(
    interactionSource: MutableInteractionSource,
    offsetX: Dp = 3.dp,
    offsetY: Dp = 3.dp,
    pressedScale: Float = 1f,
): Modifier = this then ComicPressElement(interactionSource, offsetX, offsetY, pressedScale)

private data class ComicPressElement(
    val interactionSource: MutableInteractionSource,
    val offsetX: Dp,
    val offsetY: Dp,
    val pressedScale: Float,
) : ModifierNodeElement<ComicPressNode>() {
    override fun create(): ComicPressNode = ComicPressNode(interactionSource, offsetX, offsetY, pressedScale)

    override fun update(node: ComicPressNode) {
        node.update(interactionSource, offsetX, offsetY, pressedScale)
    }
}

private class ComicPressNode(
    private var interactionSource: MutableInteractionSource,
    private var offsetX: Dp,
    private var offsetY: Dp,
    private var pressedScale: Float,
) : Modifier.Node(),
    DrawModifierNode {
    private val pressAnim = Animatable(0f)
    private var collectJob: Job? = null

    override fun onAttach() {
        startCollecting()
    }

    override fun onDetach() {
        collectJob?.cancel()
        collectJob = null
    }

    fun update(
        newSource: MutableInteractionSource,
        newOffsetX: Dp,
        newOffsetY: Dp,
        newPressedScale: Float,
    ) {
        val sourceChanged = interactionSource != newSource
        interactionSource = newSource
        offsetX = newOffsetX
        offsetY = newOffsetY
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
                    pressAnim.animateTo(if (isPressed) 1f else 0f, NoirMotion.Press.Float)
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val v = pressAnim.value
        if (v == 0f) {
            drawContent()
            return
        }
        val density = density
        val dx = with(density) { offsetX.toPx() } * v
        val dy = with(density) { offsetY.toPx() } * v
        val s = 1f - (1f - pressedScale) * v
        translate(dx, dy) {
            if (s != 1f) {
                scale(s, s) { this@draw.drawContent() }
            } else {
                this@draw.drawContent()
            }
        }
    }
}

/**
 * Starburst komik ("action burst") untuk badge/sticker — poligon berduri dua radius.
 * [innerRatio] makin kecil = duri makin tajam; [rotationDegrees] memutar agar duri
 * tidak selalu vertikal (kesan stempel tangan).
 */
class ComicBurstShape(
    private val points: Int = 12,
    private val innerRatio: Float = 0.70f,
    private val rotationDegrees: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(size.width, size.height) / 2f
        val rot = (rotationDegrees * PI / 180.0).toFloat()
        val steps = (points * 2).coerceAtLeast(6)
        val path = Path()
        for (i in 0 until steps) {
            val angle = rot + (i * 2.0 * PI / steps).toFloat()
            val r = if (i % 2 == 0) maxR else maxR * innerRatio
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Speech bubble / caption box komik: rounded rect dengan ekor segitiga di tepi bawah
 * (ekor DI DALAM bounds sehingga aman dari clip induk). Untuk pesan empty/error state.
 *
 * @param cornerRatio radius sudut relatif terhadap dimensi terkecil.
 * @param tailAnchorX posisi horizontal puncak ekor (0..1 dari lebar).
 */
class ComicSpeechBubbleShape(
    private val cornerRatio: Float = 0.16f,
    private val tailWidthFraction: Float = 0.16f,
    private val tailHeightFraction: Float = 0.26f,
    private val tailAnchorX: Float = 0.72f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = min(w, h) * cornerRatio
        val tailH = (h * tailHeightFraction).coerceAtMost(h * 0.4f)
        val bodyBottom = h - tailH
        val tailHalf = w * tailWidthFraction / 2f
        val tailX = (w * tailAnchorX).coerceIn(tailHalf + r, w - tailHalf - r)

        val body = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, bodyBottom, CornerRadius(r, r)))
        }
        val tail = Path().apply {
            moveTo(tailX - tailHalf, bodyBottom)
            lineTo(tailX + tailHalf, bodyBottom)
            lineTo(tailX, h)
            close()
        }
        val merged = Path().apply { op(body, tail, PathOperation.Union) }
        return Outline.Generic(merged)
    }
}

/**
 * Halftone dot field parametrik (screentone cetak) — tile GPU kecil yang diulang
 * (O(1) per frame seperti shader brush lain di ComicMangaEffects). Di-remember per
 * parameter agar tidak membuat bitmap baru setiap rekomposisi.
 *
 * @param cell jarak antar titik (ukuran tile).
 * @param dotRatio radius titik relatif terhadap tile (0..0.5).
 */
@Composable
fun rememberHalftoneBrush(
    color: Color,
    cell: Dp = 14.dp,
    dotRatio: Float = 0.20f,
): ShaderBrush {
    val density = LocalDensity.current
    return remember(color, cell, dotRatio, density.density) {
        val cellPx = (cell.value * density.density).toInt().coerceIn(8, 64)
        val bitmap = ImageBitmap(cellPx, cellPx)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        canvas.drawCircle(Offset(cellPx / 2f, cellPx / 2f), cellPx * dotRatio, paint)
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }
}
