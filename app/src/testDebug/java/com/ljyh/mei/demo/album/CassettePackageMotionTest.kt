package com.ljyh.mei.demo.album

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CassettePackageMotionTest {
    @Test fun closedPackageContainsAnUprightTapeAndOpenStopKeepsItInTheTray() {
        val closed = cassettePackagePose(0f)
        val open = cassettePackagePose(CassettePackageOpenProgress)
        assertEquals(0f, closed.lidAngle, .001f)
        assertEquals(145f, open.lidAngle, .001f)
        assertEquals(closed.tapeX, open.tapeX, 0f)
        assertEquals(closed.tapeZ, open.tapeZ, 0f)
        assertEquals(90f, open.tapeAngle, .001f)
    }

    @Test fun tapeOnlyTurnsOrSlidesAfterItHasClearedTheCaseRim() {
        for (step in 0..1000) {
            val pose = cassettePackagePose(step / 1000f)
            if (pose.tapeX > .001f || pose.tapeAngle < 89.99f) {
                val tapeBack = pose.tapeZ - .055f * CassetteBodyScale
                assertTrue("Tape hit the rim at step $step", tapeBack > CassetteCaseRimZ + .02f)
                assertEquals(145f, pose.lidAngle, .001f)
            }
        }
    }

    @Test fun closingNeverReachesTheTapeUntilItHasReturnedUprightToTheTray() {
        val stored = cassettePackagePose(0f)
        for (step in 1000 downTo 0) {
            val pose = cassettePackagePose(step / 1000f)
            if (pose.lidAngle < 144.99f) {
                assertEquals(stored.tapeX, pose.tapeX, .001f)
                assertEquals(stored.tapeZ, pose.tapeZ, .001f)
                assertEquals(90f, pose.tapeAngle, .001f)
            }
        }
    }

    @Test fun fullyRemovedTapeClearsThePackageAndOutOfRangeInputClampsToEndpoints() {
        val removed = cassettePackagePose(1f)
        val tapeLeft = removed.tapeX - .79f * CassetteBodyScale
        assertTrue(tapeLeft > .55f)
        assertEquals(0f, removed.tapeAngle, .001f)
        assertEquals(cassettePackagePose(0f), cassettePackagePose(-1f))
        assertEquals(removed, cassettePackagePose(2f))
    }
}
