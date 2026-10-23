package com.onlasdan.comic.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    ) {
        dismissJob?.cancel()
        _currentToast.value = ToastMessage(
            text = text,
            icon = icon,
            isError = isError,
            isSuccess = isSuccess,
        )
    }

    fun success(text: String, icon: ImageVector? = null) {
        show(text = text, icon = icon, isSuccess = true)
    }

    fun error(text: String, icon: ImageVector? = null) {
        show(text = text, icon = icon, isError = true)
    }

    fun dismiss() {
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
                delay(2600L)
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
                    if (activeMsg.icon != null) {
                        Icon(
                            imageVector = activeMsg.icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp),
                        )
                    } else {
                        // Rose/Accent dot indicator ala card.html
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                    }

                    Text(
                        text = activeMsg.text,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
