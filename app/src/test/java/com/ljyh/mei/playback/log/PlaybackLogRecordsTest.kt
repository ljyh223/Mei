package com.ljyh.mei.playback.log

import com.ljyh.mei.playback.queue.QueueSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class PlaybackLogRecordsTest {
    @Test fun uaAndBiKeepDistinctTimesUnitsAndLineBoundaries() {
        val event = PlaybackLogEvent(
            PlaybackLogTrack(123, "entry", "Song", "Artist", 180000, "album", "88"),
            "owner", "application-session", 1700000000123, 1700000009123,
            realTimeMs = 4500, playedTimeMs = 9000, end = "ui",
        )
        val (ua, bi) = event.records(42, 1700000010000)
        assertTrue(ua.endsWith("\n"))
        assertTrue(bi.endsWith("\n"))
        val parts = ua.trimEnd('\n').split('\u0001')
        assertEquals(listOf("1700000010", "_pld"), parts.take(2))
        val fields = Json.parseToJsonElement(parts[2]).jsonObject
        fun value(key: String) = fields.getValue(key).jsonPrimitive.content
        assertEquals("1700000000", value("startlogtime"))
        assertEquals("1700000009123", value("logtime"))
        assertEquals("1700000010000", value("_logTime"))
        assertEquals("4", value("realtime"))
        assertEquals("9", value("time"))
        assertEquals("application-session", value("_sessid"))
        assertEquals("ui", value("end"))
        assertEquals("123", value("id"))
        assertEquals("play", bi.split('\u0001')[1])
        assertFalse(Json.parseToJsonElement(bi.split('\u0001')[2]).jsonObject.containsKey("_sessid"))
    }

    @Test fun collectionSourcesKeepTheirRealIdentifiers() {
        assertEquals("list" to "44", playbackLogSource(QueueSource(QueueSource.Type.PLAYLIST, "44"), 12))
        assertEquals("album" to "55", playbackLogSource(QueueSource(QueueSource.Type.ALBUM, "55"), 12))
        assertEquals("artist" to "66", playbackLogSource(QueueSource(QueueSource.Type.ARTIST, "66"), 12))
        assertEquals("track" to "12", playbackLogSource(QueueSource(QueueSource.Type.PLAYLIST, "unknown"), 12))
        assertEquals("track" to "12", playbackLogSource(null, 12))
    }
}
