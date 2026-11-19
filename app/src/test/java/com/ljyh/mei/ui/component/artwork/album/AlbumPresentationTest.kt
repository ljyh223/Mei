package com.ljyh.mei.ui.component.artwork.album

import org.junit.Assert.*
import org.junit.Test

class AlbumPresentationTest {
    @Test fun vinylClearsTheSleeveBeforeTurningToTheTracklistAndClosingReversesThePath() {
        for (step in 0..1000) {
            val pose = albumPresentation(AlbumMediaStyle.Vinyl, step / 1000f)
            if (pose.yawOffset > .01f) assertTrue("Sleeve turned before record cleared at $step", pose.opening > .99f)
        }
        val extracted = albumPresentation(AlbumMediaStyle.Vinyl, 1f)
        assertEquals(1f, extracted.opening, 0f)
        assertEquals(180f, extracted.yawOffset, .001f)
        assertEquals(albumPresentation(AlbumMediaStyle.Vinyl, 0f), albumPresentation(AlbumMediaStyle.Vinyl, -1f))
        assertEquals(extracted, albumPresentation(AlbumMediaStyle.Vinyl, 2f))
    }

    @Test fun openingAndInterruptedReverseSequencesKeepAllMechanismsBoundedAndContinuous() {
        for (format in listOf(AlbumMediaStyle.CD, AlbumMediaStyle.Cassette, AlbumMediaStyle.Vinyl)) {
            val progress = (0..730).map { it / 1000f } + (730 downTo 0).map { it / 1000f }
            val poses = progress.map { albumPresentation(format, it) }
            for (pose in poses) {
                assertTrue(pose.opening in 0f..155f)
                assertTrue(pose.pitch in -8f..0f)
            }
            for ((first, next) in poses.zipWithNext()) {
                assertTrue(kotlin.math.abs(first.opening - next.opening) < 1f)
                assertTrue(kotlin.math.abs(first.yawOffset - next.yawOffset) < 1.5f)
            }
            assertEquals(poses.first(), poses.last())
        }
    }

    @Test fun everyRealTrackAppearsOnceWithItsOriginalNumberAcrossLinerPages() {
        val tracks = (1..25).map { AlbumPackageTrack(it, if (it % 2 == 0) "同名曲目" else "曲目 $it", 190_000L + it) }
        val pages = albumLinerPages(tracks)
        assertEquals(4, pages.size)
        assertEquals(tracks, pages.flatten())
        assertTrue(pages.all { it.size <= 8 })
        assertEquals(listOf(25), pages.last().map { it.number })
        assertEquals(listOf(emptyList<AlbumPackageTrack>()), albumLinerPages(emptyList()))
    }

    @Test fun missingDurationsDoNotPrintZerosAndRealDurationsUseMinutesAndSeconds() {
        assertEquals("3:10", AlbumPackageTrack(1, "Song", 190_999).durationLabel)
        assertEquals("60:01", AlbumPackageTrack(1, "Long", 3_601_000).durationLabel)
        assertEquals("", AlbumPackageTrack(1, "Unknown", 0).durationLabel)
        assertEquals("", AlbumPackageTrack(1, "Unknown", -1).durationLabel)
    }

    @Test fun reflectionFadePreservesBothNearAndFarContactEdgesInsideTheStage() {
        for (height in listOf(120f, 320f, 800f)) {
            val geometry = albumReflectionGeometry(height, height * .84f, height * .70f, height * 1.8f)!!
            assertEquals(height * .70f, geometry.top, .001f)
            assertTrue(geometry.top + geometry.height > height * .84f)
            assertTrue(geometry.top + geometry.height <= height)
        }
        assertNull(albumReflectionGeometry(0f, 0f, 0f, 0f))
        assertNull(albumReflectionGeometry(120f, 120f, 130f, 250f))
        assertNull(albumReflectionGeometry(120f, 100f, Float.NaN, 200f))
    }
    @Test fun vinylHasAnExtractionPauseThenAnUnhurriedTurnToTheBack() {
        fun at(milliseconds: Int) = albumPresentation(AlbumMediaStyle.Vinyl,
            milliseconds.toFloat() / VinylPresentationDurationMillis)
        assertEquals(1f, at(650).opening, 0f)
        assertEquals(0f, at(700).yawOffset, 0f)
        assertTrue(at(1100).yawOffset in 0f..30f)
        assertTrue(at(1600).yawOffset in 80f..105f)
        assertTrue(at(2100).yawOffset in 160f..180f)
        assertEquals(180f, at(2400).yawOffset, .001f)
    }

    @Test fun widerLinersUseFewerLargerRowsWithoutDroppingAnyTracks() {
        val tracks = (1..17).map { AlbumPackageTrack(it, "Track $it", 180_000) }
        for (format in listOf(AlbumMediaStyle.CD, AlbumMediaStyle.Vinyl)) {
            val pages = albumLinerPages(tracks, format)
            assertEquals(listOf(6, 6, 5), pages.map { it.size })
            assertEquals(tracks, pages.flatten())
        }
        assertEquals(listOf(8, 8, 1), albumLinerPages(tracks, AlbumMediaStyle.Cassette).map { it.size })
        val cd = albumPresentation(AlbumMediaStyle.CD, 1f)
        assertEquals(135f, cd.opening, .001f)
        val readingYaw = -16f + cd.yawOffset
        val linerFacing = -kotlin.math.cos(Math.toRadians((cd.opening - readingYaw).toDouble()))
        assertTrue("Liner should face the reader rather than turn edge-on", linerFacing > .95)
    }

    @Test fun openIntentAlwaysOffersReturnEvenWhenTheAnimationWasPausedEarly() {
        assertEquals(0f, albumNextOpeningTarget(AlbumMediaStyle.Vinyl, 1f), 0f)
        assertEquals(1f, albumNextOpeningTarget(AlbumMediaStyle.Vinyl, 0f), 0f)
        assertEquals(0f, albumNextOpeningTarget(AlbumMediaStyle.Vinyl, .29f), 0f)
        var tape = 0f
        tape = albumNextOpeningTarget(AlbumMediaStyle.Cassette, tape)
        assertEquals(CassettePackageOpenProgress, tape, 0f)
        tape = albumNextOpeningTarget(AlbumMediaStyle.Cassette, tape)
        assertEquals(1f, tape, 0f)
        assertEquals(0f, albumNextOpeningTarget(AlbumMediaStyle.Cassette, tape), 0f)
    }

    @Test fun modelWhitespaceSharesTheFadeWithoutLosingToolbarProtection() {
        for (height in listOf(120f, 240f, 316f, 460f)) {
            val minimumModelInset = height * (AlbumModelCenterY - AlbumModelHalfHeight)
            val clearance = albumModelTopClearanceDp(34f, height, AlbumMediaStyle.Cassette)
            assertTrue(clearance >= 0f && clearance < 34f)
            assertTrue(clearance + minimumModelInset >= 35f - .001f)
        }
        assertEquals(34f, albumModelTopClearanceDp(34f, 316f, AlbumMediaStyle.Plain), 0f)
        assertEquals(0f, albumModelTopClearanceDp(0f, 316f, AlbumMediaStyle.Vinyl), 0f)
    }

}
