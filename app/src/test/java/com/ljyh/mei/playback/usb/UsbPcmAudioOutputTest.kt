package com.ljyh.mei.playback.usb

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class UsbPcmAudioOutputTest {
    private class Transport : UsbPcmTransport {
        var queued = 0L
        var completed = 0L
        var limit = Int.MAX_VALUE
        var capacity = 600L
        var starts = 0
        var pauses = 0
        var closes = 0
        var startSucceeds = true
        var error: String? = null
        override fun write(buffer: ByteBuffer): Int {
            val written = minOf(buffer.remaining(), limit)
            if (written > 0) queued += written / 4
            return written
        }
        override fun queuedFrames() = queued
        override fun completedFrames() = completed
        override fun capacityFrames() = capacity
        override fun play(): Boolean { starts++; return startSucceeds }
        override fun pause(): Boolean { pauses++; return true }
        override fun flush(): Boolean { queued = 0; completed = 0; return true }
        override fun drain() = true
        override fun failure() = error
        override fun close() { closes++ }
    }
    private class Fixture {
        val transport = Transport()
        var time = 0L
        var active = false
        val failures = mutableListOf<String>()
        var releases = 0
        val output = UsbPcmAudioOutput(transport, 1000, 4, 0, { time }, { active = it }, { failures += it }, { releases++ })
        fun write(frames: Int): ByteBuffer = ByteBuffer.allocateDirect(frames * 4).also { output.write(it, 1, 0) }
    }

    @Test fun lowLatencyPrerollCanStartWithoutWaitingFor300Ms() {
        val t = Transport()
        val output = UsbPcmAudioOutput(t, 1000, 4, 0, { 0 }, {}, {}, {}, prerollMs = { 50 })
        output.play()
        output.write(ByteBuffer.allocateDirect(49 * 4), 1, 0)
        assertEquals(0, t.starts)
        output.write(ByteBuffer.allocateDirect(4), 1, 0)
        assertEquals(1, t.starts)
    }

    @Test fun resampledClockUsesOutputRateAndStillReportsInputBufferFrames() {
        val t = Transport()
        val output = UsbPcmAudioOutput(t, 1000, 4, 0, { 0 }, {}, {}, {}, outputRate = 2000)
        output.write(ByteBuffer.allocateDirect(1000 * 4), 1, 0)
        t.completed = 1500
        assertEquals(750000L, output.positionUs)
        assertEquals(300L, output.bufferSizeInFrames)
        t.completed = 3000
        assertEquals(1000000L, output.positionUs)
    }

    @Test fun playWaitsForRealPcmPrerollBeforeStartingTransport() {
        val f = Fixture()
        f.output.play()
        assertEquals(0, f.transport.starts)
        f.write(299)
        assertEquals(0, f.transport.starts)
        f.write(1)
        assertEquals(1, f.transport.starts)
        assertFalse(f.active)
        f.transport.completed = 10
        assertEquals(10_000L, f.output.positionUs)
        assertTrue(f.active)
    }

    @Test fun smallerBufferCannotDeadlockWaitingForImpossiblePreroll() {
        val f = Fixture()
        f.transport.capacity = 100
        f.output.play()
        f.write(75)
        assertEquals(1, f.transport.starts)
    }

    @Test fun positionDoesNotAdvanceMerelyBecauseTimePassed() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.time = 1_000
        assertEquals(0, f.output.positionUs)
        assertFalse(f.active)
    }

    @Test fun shortTrackStartsWhenEndOfStreamArrives() {
        val f = Fixture()
        f.output.play()
        f.write(50)
        assertEquals(0, f.transport.starts)
        f.output.stop()
        assertEquals(1, f.transport.starts)
        assertEquals(50, f.transport.queued)
        assertEquals(0, f.transport.closes)
    }

    @Test fun partialWriteAdvancesOnlyBytesActuallyAccepted() {
        val f = Fixture()
        f.transport.limit = 8
        val buffer = ByteBuffer.allocateDirect(24)
        buffer.position(4)
        assertFalse(f.output.write(buffer, 1, 0))
        assertEquals(12, buffer.position())
        assertFalse(f.output.write(buffer, 1, 0))
        assertEquals(20, buffer.position())
        assertTrue(f.output.write(buffer, 1, 0))
        assertEquals(24, buffer.position())
    }

    @Test fun pauseKeepsQueuedPcmAndResumeDoesNotWaitForFullPreroll() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.output.pause()
        f.transport.queued = 20
        f.output.play()
        assertEquals(2, f.transport.starts)
        assertEquals(1, f.transport.pauses)
        assertEquals(20, f.transport.queued)
    }

    @Test fun preparingWhilePausedNeverStartsTransport() {
        val f = Fixture()
        f.write(400)
        f.output.stop()
        assertEquals(0, f.transport.starts)
        f.output.play()
        assertEquals(1, f.transport.starts)
    }

    @Test fun transferWithoutCompletionsFailsOnceInsteadOfHangingForever() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.time = 3_001
        f.output.positionUs
        f.output.positionUs
        assertEquals(1, f.failures.size)
        assertFalse(f.active)
        assertFalse(f.output.write(ByteBuffer.allocateDirect(4), 1, 0))
    }

    @Test fun longPauseDoesNotCountAsTransportStall() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.output.pause()
        f.time = 30_000
        f.output.positionUs
        assertTrue(f.failures.isEmpty())
        f.output.play()
        f.output.positionUs
        assertTrue(f.failures.isEmpty())
    }

    @Test fun transportFailureIsPublishedWithoutPretendingPlaybackSucceeded() {
        val f = Fixture()
        f.transport.startSucceeds = false
        f.output.play()
        f.write(300)
        assertFalse(f.active)
        assertEquals(1, f.failures.size)
    }

    @Test fun releaseIsIdempotentAndStopsFurtherWrites() {
        val f = Fixture()
        f.output.release()
        f.output.release()
        assertEquals(1, f.transport.closes)
        assertEquals(1, f.releases)
        assertFalse(f.output.write(ByteBuffer.allocateDirect(4), 1, 0))
    }

    @Test fun flushRemovesOldClockAndPcmBeforeSeek() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.transport.completed = 80
        assertEquals(80_000L, f.output.positionUs)
        f.output.flush()
        assertEquals(0L, f.output.positionUs)
        assertEquals(0L, f.transport.queued)
        assertFalse(f.active)
        f.write(300)
        assertEquals(2, f.transport.starts)
    }

    @Test fun activeCompletionsResetStallDeadline() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.time = 2_900
        f.transport.completed = 100
        f.output.positionUs
        f.time = 5_800
        f.output.positionUs
        assertTrue(f.failures.isEmpty())
    }

    @Test fun nextTrackReusingOutputCanBecomeActiveAfterPreviousTrackDrained() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.transport.completed = 300
        f.transport.queued = 0
        f.output.stop()
        f.output.positionUs
        assertFalse(f.active)
        f.write(100)
        f.transport.completed = 350
        f.output.positionUs
        assertTrue(f.active)
        assertEquals(350_000L, f.output.positionUs)
    }

    @Test fun transportAcceptingNoPcmFailsEvenBeforeTheFirstTransferStarts() {
        val f = Fixture()
        f.transport.limit = 0
        f.output.play()
        f.write(300)
        f.time = 3_001
        f.output.positionUs
        assertEquals(0, f.transport.starts)
        assertEquals(1, f.failures.size)
    }

    @Test fun waitingForUpstreamDataIsNotReportedAsAUsbFailure() {
        val f = Fixture()
        f.output.play()
        f.time = 5_000
        f.output.positionUs
        assertTrue(f.failures.isEmpty())
    }

    @Test fun repeatedPlayDoesNotHideAStalledTransport() {
        val f = Fixture()
        f.output.play()
        f.write(300)
        f.time = 2_000
        f.output.play()
        f.time = 3_001
        f.output.positionUs
        assertEquals(1, f.failures.size)
    }

    @Test fun unalignedNativeWriteIsRejectedWithoutMovingInputPosition() {
        val f = Fixture()
        f.transport.limit = 3
        val buffer = ByteBuffer.allocateDirect(8)
        assertFalse(f.output.write(buffer, 1, 0))
        assertEquals(0, buffer.position())
        assertEquals(1, f.failures.size)
    }
}
