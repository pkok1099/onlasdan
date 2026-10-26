package com.onlasdan.comic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.ui.theme.DarkOutlineVariant
import com.onlasdan.comic.ui.theme.DarkSurface
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Singleton dismiss spring spec dari Effects.Dismiss. */
private val DismissSpring = NoirMotion.Effects.Dismiss

/**
 * Menghitung opasitas scrim berdasarkan progress sheet (1.0f = full open, 0.0f = closed).
 * Scrim dipotong seketika ke 0f saat progress mencapai cutoffThreshold agar interaksi di bawahnya
 * langsung responsif tanpa menunggu animasi selesai sepenuhnya.
 */
internal fun calculateScrimAlpha(
    sheetProgress: Float,
    maxAlpha: Float = 0.6f,
    cutoffThreshold: Float = 0.12f,
): Float {
    if (sheetProgress <= cutoffThreshold) return 0f
    return (((sheetProgress - cutoffThreshold) / (1f - cutoffThreshold)).coerceIn(0f, 1f)) * maxAlpha
}

/**
 * Custom bottom sheet dengan kontrol penuh atas gesture dan physics.
 *
 * Menggantikan M3 ModalBottomSheet yang physics internal-nya tidak bisa dikustom.
 * Aturan (sesuai spek):
 * 1. Selama onDrag: snapTo() dengan hard clamp di expandedOffset (tanpa animateTo).
 *    Spring hanya di onDragStopped.
 * 2. Posisi via Modifier.offset (bukan ubah height) — tanpa remeasurement.
 * 3. NestedScrollConnection: sheet konsumsi drag ke atas sampai expanded,
 *    lalu serahkan ke konten. Sheet turun hanya jika konten sudah mentok atas.
 */
@Composable
fun NoirBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetHeight: Dp = Dp.Unspecified,
    dragHandle: @Composable () -> Unit = { NoirDragHandle() },
    content: @Composable BoxScope.() -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenHeightPx = windowInfo.containerSize.height.toFloat()
    val sheetHeightPx = if (sheetHeight == Dp.Unspecified) {
        screenHeightPx * 0.92f
    } else {
        with(density) { sheetHeight.toPx() }
    }
    val expandedOffset = screenHeightPx - sheetHeightPx
    val hiddenOffset = screenHeightPx

    val offsetY = remember { Animatable(hiddenOffset) }

    val sheetProgress = if (hiddenOffset > expandedOffset) {
        ((hiddenOffset - offsetY.value) / (hiddenOffset - expandedOffset)).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Masuk dengan spring saat pertama tampil.
    LaunchedEffect(Unit) {
        withContext(NoirForceMotionScale) {
            offsetY.animateTo(expandedOffset, animationSpec = NoirMotion.Spatial.asFloat)
        }
    }

    fun dismiss() {
        scope.launch {
            withContext(NoirForceMotionScale) {
                offsetY.animateTo(hiddenOffset, animationSpec = DismissSpring)
            }
            onDismiss()
        }
    }

    // Back menutup sheet dulu sebelum keluar layar (standar UX).
    BackHandler { dismiss() }

    // Nested scroll: prioritas sheet vs konten scrollable (LazyColumn).
    val nestedScrollConnection = remember(expandedOffset, hiddenOffset, sheetHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Geser ke ATAS (available.y negatif): jika sheet belum mentok expanded,
                // sheet konsumsi dulu sampai expanded.
                if (available.y < 0) {
                    val current = offsetY.value
                    if (current > expandedOffset) {
                        val consumed = available.y.coerceAtLeast(expandedOffset - current)
                        scope.launch { offsetY.snapTo((current + consumed).coerceAtLeast(expandedOffset)) }
                        return Offset(0f, consumed)
                    }
                    // Sheet sudah mentok atas — serahkan 100% ke konten.
                    return Offset.Zero
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Geser ke BAWAH (available.y positif): konten sudah mentok di paling atas,
                // sisa scroll dipakai untuk sedikit menarik sheet ke bawah jika digeser perlahan.
                if (available.y > 0) {
                    val current = offsetY.value
                    val newOffset = (current + available.y).coerceAtMost(hiddenOffset)
                    val consumedY = newOffset - current
                    scope.launch { offsetY.snapTo(newOffset) }
                    // Sheet hanya dismiss otomatis via drag konten jika ditarik sangat jauh (> 60% sheet).
                    if (newOffset >= expandedOffset + sheetHeightPx * 0.6f) {
                        dismiss()
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Kelebihan kecepatan fling dari konten list TIDAK BOLEH menutup sheet secara tiba-tiba.
                // Jika sheet sempat tertarik sedikit akibat overscroll, kembalikan posisi sheet ke expandedOffset.
                if (offsetY.value > expandedOffset + sheetHeightPx * 0.45f) {
                    dismiss()
                } else {
                    // Snap kembali ke posisi expanded dengan spring natural.
                    scope.launch {
                        withContext(NoirForceMotionScale) {
                            offsetY.animateTo(expandedOffset, animationSpec = NoirMotion.Spatial.asFloat)
                        }
                    }
                }
                return available
            }
        }
    }

    val draggableState = rememberDraggableState { delta ->
        // onDrag pada drag handle: snapTo dengan HARD CLAMP di expandedOffset (spek 1a).
        // Tidak ada animateTo di sini — posisi mengikuti jari.
        val current = offsetY.value
        val newOffset = (current + delta).coerceAtLeast(expandedOffset)
        scope.launch { offsetY.snapTo(newOffset) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        // Scrim layer terpisah dengan sinkronisasi 1:1 fase draw
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = if (hiddenOffset > expandedOffset) {
                        ((hiddenOffset - offsetY.value) / (hiddenOffset - expandedOffset)).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    alpha = calculateScrimAlpha(progress)
                }
                .background(Color.Black)
                .clickable(
                    enabled = sheetProgress > 0.12f,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    dismiss()
                },
        )

        // Sheet: posisi via offset (bukan height) — spek 2.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { sheetHeightPx.toDp() })
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, (offsetY.value - expandedOffset).roundToInt()) }
                .background(
                    DarkSurface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ),
        ) {
            // Background extension di bawah (spek 2): cegah bolong saat over-drag.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 199.dp)
                    .background(DarkSurface),
            )

            ComicSheetAmbientBackdrop()

            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle bar: draggable aktif HANYA di area handle ini agar tidak
                // memblokir gestur scroll LazyColumn konten di bawahnya.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                // Spring HANYA di sini (spek 1): jari sudah dilepas.
                                scope.launch {
                                    withContext(NoirForceMotionScale) {
                                        if (velocity > 800 || offsetY.value > expandedOffset + sheetHeightPx * 0.3f) {
                                            offsetY.animateTo(hiddenOffset, animationSpec = DismissSpring)
                                            onDismiss()
                                        } else {
                                            offsetY.animateTo(expandedOffset, animationSpec = NoirMotion.Spatial.asFloat)
                                        }
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    dragHandle()
                }

                // Konten bebas gesture interception: LazyColumn mengonsumsi scroll vertikal
                // sepenuhnya dan berkomunikasi via NestedScrollConnection di root Box.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun NoirDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(DarkOutlineVariant, RoundedCornerShape(2.dp))
                .height(4.dp)
                .fillMaxWidth(0.12f),
        )
    }
}
