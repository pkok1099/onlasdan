package com.onlasdan.comic.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NoirBottomSheetPhysicsTest {

    @Test
    fun calculateScrimAlpha_whenProgressIsOne_returnsMaxAlpha() {
        val alphaDefault = calculateScrimAlpha(1.0f)
        assertEquals(0.6f, alphaDefault, 0.0001f)

        val alphaCustom = calculateScrimAlpha(1.0f, maxAlpha = 0.8f)
        assertEquals(0.8f, alphaCustom, 0.0001f)
    }

    @Test
    fun calculateScrimAlpha_whenProgressAtCutoff_returnsZero() {
        val alpha = calculateScrimAlpha(0.12f)
        assertEquals(0.0f, alpha, 0.0001f)

        val alphaCustomCutoff = calculateScrimAlpha(0.20f, cutoffThreshold = 0.20f)
        assertEquals(0.0f, alphaCustomCutoff, 0.0001f)
    }

    @Test
    fun calculateScrimAlpha_whenProgressBelowCutoff_returnsZero() {
        assertEquals(0.0f, calculateScrimAlpha(0.10f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(0.05f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(0.0f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(0.1199f), 0.0001f)
    }

    @Test
    fun calculateScrimAlpha_whenProgressHalfway_scalesProportionally() {
        // Halfway between cutoff (0.12f) and 1.0f:
        // 0.12f + (1.0f - 0.12f) * 0.5f = 0.56f
        // Expected alpha: 0.5f * 0.6f = 0.30f
        val halfwayProgress = 0.56f
        val alpha = calculateScrimAlpha(halfwayProgress)
        assertEquals(0.30f, alpha, 0.0001f)

        // Quarter-way: 0.12f + 0.88f * 0.25f = 0.34f -> 0.25f * 0.6f = 0.15f
        assertEquals(0.15f, calculateScrimAlpha(0.34f), 0.0001f)

        // Three-quarter-way: 0.12f + 0.88f * 0.75f = 0.78f -> 0.75f * 0.6f = 0.45f
        assertEquals(0.45f, calculateScrimAlpha(0.78f), 0.0001f)
    }

    @Test
    fun calculateScrimAlpha_whenProgressExceedsOne_clampsToMaxAlpha() {
        assertEquals(0.6f, calculateScrimAlpha(1.05f), 0.0001f)
        assertEquals(0.6f, calculateScrimAlpha(1.2f), 0.0001f)
        assertEquals(0.6f, calculateScrimAlpha(2.0f), 0.0001f)

        assertEquals(0.75f, calculateScrimAlpha(1.5f, maxAlpha = 0.75f), 0.0001f)
    }

    @Test
    fun calculateScrimAlpha_whenProgressNegative_returnsZero() {
        assertEquals(0.0f, calculateScrimAlpha(-0.01f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(-0.5f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(-1.0f), 0.0001f)
        assertEquals(0.0f, calculateScrimAlpha(-100.0f), 0.0001f)
    }
}
