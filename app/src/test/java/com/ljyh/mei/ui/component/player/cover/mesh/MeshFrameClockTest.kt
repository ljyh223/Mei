package com.ljyh.mei.ui.component.player.cover.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshFrameClockTest {
    @Test fun staticArtworkSleepsButAlbumCrossfadeCanFinishWithoutMovingTheBackground() {
        val clock = MeshFrameClock()
        clock.timeSeconds(0L)
        assertEquals(1f, clock.timeSeconds(1_000_000_000L), 0f)
        clock.setStaticMode(true)
        assertFalse(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))

        // A new album wakes the host explicitly; only its crossfade requests subsequent frames.
        assertTrue(clock.needsAnotherFrame(hasArtwork = true, transitioning = true))
        assertEquals(1f, clock.timeSeconds(60_000_000_000L), 0f)
        assertEquals(1f, clock.timeSeconds(61_000_000_000L), 0f)
        assertFalse(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
    }

    @Test fun disablingStaticModeDoesNotAddTheIdleTimeToTheNextFrame() {
        val clock = MeshFrameClock()
        clock.timeSeconds(0L)
        clock.timeSeconds(1_000_000_000L)
        clock.setStaticMode(true)
        clock.setStaticMode(false)
        assertTrue(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
        assertEquals(1f, clock.timeSeconds(120_000_000_000L), 0f)
        assertEquals(2f, clock.timeSeconds(121_000_000_000L), 0f)
    }

    @Test fun pausedPlaybackAllowsAlbumTransitionsAndResumesWithoutAnAnimationJump() {
        val clock = MeshFrameClock()
        clock.timeSeconds(0L)
        clock.timeSeconds(1_000_000_000L)
        clock.setPlaying(false)
        assertFalse(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
        assertTrue(clock.needsAnotherFrame(hasArtwork = true, transitioning = true))
        assertEquals(1f, clock.timeSeconds(10_000_000_000L), 0f)
        clock.setPlaying(true)
        assertEquals(1f, clock.timeSeconds(20_000_000_000L), 0f)
        assertEquals(2f, clock.timeSeconds(21_000_000_000L), 0f)
    }

    @Test fun surfaceRecreationOrVisibilityResumePreservesTimeButDiscardsHiddenDuration() {
        val clock = MeshFrameClock()
        clock.timeSeconds(0L)
        clock.timeSeconds(1_000_000_000L)
        clock.resetFrameTime()
        assertEquals(1f, clock.timeSeconds(60_000_000_000L), 0f)
        assertEquals(2f, clock.timeSeconds(61_000_000_000L), 0f)
        clock.setStaticMode(true)
        clock.resetFrameTime()
        assertEquals(2f, clock.timeSeconds(120_000_000_000L), 0f)
        assertFalse(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
    }

    @Test fun repeatedSettingsDoNotResetTheRunningClockAndNoArtworkNeverLoops() {
        val clock = MeshFrameClock()
        assertFalse(clock.needsAnotherFrame(hasArtwork = false, transitioning = false))
        assertFalse(clock.needsAnotherFrame(hasArtwork = false, transitioning = true))
        clock.timeSeconds(0L)
        clock.setStaticMode(false)
        clock.setPlaying(true)
        assertEquals(1f, clock.timeSeconds(1_000_000_000L), 0f)
        clock.setStaticMode(true)
        clock.setPlaying(false)
        clock.setStaticMode(false)
        assertFalse(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
        clock.setPlaying(true)
        assertTrue(clock.needsAnotherFrame(hasArtwork = true, transitioning = false))
    }
}
