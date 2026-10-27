package comic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comic.theme.ComicSfxCyan
import comic.theme.ComicSfxPink
import comic.theme.ComicSfxYellow
import comic.theme.ComicSublabelSurface
import comic.theme.InterFontFamily
import comic.theme.PillShape
import comic.theme.PureBlack
import comic.theme.Spacing
import comic.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

private val EnergyBarBrush = Brush.horizontalGradient(
    colors = listOf(ComicSfxCyan, ComicSfxPink, ComicSfxYellow),
)

private val BaseSfxCharStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 38.sp,
)

@Composable
internal fun BoxScope.ComicPullForeground(state: ComicPullState) {
    if (state.isRefreshingVisual) {
        ComicPullBurstStage(state = state)
    } else {
        ComicSfxPullStage(
            state = state,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ComicSfxPullStage(
    state: ComicPullState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(state.charSprings.size) { idx ->
                MangaExtrudedSfxChar(state = state, index = idx)
            }
        }
        ComicSfxMetaWrap(sfx = state.currentSfx, progress = state.progress)
    }
}

@Composable
private fun MangaExtrudedSfxChar(state: ComicPullState, index: Int) {
    val spring = state.charSprings[index]
    val p = spring.pos.coerceAtLeast(0f)
    if (p <= 0.01f) return
    val charText = state.currentSfx.chars.getOrElse(index) { "" }
    val readyTension = if (state.progress >= 1.0f) 2.2f else 0f

    Box(
        modifier = Modifier.sfxCharTransform(spring, p, index, state.timeTick, readyTension),
        contentAlignment = Alignment.Center,
    ) {
        MangaCharLayers(char = charText, isHighlight = index == 2)
    }
}

private fun Modifier.sfxCharTransform(
    spring: CharSpringState,
    p: Float,
    index: Int,
    timeTick: Float,
    readyTension: Float,
): Modifier = graphicsLayer {
    val clampedP = p.coerceAtMost(1f)
    val floatY = sin(timeTick * 5f + index * 1.4f) * 2.5f
    val jitterX = if (readyTension > 0f) sin(timeTick * 45f + index * 2f) * readyTension else 0f
    val jitterRot = if (readyTension > 0f) cos(timeTick * 38f + index) * 3.5f else 0f
    val scaleVal = 0.2f + p * 0.88f
    alpha = (p * 1.4f).coerceIn(0f, 1f)
    translationX = jitterX.dp.toPx()
    translationY = ((1f - clampedP) * 28f + floatY).dp.toPx()
    scaleX = scaleVal
    scaleY = scaleVal
    rotationZ = spring.rotAngle * (1.2f - clampedP * 0.5f) + jitterRot
}

@Composable
private fun MangaCharLayers(char: String, isHighlight: Boolean) {
    if (!isHighlight) {
        Text(
            text = char,
            style = BaseSfxCharStyle,
            color = ComicSfxCyan,
            modifier = Modifier.offset { IntOffset((-2.5).dp.roundToPx(), (-1.5).dp.roundToPx()) },
        )
    }
    Text(
        text = char,
        style = BaseSfxCharStyle,
        color = ComicSfxPink,
        modifier = Modifier.offset { IntOffset(3.5.dp.roundToPx(), 4.5.dp.roundToPx()) },
    )
    Text(
        text = char,
        style = BaseSfxCharStyle.copy(drawStyle = Stroke(width = 10f, join = StrokeJoin.Round)),
        color = PureBlack,
    )
    val glowColor = if (isHighlight) ComicSfxYellow.copy(alpha = 0.65f) else ComicSfxPink.copy(alpha = 0.55f)
    Text(
        text = char,
        style = BaseSfxCharStyle.copy(shadow = Shadow(glowColor, Offset(0f, 6f), 18f)),
        color = if (isHighlight) ComicSfxYellow else TextPrimary,
    )
}

@Composable
private fun ComicSfxMetaWrap(sfx: ComicSfx, progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ComicSfxEnergyBar(progress = progress)
        ComicSfxSublabelPill(sfx = sfx, progress = progress)
    }
}

@Composable
private fun ComicSfxEnergyBar(progress: Float) {
    val barAlpha = if (progress > 0.15f) 1f else 0f
    val fillFraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(4.dp)
            .graphicsLayer { alpha = barAlpha }
            .clip(PillShape)
            .background(TextPrimary.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillFraction)
                .background(EnergyBarBrush, PillShape),
        )
    }
}

@Composable
private fun ComicSfxSublabelPill(sfx: ComicSfx, progress: Float) {
    val subAlpha = ((progress - 0.45f) / 0.45f).coerceIn(0f, 1f)
    if (subAlpha <= 0.01f) return
    val isReady = progress >= 1.0f
    val borderCol = if (isReady) ComicSfxPink.copy(alpha = 0.65f) else TextPrimary.copy(alpha = 0.12f)
    val subScale = if (isReady) 1.05f else 1f
    val fillFraction = progress.coerceIn(0f, 1f)

    Text(
        text = sfx.sublabel,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 9.5.sp,
        color = TextPrimary.copy(alpha = 0.88f),
        letterSpacing = 1.3.sp,
        modifier = Modifier
            .graphicsLayer {
                alpha = subAlpha
                translationY = ((1f - fillFraction) * 8f).dp.toPx()
                scaleX = subScale
                scaleY = subScale
            }
            .background(ComicSublabelSurface.copy(alpha = 0.78f), PillShape)
            .border(1.dp, borderCol, PillShape)
            .padding(horizontal = 9.dp, vertical = 2.dp),
    )
}
