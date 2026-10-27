package comic.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * Palet warna komik pop vibran (Manga, Manhwa, Manhua) yang selaras dengan
 * Pull-To-Refresh Elastic Noir v2.4 & Halftone Studio (card.html).
 */
object ComicVibrantColors {
    val ManhwaCyan = Color(0xFF00F0FF)
    val ManhwaSky = Color(0xFF38BDF8)
    val ManhwaIndigo = Color(0xFF6366F1)

    val MangaYellow = Color(0xFFFFE600)
    val MangaAmber = Color(0xFFFFB703)
    val MangaGold = Color(0xFFFFD166)

    val ManhuaOrange = Color(0xFFFF5500)
    val ManhuaCoral = Color(0xFFFF6B6B)
    val ManhuaCrimson = Color(0xFFE63946)

    val ComicPurple = Color(0xFFA855F7)
    val ComicViolet = Color(0xFFC084FC)
    val ComicPink = Color(0xFFFF1E7A)
    val ComicNeonPink = Color(0xFFFF007A)

    val AuraEmerald = Color(0xFF10B981)
    val AuraMint = Color(0xFF34D399)

    val HalftoneDotDark = Color(0x18FFFFFF)
    val ScreentoneOverlay = Color(0x0E00F0FF)
    val InkShadow = Color(0x55000000)

    fun forOrigin(origin: String?): Color = when (origin?.lowercase()) {
        "manhwa" -> ManhwaCyan
        "manhua" -> ManhuaOrange
        "manga" -> MangaYellow
        "doujinshi" -> ComicPink
        else -> ComicPink
    }
}

/**
 * Cached GPU-accelerated Tile Shader untuk pola titik halftone dual-warna (Pink + Cyan)
 * sesuai `.card-halftone-pattern` & `.card-halftone-cyan` di prototipe card.html.
 * Merender ribuan titik dalam 1 operasi GPU `drawRect` (O(1), 0 lag).
 */
val CardHalftoneShaderBrush: ShaderBrush by lazy {
    val tilePx = 20
    val bitmap = ImageBitmap(tilePx, tilePx)
    val canvas = Canvas(bitmap)
    val pinkPaint = Paint().apply {
        color = ComicVibrantColors.ComicPink.copy(alpha = 0.82f)
        isAntiAlias = true
    }
    val cyanPaint = Paint().apply {
        color = ComicVibrantColors.ManhwaCyan.copy(alpha = 0.72f)
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(5f, 5f), 4.2f, pinkPaint)
    canvas.drawCircle(Offset(15f, 15f), 4.0f, cyanPaint)
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

val AmbientHalftoneShaderBrush: ShaderBrush by lazy {
    val tilePx = 26
    val bitmap = ImageBitmap(tilePx, tilePx)
    val canvas = Canvas(bitmap)
    val dotPaint = Paint().apply {
        color = Color.White
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(13f, 13f), 1.6f, dotPaint)
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

/**
 * Halftone Screentone Shader untuk Skeleton: screentone cetak komik dual-density
 * (dot utama + "insurance dot") dengan satu aksen cyan per tile — pola checkerboard
 * yang seamless by construction, terbaca jelas sebagai tekstur manga tanpa berisik.
 */
val SkeletonHalftoneShaderBrush: ShaderBrush by lazy {
    val tilePx = 24
    val bitmap = ImageBitmap(tilePx, tilePx)
    val canvas = Canvas(bitmap)
    val primaryDot = Paint().apply {
        color = Color.White.copy(alpha = 0.14f)
        isAntiAlias = true
    }
    val insuranceDot = Paint().apply {
        color = Color.White.copy(alpha = 0.09f)
        isAntiAlias = true
    }
    val accentDot = Paint().apply {
        color = ComicVibrantColors.ManhwaCyan.copy(alpha = 0.10f)
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(7f, 7f), 2.3f, primaryDot)
    canvas.drawCircle(Offset(19f, 7f), 1.3f, insuranceDot)
    canvas.drawCircle(Offset(7f, 19f), 1.3f, insuranceDot)
    canvas.drawCircle(Offset(19f, 19f), 1.7f, accentDot)
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

/**
 * Manga / Manhwa Halftone Screentone Pattern overlay menggunakan GPU ShaderBrush (O(1)).
 */
fun DrawScope.drawMangaHalftone(
    color: Color = ComicVibrantColors.HalftoneDotDark,
    stepPx: Float = 16f,
    dotRadiusPx: Float = 1.6f,
) {
    val effectiveAlpha = color.alpha.coerceIn(0f, 1f)
    if (effectiveAlpha > 0.002f) {
        drawRect(brush = AmbientHalftoneShaderBrush, alpha = effectiveAlpha)
    }
}

/**
 * Manga Speedlines / Action Lines yang memancar dari titik tengah/sudut.
 */
fun DrawScope.drawMangaSpeedlines(
    color: Color = Color.White.copy(alpha = 0.08f),
    lineCount: Int = 16,
    strokeWidthPx: Float = 1.2f,
    centerOffset: Offset = Offset(size.width / 2f, size.height / 2f),
) {
    val radius = maxOf(size.width, size.height) * 1.2f
    val angleStep = (2.0 * Math.PI / lineCount).toFloat()
    for (i in 0 until lineCount) {
        val angle = i * angleStep
        val startX = centerOffset.x + (radius * 0.45f) * cos(angle)
        val startY = centerOffset.y + (radius * 0.45f) * sin(angle)
        val endX = centerOffset.x + radius * cos(angle)
        val endY = centerOffset.y + radius * sin(angle)
        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidthPx,
        )
    }
}

/**
 * Ambient Comic Energy Aura Canvas: Dirender secara statis-cached (drawWithCache)
 * tanpa loop per-frame tak terbatas agar 120fps terjaga mulus.
 */
@Composable
fun ComicMangaAmbientBackdrop(
    modifier: Modifier = Modifier,
    origin: String? = null,
) {
    val accentColor = ComicVibrantColors.forOrigin(origin)
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.07f),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.3f, 0f),
                    )
                    drawCircle(
                        color = ComicVibrantColors.ManhwaCyan.copy(alpha = 0.04f),
                        radius = size.width * 0.75f,
                        center = Offset(size.width, size.height),
                    )
                    drawRect(brush = AmbientHalftoneShaderBrush, alpha = 0.022f)
                }
            },
    )
}
