package com.ljyh.mei.data.model.auth

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrLoginModelsTest {
    @Test
    fun decodesBusinessErrorWithoutQrKey() {
        val response = Gson().fromJson(
            """{"code":400,"message":"risk control"}""",
            QrLoginKeyResponse::class.java,
        )

        assertEquals(400, response.code)
        assertEquals("risk control", response.message)
        assertNull(response.unikey)
    }

    @Test
    fun extractsMusicUFromJsonCookieField() {
        val cookie = "MUSIC_U=json-token; Max-Age=1296000; Path=/; HTTPOnly"

        assertEquals("json-token", extractMusicU(listOf(cookie)))
    }

    @Test
    fun extractsMusicUFromSetCookieWithoutReturningAttributes() {
        val headers = listOf(
            "NMTID=other; Path=/",
            "MUSIC_U=header-token; Expires=Wed, 09 Sep 2026 10:18:14 GMT; Path=/",
        )

        assertEquals("header-token", extractMusicU(headers))
    }

    @Test
    fun prefersJsonCookieWhenBothSourcesContainMusicU() {
        val cookies = listOf(
            "MUSIC_U=json-token; Path=/",
            "MUSIC_U=header-token; Path=/",
        )

        assertEquals("json-token", extractMusicU(cookies))
    }

    @Test
    fun returnsNullWhenMusicUIsAbsent() {
        assertNull(extractMusicU(listOf("NMTID=other; Path=/", null)))
    }

    @Test
    fun mapsKnownAndUnknownStatusCodes() {
        assertEquals(QrLoginStatus.Expired, QrLoginStatus.fromCode(800))
        assertEquals(QrLoginStatus.Waiting, QrLoginStatus.fromCode(801))
        assertEquals(QrLoginStatus.Scanned, QrLoginStatus.fromCode(802))
        assertEquals(QrLoginStatus.Success, QrLoginStatus.fromCode(803))
        assertEquals(QrLoginStatus.Unknown, QrLoginStatus.fromCode(500))
    }
}
