package com.ljyh.mei.playback.queue

import com.ljyh.mei.data.model.domain.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueSnapshotTest {
    @Test
    fun roundTripKeepsQueueOrderCurrentSongAndPosition() {
        val current = MediaMetadata(
            id = 2,
            title = "Second",
            coverUrl = "https://example.com/cover.jpg",
            artists = listOf(MediaMetadata.Artist(9, "Artist")),
            duration = 180_000,
            album = MediaMetadata.Album(4, "Album"),
        )
        val snapshot = PlaybackQueueSnapshot(listOf("1", "2", "3"), 1, 43_000, current, isFmMode = true)

        assertEquals(snapshot, PlaybackQueueSnapshotCodec.decode(PlaybackQueueSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun oldSnapshotRestoresAsNormalQueue() {
        val oldSnapshot = """{"ids":["1"],"currentIndex":0,"positionMs":0}"""

        assertEquals(false, PlaybackQueueSnapshotCodec.decode(oldSnapshot)?.isFmMode)
    }

    @Test
    fun rejectsInvalidOrMismatchedSnapshots() {
        assertNull(PlaybackQueueSnapshotCodec.decode("{"))
        assertNull(PlaybackQueueSnapshotCodec.decode("""{"ids":["1"],"currentIndex":2,"positionMs":0}"""))
        assertNull(PlaybackQueueSnapshotCodec.decode("""{"ids":["1"],"currentIndex":0,"positionMs":-1}"""))
        val wrongSong = MediaMetadata(
            id = 2, title = "Wrong", coverUrl = "",
            artists = emptyList(), duration = 0,
            album = MediaMetadata.Album(0, ""),
        )
        assertNull(PlaybackQueueSnapshotCodec.decode(
            PlaybackQueueSnapshotCodec.encode(PlaybackQueueSnapshot(listOf("1"), 0, 0, wrongSong))
        ))
    }
}
