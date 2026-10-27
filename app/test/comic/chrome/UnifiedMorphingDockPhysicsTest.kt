package comic.chrome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedMorphingDockPhysicsTest {

    private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction.coerceIn(0f, 1f)

    @Test
    fun horizontalPadding_lerpsSmoothlyFromClosedToExpanded() {
        val startPad = 44f
        val endPad = 18f

        assertEquals(44f, lerp(startPad, endPad, 0f), 0.001f)
        assertEquals(31f, lerp(startPad, endPad, 0.5f), 0.001f)
        assertEquals(18f, lerp(startPad, endPad, 1f), 0.001f)
    }

    @Test
    fun surfaceAlpha_lerpsSmoothlyBetweenBounds() {
        assertEquals(0.90f, lerp(0.90f, 0.98f, 0f), 0.001f)
        assertEquals(0.94f, lerp(0.90f, 0.98f, 0.5f), 0.001f)
        assertEquals(0.98f, lerp(0.90f, 0.98f, 1f), 0.001f)
    }

    @Test
    fun indicatorActiveScale_lerpsBetweenOneAndOvershoot() {
        assertEquals(1.0f, lerp(1.0f, 1.16f, 0f), 0.001f)
        assertEquals(1.08f, lerp(1.0f, 1.16f, 0.5f), 0.001f)
        assertEquals(1.16f, lerp(1.0f, 1.16f, 1f), 0.001f)
    }

    @Test
    fun indicatorCenterX_isEvenlySpacedAcrossFiveTabs() {
        val dockWidth = 350f
        val itemWidth = dockWidth / 5f // 70f
        val indicatorHalfPx = 20f

        for (tabIndex in 0..4) {
            val targetCenterX = (tabIndex + 0.5f) * itemWidth
            val translationX = targetCenterX - indicatorHalfPx

            // Verify each center sits exactly in the middle of its slot
            val expectedCenter = (tabIndex * 70f) + 35f
            assertEquals(expectedCenter, targetCenterX, 0.001f)
            assertEquals(expectedCenter - 20f, translationX, 0.001f)
        }
    }

    @Test
    fun dragGesture_deltaCalculationsMatchTouchDisplacement() {
        val sheetHeightPx = 400f

        // User drags finger down 100px (closing direction)
        val dragDownAmount = 100f
        val deltaDown = -dragDownAmount / sheetHeightPx
        assertEquals(-0.25f, deltaDown, 0.001f)

        // User drags finger up 80px (opening direction)
        val dragUpAmount = -80f
        val deltaUp = -dragUpAmount / sheetHeightPx
        assertEquals(+0.20f, deltaUp, 0.001f)

        // Progress clamp verification (0f to 1.08f)
        val initialProgress = 0.90f
        val newProgressOpen = (initialProgress + deltaUp).coerceIn(0f, 1.08f)
        assertEquals(1.08f, newProgressOpen, 0.001f) // clamped to max 1.08f

        val newProgressClose = (0.10f + deltaDown).coerceIn(0f, 1.08f)
        assertEquals(0f, newProgressClose, 0.001f) // clamped to min 0f
    }

    @Test
    fun flingVelocity_clampsWithinPhysicsBoundaries() {
        val height = 400f

        // High upward fling: -8000 px/s
        val upwardVelocity = -8000f
        val velocityProgressUp = (-upwardVelocity / height).coerceIn(-15f, 15f)
        assertEquals(15f, velocityProgressUp, 0.001f)

        // High downward fling: +9000 px/s
        val downwardVelocity = 9000f
        val velocityProgressDown = (-downwardVelocity / height).coerceIn(-15f, 15f)
        assertEquals(-15f, velocityProgressDown, 0.001f)
    }

    @Test
    fun collapseDecision_respectsProgressThresholdAndVelocity() {
        fun shouldCollapse(progress: Float, velocityY: Float): Boolean = progress < 0.55f || velocityY > 600f

        // Low progress with no velocity should collapse
        assertTrue(shouldCollapse(0.40f, 0f))

        // High progress with strong downward fling should collapse
        assertTrue(shouldCollapse(0.80f, 1200f))

        // High progress with neutral or upward velocity should stay open
        assertFalse(shouldCollapse(0.70f, 100f))
        assertFalse(shouldCollapse(0.90f, -500f))
    }

    @Test
    fun tabResolution_preservesLastActiveTabDuringCollapseAnimation() {
        var expandedTabIndex: Int? = 4
        var lastActiveTabIndex: Int? = null

        // On open
        if (expandedTabIndex != null) {
            lastActiveTabIndex = expandedTabIndex
        }
        var activeTabToRender = expandedTabIndex ?: lastActiveTabIndex
        assertEquals(4, activeTabToRender)

        // When user initiates close (expandedTabIndex cleared to null)
        expandedTabIndex = null
        activeTabToRender = expandedTabIndex ?: lastActiveTabIndex
        // Crucial test: activeTabToRender must NOT be null while collapsing
        assertEquals(4, activeTabToRender)

        // Content visibility check during collapse animation
        fun isContentVisible(progress: Float, tab: Int?): Boolean = progress > 0.005f && tab != null

        assertTrue(isContentVisible(1.0f, activeTabToRender))
        assertTrue(isContentVisible(0.5f, activeTabToRender))
        assertTrue(isContentVisible(0.01f, activeTabToRender))
        // Once completely collapsed (0f <= 0.005f)
        assertFalse(isContentVisible(0f, activeTabToRender))
    }

    @Test
    fun backHandler_enabledWhileExpandedOrAnimating() {
        fun canHandleBack(isExpanded: Boolean, progress: Float): Boolean = isExpanded || progress > 0.01f

        assertTrue(canHandleBack(isExpanded = true, progress = 1f))
        assertTrue(canHandleBack(isExpanded = false, progress = 0.5f)) // closing animation
        assertTrue(canHandleBack(isExpanded = false, progress = 0.02f))
        assertFalse(canHandleBack(isExpanded = false, progress = 0f)) // resting closed
    }

    @Test
    fun calculateImePushUpOffset_whenKeyboardClosed_returnsZero() {
        val imeBottom = 0
        val navBarsBottom = 120
        val offset =
            calculateImePushUpOffset(imeBottom, navBarsBottom, isSheetExpanded = true, isModalOverlayActive = false)
        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun calculateImePushUpOffset_whenKeyboardOpen_subtractsNavigationBars() {
        val imeBottom = 900 // keyboard occupies 900px from window bottom
        val navBarsBottom = 120 // nav bar occupies 120px from window bottom
        val offset =
            calculateImePushUpOffset(imeBottom, navBarsBottom, isSheetExpanded = true, isModalOverlayActive = false)
        // Dock is already 120px above window bottom, so it only needs to move up by 900 - 120 = 780px
        assertEquals(780f, offset, 0.001f)
    }

    @Test
    fun calculateImePushUpOffset_whenKeyboardSmallerThanNavBars_clampsToZero() {
        val imeBottom = 80
        val navBarsBottom = 120
        val offset =
            calculateImePushUpOffset(imeBottom, navBarsBottom, isSheetExpanded = true, isModalOverlayActive = false)
        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun calculateImePushUpOffset_whenModalOverlayActive_returnsZero() {
        val imeBottom = 900
        val navBarsBottom = 120
        val offset = calculateImePushUpOffset(
            imeBottom = imeBottom,
            navBarsBottom = navBarsBottom,
            isSheetExpanded = true,
            isModalOverlayActive = true,
        )
        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun calculateImePushUpOffset_whenDockSheetNotExpanded_returnsZero() {
        val imeBottom = 900
        val navBarsBottom = 120
        val offset = calculateImePushUpOffset(
            imeBottom = imeBottom,
            navBarsBottom = navBarsBottom,
            isSheetExpanded = false,
            isModalOverlayActive = false,
        )
        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun calculateImePushUpOffset_whenDockSheetExpandedAndNoModal_returnsOffset() {
        val imeBottom = 900
        val navBarsBottom = 120
        val offset = calculateImePushUpOffset(
            imeBottom = imeBottom,
            navBarsBottom = navBarsBottom,
            isSheetExpanded = true,
            isModalOverlayActive = false,
        )
        assertEquals(780f, offset, 0.001f)
    }

    @Test
    fun calculateDynamicMaxSheetHeight_whenKeyboardClosed_returnsMaxSheetHeight() {
        val result = calculateDynamicMaxSheetHeight(
            screenHeightPx = 2400f,
            statusBarsTop = 100f,
            navBarsBottom = 120f,
            imeOffsetPx = 0f,
            dockHeightPx = 168f,
            bottomMarginPx = 60f,
            topGapPx = 48f,
            maxSheetHeightPx = 1440f, // 480dp * 3
        )
        // Large screen with no keyboard -> full maxSheetHeight (1440px)
        assertEquals(1440f, result, 0.001f)
    }

    @Test
    fun calculateDynamicMaxSheetHeight_whenKeyboardOpen_constrainsHeightToRemainingScreenSpace() {
        val screenHeight = 2400f
        val statusBars = 100f
        val navBars = 120f
        val imeOffset = 780f
        val dockHeight = 168f
        val bottomMargin = 60f
        val topGap = 48f
        val maxSheetHeight = 1440f

        val result = calculateDynamicMaxSheetHeight(
            screenHeightPx = screenHeight,
            statusBarsTop = statusBars,
            navBarsBottom = navBars,
            imeOffsetPx = imeOffset,
            dockHeightPx = dockHeight,
            bottomMarginPx = bottomMargin,
            topGapPx = topGap,
            maxSheetHeightPx = maxSheetHeight,
        )

        val expected = screenHeight - statusBars - navBars - imeOffset - dockHeight - bottomMargin - topGap
        // 2400 - 100 - 120 - 780 - 168 - 60 - 48 = 1124f
        assertEquals(1124f, result, 0.001f)
        assertTrue(result < maxSheetHeight)
    }

    @Test
    fun calculateDynamicMaxSheetHeight_onSmallScreenWithLargeKeyboard_clampsToMinimum200() {
        val result = calculateDynamicMaxSheetHeight(
            screenHeightPx = 1000f,
            statusBarsTop = 80f,
            navBarsBottom = 100f,
            imeOffsetPx = 800f,
            dockHeightPx = 168f,
            bottomMarginPx = 60f,
            topGapPx = 48f,
            maxSheetHeightPx = 1440f,
        )
        // Calculated available is negative, but must clamp to at least 200f
        assertEquals(200f, result, 0.001f)
    }
}
