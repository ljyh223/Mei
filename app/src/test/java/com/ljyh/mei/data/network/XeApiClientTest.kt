package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.utils.encrypt.XeApiPublicKey
import java.util.Base64
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XeApiClientTest {
    @Test
    fun publicKeyCacheRefreshesBeforeExpiryAndRejectsInvalidKeys() {
        val now = 1_000_000_000L
        val key = XeApiPublicKey(
            publicKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }),
            version = "1",
            sk = "test-sk",
            nextUpdateTime = now + 8L * 24 * 60 * 60 * 1000,
        )

        assertTrue(key.isUsableAt(now))
        assertFalse(key.needsRefreshAt(now))
        assertTrue(key.copy(nextUpdateTime = now + 7L * 24 * 60 * 60 * 1000).needsRefreshAt(now))
        assertFalse(key.copy(nextUpdateTime = now).isUsableAt(now))
        assertFalse(key.copy(publicKey = "invalid").isUsableAt(now))
        assertFalse(key.copy(sk = "").isUsableAt(now))
    }

    @Test
    fun publicKeyRegistrationKeepsItsSeparateEapiFields() {
        val params = buildPublicKeyRegistrationParams(
            currentVersion = "1",
            timestamp = "123",
            nonce = "456",
            signature = "signed",
            deviceId = "device",
            userId = "user",
        )

        assertEquals("1", params["currentKeyVersion"])
        assertEquals("signed", params["signature"])
        assertEquals("device", params["deviceId"])
        assertEquals("user", params["uid"])
        assertEquals("9.6.05", params["appVersion"])
    }

    @Test
    fun lyricV1UsesCapturedQueryContract() {
        val url = buildLyricV1Url("536623722")

        assertEquals("/xeapi/song/lyric/v1", url.encodedPath)
        assertEquals(
            "id=536623722&cp=false&lv=0&tv=0&kv=0&rv=0&yv=0&ytv=0&yrv=0",
            url.encodedQuery,
        )
        assertFalse(url.encodedQuery.contains("e_r="))
        val plaintext = buildLyricV1Request("536623722").encryptionPlaintext().decodeToString()
        assertTrue(plaintext.contains("yrv=0&e_r=true"))
        assertEquals(1, "e_r=true".toRegex().findAll(plaintext).count())
    }

    @Test
    fun songUrlV1UsesTheCapturedQueryContract() {
        val url = buildSongUrlV1Url(
            GetSongUrlV1(ids = "[393594,409647155]", level = "exhigh"),
            userId = "1234567890",
            json = Json,
        )

        assertEquals("interface3.music.163.com", url.host)
        assertEquals("/xeapi/song/enhance/player/url/v1", url.encodedPath)
        assertEquals(
            "ids=%5B%22393594_0%22%2C%22409647155_0%22%5D",
            url.encodedQuery.substringBefore('&'),
        )
        assertEquals(14, url.parameters.names().size)
        assertEquals("[\"393594_0\",\"409647155_0\"]", url.parameters["ids"])
        assertEquals("aac", url.parameters["encodeType"])
        assertEquals("1234567890", url.parameters["cliUserId"])
        assertEquals("{\"393594\":1,\"409647155\":1}", url.parameters["trialModes"])
    }

    @Test
    fun losslessKeepsFlacEncoding() {
        val url = buildSongUrlV1Url(
            GetSongUrlV1(ids = "[393594]", level = "lossless"),
            userId = "1234567890",
            json = Json,
        )

        assertEquals("flac", url.parameters["encodeType"])
    }
}
