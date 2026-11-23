package com.ljyh.mei.playback.log

import org.junit.Assert.*
import org.junit.Test

class PlaybackLogSessionTest {
    private val track = PlaybackLogTrack(12, "first", "Song", "Artist", 180000, "list", "7")
    private val session = PlaybackLogSession()

    private fun update(time: Long, playing: Boolean = true, speed: Float = 1f,
                       owner: String? = "owner", song: PlaybackLogTrack? = track, wall: Long = 1700000000000 + time) =
        session.update(song, owner, "app-session", playing, speed, time, wall)

    @Test fun bufferingBeforeFirstAudioDoesNotCreateAPhantomPlayback() {
        assertTrue(update(0, playing = false).isEmpty())
        assertNull(session.finish(1000, 1000))
        assertEquals(1, update(1000).size)
    }

    @Test fun pausesAndBufferingDoNotCountAndResumeDoesNotStartAgain() {
        assertEquals(1, update(0).size)
        assertTrue(update(2500, playing = false).isEmpty())
        assertTrue(update(9000, playing = false).isEmpty())
        assertTrue(update(10000).isEmpty())
        val end = session.finish(11500, 1700000011500, "playend")!!
        assertEquals(4000, end.realTimeMs)
        assertEquals(4000, end.playedTimeMs)
        assertEquals("playend", end.end)
        assertNull(session.finish(20000, 1700000020000))
    }

    @Test fun speedChangesAccruePreviousSegmentAtItsOwnRate() {
        update(0)
        update(1000, speed = 2f)
        update(3000, speed = 0.5f)
        val end = session.finish(5000, 1700000005000)!!
        assertEquals(5000, end.realTimeMs)
        assertEquals(6000, end.playedTimeMs)
    }

    @Test fun clockCorrectionAndPositionEventsCannotInflateTime() {
        update(0)
        update(2000, wall = 100)
        update(2500, wall = 9999999999999)
        assertEquals(3000, session.finish(3000, 20)!!.realTimeMs)
    }

    @Test fun repeatedSongAndDuplicateQueueEntriesAreIndependent() {
        update(0)
        assertEquals(1000, session.finish(1000, 1700000001000, "playend")!!.realTimeMs)
        assertEquals(1, update(1000).size)
        val next = track.copy(entryId = "second", source = "album", sourceId = "88")
        val events = update(2000, song = next)
        assertEquals(2, events.size)
        assertEquals("list", events[0].track.source)
        assertEquals(1000, events[0].realTimeMs)
        assertEquals("album", events[1].track.source)
        assertEquals(1000, session.finish(3000, 1700000003000)!!.realTimeMs)
    }

    @Test fun accountChangeNeverRelabelsOldListeningTime() {
        update(0)
        val events = update(3000, owner = "new-owner")
        assertEquals("owner", events[0].owner)
        assertEquals(3000, events[0].realTimeMs)
        assertEquals("new-owner", events[1].owner)
        assertEquals(0, events[1].realTimeMs)
    }

    @Test fun unauthenticatedAndExternalTracksDoNotStartSessions() {
        assertTrue(update(0, owner = null).isEmpty())
        assertTrue(update(1000, song = null).isEmpty())
        assertNull(session.finish(3000, 1700000003000))
    }
}
