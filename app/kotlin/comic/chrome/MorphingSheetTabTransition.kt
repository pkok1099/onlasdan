package comic.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import comic.util.NoirForceMotionScale
import comic.util.NoirMotion
import comic.util.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class TabTransitionState(
    val prevTab: Animatable<Float, *>,
    val alpha: Animatable<Float, *>,
    val scale: Animatable<Float, *>,
    val offsetX: Animatable<Float, *>,
    val shake: Animatable<Float, *>,
)

@Composable
private fun rememberTabTransitionState(initialTab: Int): TabTransitionState = remember {
    TabTransitionState(
        prevTab = Animatable(initialTab.toFloat()),
        alpha = Animatable(1f),
        scale = Animatable(1f),
        offsetX = Animatable(0f),
        shake = Animatable(0f),
    )
}

private suspend fun animateTabShift(scope: CoroutineScope, state: TabTransitionState, dir: Float) {
    withContext(NoirForceMotionScale) {
        state.alpha.snapTo(0.30f)
        state.scale.snapTo(0.94f)
        state.offsetX.snapTo(dir * 32f)
        state.shake.snapTo(1f)
        scope.launch { state.alpha.animateTo(1f, NoirMotion.Effects.Fast) }
        scope.launch { state.scale.animateTo(1f, NoirMotion.SpatialFast.asFloat) }
        scope.launch { state.offsetX.animateTo(0f, NoirMotion.SpatialFast.asFloat) }
        scope.launch { state.shake.animateTo(0f, NoirMotion.SpatialMedium.asFloat) }
    }
}

/**
 * Kontainer transisi sheet-to-sheet di dalam UnifiedMorphingDock
 * dengan hentakan directional slide dan impact pop pegas 100%.
 */
@Composable
fun MorphingSheetTabTransition(
    currentTab: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val state = rememberTabTransitionState(currentTab)

    LaunchedEffect(currentTab) {
        val dir = (currentTab - state.prevTab.value).sign
        state.prevTab.snapTo(currentTab.toFloat())
        if (dir != 0f && !reducedMotion) animateTabShift(this, state, dir)
    }

    Box(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            val shakeVal = state.shake.value
            val shakeOffset = if (shakeVal > 0.01f) sin(shakeVal * 3.0 * PI).toFloat() * 4.5f else 0f
            alpha = state.alpha.value
            scaleX = state.scale.value
            scaleY = state.scale.value
            translationX = state.offsetX.value + shakeOffset
        },
    ) {
        content(currentTab)
    }
}
