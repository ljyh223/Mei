package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.GetIntelligence
import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.network.Resource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class IntelligencePlaybackTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun requestUsesSongIdRatherThanSongDetailField() {
        val body = json.parseToJsonElement(json.encodeToString(GetIntelligence(
            songId = "101", playlistId = "202", startMusicId = "101",
        ))).jsonObject
        assertEquals("101", body.getValue("songId").jsonPrimitive.content)
        assertEquals("202", body.getValue("playlistId").jsonPrimitive.content)
        assertEquals("101", body.getValue("startMusicId").jsonPrimitive.content)
        assertEquals("fromPlayOne", body.getValue("type").jsonPrimitive.content)
        assertFalse(body.containsKey("c"))
    }

    @Test
    fun ordinaryTracksCanOmitCloudQualityAndMessageFields() {
        val response = json.decodeFromString<Intelligence>("""
            {"code":200,"data":[{"id":3000000001,"songInfo":{
              "id":3000000001,"name":"Song","dt":200000,
              "al":{"id":4000000001,"name":"Album","picUrl":"https://example.com/cover.jpg"},
              "ar":[{"id":5000000001,"name":"Artist"}],
              "pc":null,"h":null,"m":null,"l":null,"a":null,"privilege":null
            }}]}
        """.trimIndent())
        val result = intelligencePlaybackTracks(response, null) as Resource.Success
        val song = result.data.single()
        assertEquals(3000000001L, song.id)
        assertEquals(4000000001L, song.album.id)
        assertEquals(5000000001L, song.artists.single().id)
        assertEquals(200000L, song.duration)
        assertEquals("https://example.com/cover.jpg", song.coverUrl)
    }

    @Test
    fun seedRemainsFirstAndDuplicateRecommendationsAreRemoved() {
        val response = response("""
            {"songInfo":{"id":1,"name":"Seed"}},
            {"songInfo":{"id":2,"name":"Recommendation"}},
            {"songInfo":{"id":2,"name":"Repeated"}}
        """.trimIndent())
        val seed = seed()
        val result = intelligencePlaybackTracks(response, seed) as Resource.Success
        assertEquals(listOf(1L, 2L), result.data.map { it.id })
        assertEquals(seed, result.data.first())
    }

    @Test
    fun recommendationsStillPlayWhenSeedMetadataIsUnavailable() {
        val result = intelligencePlaybackTracks(response("""{"songInfo":{"id":2,"name":"Song"}}"""), null)
            as Resource.Success
        assertEquals(listOf(2L), result.data.map { it.id })
    }

    @Test
    fun missingAndInvalidSongInfoAreSkipped() {
        val result = intelligencePlaybackTracks(response("""
            {"id":3,"songInfo":null},{"id":4},
            {"songInfo":{"id":0}},{"songInfo":{"id":2,"name":"Song"}}
        """.trimIndent()), null) as Resource.Success
        assertEquals(listOf(2L), result.data.map { it.id })
    }

    @Test
    fun emptyRecommendationsDoNotReplacePlaybackWithOnlyTheSeed() {
        assertTrue(intelligencePlaybackTracks(response(""), seed()) is Resource.Error)
        assertTrue(intelligencePlaybackTracks(response("""{"songInfo":null}"""), seed()) is Resource.Error)
    }

    @Test
    fun serverFailureUsesMessageInsteadOfAnEmptyQueue() {
        val response = json.decodeFromString<Intelligence>("""{"code":301,"message":"请先登录"}""")
        assertEquals(Resource.Error("请先登录"), intelligencePlaybackTracks(response, seed()))
    }

    @Test
    fun nullServerMessageUsesReadableFallback() {
        val response = json.decodeFromString<Intelligence>("""{"code":500,"message":null}""")
        val result = intelligencePlaybackTracks(response, null) as Resource.Error
        assertTrue(result.message.contains("心动模式"))
    }

    private fun response(entries: String): Intelligence =
        json.decodeFromString("""{"code":200,"data":[$entries]}""")

    private fun seed() = MediaMetadata(
        id = 1, title = "Seed", coverUrl = "", artists = emptyList(), duration = 100,
        album = MediaMetadata.Album(1, "Album"),
    )
}
