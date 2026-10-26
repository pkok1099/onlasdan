package com.onlasdan.comic.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.onlasdan.comic.ui.theme.AmbientHalftoneShaderBrush
import com.onlasdan.comic.ui.theme.ComicVibrantColors
import com.onlasdan.comic.ui.theme.drawMangaSpeedlines

/**
 * Latar belakang komik pop untuk bottom sheet (ComicDetailSheet, dll) dengan
 * GPU ShaderBrush dan drawWithCache (0 CPU overhead per frame).
 */
@Composable
fun ComicSheetAmbientBackdrop(
    modifier: Modifier = Modifier,
    origin: String? = null,
) {
    val accentColor = ComicVibrantColors.forOrigin(origin)
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawSheetComicEffects(accentColor)
                }
            },
    )
}

private fun DrawScope.drawSheetComicEffects(accent: Color) {
    drawCircle(
        color = accent.copy(alpha = 0.08f),
        radius = size.width * 0.70f,
        center = Offset(size.width * 0.85f, 0f),
    )
    drawCircle(
        color = ComicVibrantColors.ManhwaCyan.copy(alpha = 0.045f),
        radius = size.width * 0.65f,
        center = Offset(0f, size.height * 0.9f),
    )
    drawRect(brush = AmbientHalftoneShaderBrush, alpha = 0.032f)
    drawMangaSpeedlines(
        color = Color.White.copy(alpha = 0.035f),
        lineCount = 10,
        strokeWidthPx = 1.0f,
        centerOffset = Offset(size.width, 0f),
    )
}
