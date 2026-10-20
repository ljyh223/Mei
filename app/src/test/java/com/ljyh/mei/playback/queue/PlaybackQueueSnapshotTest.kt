package com.ljyh.mei.playback.queue

import com.ljyh.mei.data.model.domain.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    fun duplicateSongOccurrencesKeepDifferentSourcesAfterRestore() {
        val first = QueueSource(QueueSource.Type.PLAYLIST, "10", "收藏")
        val second = QueueSource(QueueSource.Type.ALBUM, "20", "专辑")
        val snapshot = PlaybackQueueSnapshot(
            ids = listOf("123", "123"),
            currentIndex = 1,
            positionMs = 0,
            currentSong = null,
            entries = listOf(
                QueueEntrySnapshot("entry-a", first),
                QueueEntrySnapshot("entry-b", second),
            ),
        )

        val restored = PlaybackQueueSnapshotCodec.decode(PlaybackQueueSnapshotCodec.encode(snapshot))
        assertEquals(snapshot, restored)
        assertEquals(first, restored?.entries?.get(0)?.source)
        assertEquals(second, restored?.entries?.get(1)?.source)
    }

    @Test
    fun oldSnapshotRestoresAsNormalQueue() {
        val oldSnapshot = """{"ids":["1"],"currentIndex":0,"positionMs":0}"""

        assertEquals(false, PlaybackQueueSnapshotCodec.decode(oldSnapshot)?.isFmMode)
        assertNull(PlaybackQueueSnapshotCodec.decode(oldSnapshot)?.entries)
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

    @Test
    fun restoresLegacyMetadataAndSourcesWithOmittedNullsAndUnknownFields() {
        val legacy = """
            {
              "ids": ["123", "123"],
              "currentIndex": 1,
              "positionMs": 43000,
              "currentSong": {
                "id": 123,
                "title": "\u003c主打\u003e\u0026Song",
                "coverUrl": "https://example.com/cover?a=1\u0026b=2",
                "artists": [{"id": 9, "name": "歌手", "futureArtistField": true}],
                "duration": 180000,
                "album": {"id": 4, "title": "专辑"},
                "futureSongField": "ignored"
              },
              "entries": [
                {"entryId": "entry-a", "source": {"type": "PLAYLIST", "id": "10", "title": "收藏"}},
                {"entryId": "entry-b"}
              ],
              "futureQueueField": 1
            }
        """.trimIndent()
        val expectedSong = MediaMetadata(
            id = 123,
            title = "<主打>&Song",
            coverUrl = "https://example.com/cover?a=1&b=2",
            artists = listOf(MediaMetadata.Artist(9, "歌手")),
            duration = 180_000,
            album = MediaMetadata.Album(4, "专辑"),
        )
        assertEquals(
            PlaybackQueueSnapshot(
                ids = listOf("123", "123"),
                currentIndex = 1,
                positionMs = 43_000,
                currentSong = expectedSong,
                entries = listOf(
                    QueueEntrySnapshot("entry-a", QueueSource(QueueSource.Type.PLAYLIST, "10", "收藏")),
                    QueueEntrySnapshot("entry-b", null),
                ),
            ),
            PlaybackQueueSnapshotCodec.decode(legacy),
        )
    }

    @Test
    fun readsExplicitNullOptionalFields() {
        val legacy = """{"ids":["123"],"currentIndex":0,"positionMs":0,"currentSong":null,"entries":[{"entryId":"entry-a","source":null}]}"""
        assertEquals(
            PlaybackQueueSnapshot(listOf("123"), entries = listOf(QueueEntrySnapshot("entry-a"))),
            PlaybackQueueSnapshotCodec.decode(legacy),
        )
    }

    @Test
    fun allSourceTypesAndOptionalMetadataRoundTrip() {
        val song = MediaMetadata(
            id = 1, title = "Song", coverUrl = "", duration = 0,
            artists = listOf(MediaMetadata.Artist(9, "Artist", picUrl = "cover", alias = listOf("Alias"))),
            album = MediaMetadata.Album(4, "Album"), explicit = true, tns = "翻译",
        )
        for (type in QueueSource.Type.entries) {
            val snapshot = PlaybackQueueSnapshot(
                listOf("1"), currentSong = song,
                entries = listOf(QueueEntrySnapshot("entry-a", QueueSource(type))),
            )
            assertEquals(type.name, snapshot, PlaybackQueueSnapshotCodec.decode(PlaybackQueueSnapshotCodec.encode(snapshot)))
        }
    }

    @Test
    fun writesDefaultsAndOmitsNullsWithoutChangingFieldNames() {
        val encoded = PlaybackQueueSnapshotCodec.encode(PlaybackQueueSnapshot(listOf("1")))
        val fields = Json.parseToJsonElement(encoded).jsonObject
        assertEquals("0", fields.getValue("currentIndex").jsonPrimitive.content)
        assertEquals("0", fields.getValue("positionMs").jsonPrimitive.content)
        assertEquals("false", fields.getValue("isFmMode").jsonPrimitive.content)
        assertFalse(fields.containsKey("currentSong"))
        assertFalse(fields.containsKey("entries"))
    }

    @Test
    fun rejectsCorruptNestedMetadataAndQueueEntries() {
        val invalid = listOf(
            "null",
            """{"ids":[null]}""",
            """{"ids":["1"],"currentSong":{"id":1}}""",
            """{"ids":["1"],"entries":[]}""",
            """{"ids":["1"],"entries":[null]}""",
            """{"ids":["1"],"entries":[{"entryId":""}]}""",
        )
        for (value in invalid) assertNull(value, PlaybackQueueSnapshotCodec.decode(value))
    }
}
