package com.onlasdan.comic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.theme.DarkSurfaceVariant
import com.onlasdan.comic.ui.theme.NoirText
import com.onlasdan.comic.ui.theme.ReaderSlotErrorBg
import com.onlasdan.comic.ui.theme.SoftBlush
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.theme.TextSecondary
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import com.onlasdan.comic.ui.util.animateNoirFloatAsState
import com.onlasdan.comic.ui.util.rememberReducedMotion
import kotlinx.coroutines.withContext

/**
 * State visual slot halaman reader: bedakan loading, auto-retry, dan error
 * agar tidak ada lagi "lubang hitam" yang ambigu.
 */
sealed interface ReaderPageSlotState {
    data object Loading : ReaderPageSlotState
    data class Retrying(val attempt: Int, val maxAttempts: Int) : ReaderPageSlotState
    data object Error : ReaderPageSlotState
}

/** Jumlah maksimal percobaan ulang otomatis sebelum menyerah ke tombol manual. */
const val ReaderAutoRetryMaxAttempts = 3

private val SlotEdgeColor = Color.White.copy(alpha = 0.06f)

/**
 * Slot halaman yang belum termuat: skeleton Noir (bukan hitam polos) dengan
 * divider atas-bawah, nomor halaman samar, dan indikator status sesuai state.
 * Semua animasi pure spring, kebal terhadap system animator 0x.
 */
@Composable
fun ReaderPageSlot(
    pageNumber: Int,
    state: ReaderPageSlotState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val isError = state is ReaderPageSlotState.Error
    val slotBg = if (isError) ReaderSlotErrorBg else DarkSurfaceVariant
    val edgeColor = if (isError) SoftBlush.copy(alpha = 0.25f) else SlotEdgeColor

    // Breathing pulse — hanya saat loading/retrying; berhenti total saat error.
    val breatheSpec = if (reducedMotion) NoirMotion.stiffSpring() else NoirMotion.breatheSpring()
    val pulse = remember { Animatable(0.55f) }
    LaunchedEffect(state, breatheSpec) {
        if (state is ReaderPageSlotState.Error) {
            pulse.snapTo(1f)
        } else {
            withContext(NoirForceMotionScale) {
                while (true) {
                    pulse.animateTo(1f, breatheSpec)
                    pulse.animateTo(0.55f, breatheSpec)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = edgeColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .background(slotBg.copy(alpha = pulse.value)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "#$pageNumber",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.12f),
            )
            SlotStatus(state = state, onRetry = onRetry)
        }
        HorizontalDivider(thickness = 1.dp, color = edgeColor)
    }
}

@Composable
private fun BoxScope.SlotStatus(
    state: ReaderPageSlotState,
    onRetry: () -> Unit,
) {
    when (state) {
        is ReaderPageSlotState.Loading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BlushSpinner()
                Text(
                    text = stringResource(R.string.reader_page_loading),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }

        is ReaderPageSlotState.Retrying -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BlushSpinner()
                Text(
                    text = stringResource(
                        R.string.reader_page_retrying,
                        state.attempt,
                        state.maxAttempts,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }

        is ReaderPageSlotState.Error -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = null,
                    tint = SoftBlush.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.reader_page_error),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                RetryCapsule(onRetry = onRetry)
            }
        }
    }
}

/**
 * Spinner melingkar tipis Blush: busur berputar via spring loop
 * (snap di 360f mulus karena identik dengan 0f), bypass 0x.
 */
@Composable
private fun BlushSpinner() {
    val rotation = remember { Animatable(0f) }
    val spec = NoirMotion.shimmerSpring()
    LaunchedEffect(spec) {
        withContext(NoirForceMotionScale) {
            while (true) {
                rotation.animateTo(360f, spec)
                rotation.snapTo(0f)
            }
        }
    }
    val density = LocalDensity.current
    val stroke = remember(density) {
        Stroke(width = with(density) { 2.dp.toPx() }, cap = StrokeCap.Round)
    }
    Canvas(modifier = Modifier.size(30.dp)) {
        drawArc(
            color = SoftBlush,
            startAngle = rotation.value,
            sweepAngle = 100f,
            useCenter = false,
            style = stroke,
        )
    }
}

/**
 * Tombol kapsul retry: tactile feedback scale 0.94f -> 1.0f dengan
 * Spatial Spring (0.8f, 800f), bypass 0x via animateNoirFloatAsState.
 */
@Composable
private fun RetryCapsule(onRetry: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateNoirFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = NoirMotion.spatialSpring(),
        label = "retryCapsulePress",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(SoftBlush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRetry,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.reader_page_retry),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = NoirText,
        )
    }
}
