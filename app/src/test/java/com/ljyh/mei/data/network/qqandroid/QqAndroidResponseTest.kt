package com.ljyh.mei.data.network.qqandroid

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QqAndroidResponseTest {
    @Test
    fun moduleFailureIsNotTreatedAsAnEmptySearch() {
        val root = Json.parseToJsonElement(
            """{"code":0,"${QqAndroidParser.KEY_SEARCH}":{"code":2000}}"""
        ).jsonObject

        try {
            requireSuccessfulQqResponse(root, QqAndroidParser.MOD_SEARCH, QqAndroidParser.MET_SEARCH)
            fail("Expected module error")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("code=2000"))
        }
    }

    @Test
    fun guestSearchResultKeepsTheLyricIdAndAlbumArtwork() {
        val root = Json.parseToJsonElement(
            """{"code":0,"${QqAndroidParser.KEY_SEARCH}":{"code":0,"data":{"body":{"item_song":{"items":[{"id":97773,"mid":"song-mid","name":"晴天","singer":[{"name":"周杰伦"}],"album":{"title":"叶惠美","id":42,"pmid":"album-mid"},"interval":269}]}}}}}"""
        ).jsonObject

        requireSuccessfulQqResponse(root, QqAndroidParser.MOD_SEARCH, QqAndroidParser.MET_SEARCH)
        val track = QqAndroidParser.parseSearch(root).single().toMusicTrack()
        assertEquals("97773", track.id.value)
        assertEquals("qq", track.id.source)
        assertEquals("周杰伦", track.artists.single().name)
        assertEquals("song-mid", track.playbackId)
        assertEquals("https://y.qq.com/music/photo_new/T002R300x300M000album-mid.jpg", track.album?.coverUrl)
        assertEquals(269_000L, track.durationMs)
    }

    @Test
    fun emptySearchIsDistinctFromMissingResults() {
        val empty = Json.parseToJsonElement(
            """{"${QqAndroidParser.KEY_SEARCH}":{"data":{"body":{"item_song":{"items":[]}}}}}"""
        ).jsonObject
        assertTrue(QqAndroidParser.parseSearch(empty).isEmpty())

        val malformed = Json.parseToJsonElement(
            """{"${QqAndroidParser.KEY_SEARCH}":{"data":{"body":{}}}}"""
        ).jsonObject
        try {
            QqAndroidParser.parseSearch(malformed)
            fail("Expected missing results error")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("item_song.items"))
        }
    }
}
