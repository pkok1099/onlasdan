package com.onlasdan.comic.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.onlasdan.comic.ui.theme.*
import com.onlasdan.comic.ui.util.NoirForceMotionScale
import com.onlasdan.comic.ui.util.NoirMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val icon: ImageVector? = null,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

object AppToast {
    private val _currentToast = MutableStateFlow<ToastMessage?>(null)
    val currentToast = _currentToast.asStateFlow()
    private var dismissJob: Job? = null

    fun show(
        text: String,
        icon: ImageVector? = null,
        isError: Boolean = false,
        isSuccess: Boolean = false,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        dismissJob?.cancel()
        _currentToast.value = ToastMessage(
            text = text,
            icon = icon,
            isError = isError,
            isSuccess = isSuccess,
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }

    fun success(text: String, icon: ImageVector? = null) {
        show(text = text, icon = icon, isSuccess = true)
    }

    fun error(text: String, icon: ImageVector? = null) {
        show(text = text, icon = icon, isError = true)
    }

    // Toast beraksi (Fase A UX): tombol seperti "Urungkan" dengan durasi
    // tampil lebih panjang (5 detik) agar sempat dijangkau pengguna.
    fun action(text: String, actionLabel: String, icon: ImageVector? = null, onAction: () -> Unit) {
        show(text = text, icon = icon, actionLabel = actionLabel, onAction = onAction)
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentToast.value = null
    }

    internal fun setDismissJob(job: Job) {
        dismissJob = job
    }
}

/**
 * Host notifikasi Toast bertema Noir & Blush di bawah status bar.
 * Kebal terhadap Developer Options 0x via withContext(NoirForceMotionScale).
 */
@Composable
fun NoirToastHost(
    modifier: Modifier = Modifier,
) {
    val toast by AppToast.currentToast.collectAsState()
    val view = LocalView.current
    val density = LocalDensity.current
    val slidePx = with(density) { 48.dp.toPx() }

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(-slidePx) }
    var displayedToast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(toast) {
        val current = toast
        if (current != null) {
            displayedToast = current
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val job = launch {
                withContext(NoirForceMotionScale) {
                    launch { alpha.animateTo(1f, NoirMotion.effectsFastSpring()) }
                    offsetY.animateTo(0f, NoirMotion.pillThrowSpring())
                }
                delay(if (current.actionLabel != null) 5000L else 2600L)
                withContext(NoirForceMotionScale) {
                    launch { alpha.animateTo(0f, NoirMotion.effectsFastSpring()) }
                    offsetY.animateTo(-slidePx, NoirMotion.pillDropSpring())
                }
                AppToast.dismiss()
                displayedToast = null
            }
            AppToast.setDismissJob(job)
        } else {
            withContext(NoirForceMotionScale) {
                launch { alpha.animateTo(0f, NoirMotion.effectsFastSpring()) }
                offsetY.animateTo(-slidePx, NoirMotion.pillDropSpring())
            }
            displayedToast = null
        }
    }

    val activeMsg = displayedToast
    if (activeMsg != null && alpha.value > 0.005f) {
        val accentColor = when {
            activeMsg.isError -> ErrorRed
            activeMsg.isSuccess -> SuccessGreen
            else -> PrimaryPink
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .zIndex(9999f)
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            ToastPill(msg = activeMsg, accentColor = accentColor) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                activeMsg.onAction?.invoke()
                AppToast.dismiss()
            }
        }
    }
}

// Label aksi toast (mis. "Urungkan") — diekstrak dari NoirToastHost agar
// kompleksitas kognitif host tetap di bawah gerbang detekt.
@Composable
private fun ToastActionLabel(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

// Pil toast: permukaan + baris isi (ikon/titik aksen, teks, label aksi).
// Diekstrak dari NoirToastHost demi gerbang kompleksitas kognitif detekt.
@Composable
private fun ToastPill(msg: ToastMessage, accentColor: Color, onActionClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = NoirSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (msg.icon != null) {
                Icon(imageVector = msg.icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            } else {
                // Rose/Accent dot indicator ala card.html
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accentColor))
            }
            Text(text = msg.text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
            if (msg.actionLabel != null) {
                ToastActionLabel(label = msg.actionLabel, color = accentColor, onClick = onActionClick)
            }
        }
    }
}
