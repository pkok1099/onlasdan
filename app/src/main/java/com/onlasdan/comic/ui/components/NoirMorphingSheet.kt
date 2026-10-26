package com.onlasdan.comic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.NoirSurface
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Floating Morphing Sheet ("Noir & Blush" — Pure Physics).
 *
 * Mengambang tepat di atas Pill Dock (bukan ModalBottomSheet bawaan).
 * - Lebar: Selaras persis dengan Pill Dock (widthIn max 380.dp, padding horizontal 14.dp).
 * - Tinggi: wrapContentHeight() dengan batas maksimum (tanpa space kosong di bawah).
 * - Sudut: RoundedCornerShape(28.dp) di keempat sudut.
 * - Animasi: Mekar dari titik tombol Pill Dock (scaleX 0.2f->1f, scaleY 0.1f->1f, translationY 80f->0f)
 *   menggunakan NoirSpatialHeavy (spring dampingRatio = 0.82f, stiffness = 380f).
 */
@Composable
fun NoirMorphingSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorXFraction: Float,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val animProgress = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isRendered by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        withContext(NoirForceMotionScale) {
            if (visible) {
                isRendered = true
                launch { alphaAnim.animateTo(1f, NoirMotion.Effects.Float) }
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = NoirMotion.SpatialHeavy.asFloat,
                )
            } else {
                launch { dragOffsetY.animateTo(0f, NoirMotion.SpatialFast.asFloat) }
                launch { alphaAnim.animateTo(0f, NoirMotion.Effects.Float) }
                animProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = NoirMotion.SpatialHeavy.asFloat,
                )
                isRendered = false
            }
        }
    }

    if (!isRendered && !visible && animProgress.value < 0.01f) return

    val p = animProgress.value

    // Transformasi fisika: mekar dari tombol di Pill Dock
    val currentScaleX = 0.2f + 0.8f * p
    val currentScaleY = 0.1f + 0.9f * p
    val currentTranslationY = 70f * (1f - p) + dragOffsetY.value

    BackHandler(enabled = visible) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Occupy full parent so BottomCenter alignment works
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 96.dp) // Tepat di atas Pill Dock (56dp tinggi + 24dp padding + ~16dp gap)
            .padding(horizontal = 20.dp), // Lebar sejajar dengan Pill Dock
        contentAlignment = Alignment.BottomCenter, // <-- KUNCI: Sheet duduk di bawah, bukan lari ke atas!
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp) // Selaras dengan batas lebar Pill Dock
                .wrapContentHeight()
                .heightIn(max = maxHeight)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(anchorXFraction, 1.0f)
                    scaleX = currentScaleX
                    scaleY = currentScaleY
                    translationY = currentTranslationY
                    alpha = alphaAnim.value
                },
            shape = RoundedCornerShape(28.dp),
            color = NoirSurface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryPink.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                ),
            ),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 16.dp),
            ) {
                // M3 Drag Handle Bar (bisa di-swipe down untuk menutup)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp)
                        .draggable(
                            state = rememberDraggableState { delta ->
                                val next = (dragOffsetY.value + delta).coerceAtLeast(0f)
                                scope.launch { dragOffsetY.snapTo(next) }
                            },
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                if (velocity > 800f || dragOffsetY.value > 100f) {
                                    onDismiss()
                                } else {
                                    scope.launch {
                                        withContext(NoirForceMotionScale) {
                                            dragOffsetY.animateTo(0f, NoirMotion.SpatialFast.asFloat)
                                        }
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                    )
                }

                // Konten sheet (membungkus konten secara pas tanpa spasi kosong)
                content()
            }
        }
    }
}
