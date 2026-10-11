package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.api.GetSongUrlV1
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class XeApiClientTest {
    @Test
    fun songUrlV1UsesTheCapturedQueryContract() {
        val url = buildSongUrlV1Url(
            GetSongUrlV1(ids = "[393594,409647155]", level = "exhigh"),
            userId = "1234567890",
            json = Json,
        )

        assertEquals("interface3.music.163.com", url.host)
        assertEquals("/xeapi/song/enhance/player/url/v1", url.encodedPath)
        assertEquals(14, url.querySize)
        assertEquals("[\"393594_0\",\"409647155_0\"]", url.queryParameter("ids"))
        assertEquals("aac", url.queryParameter("encodeType"))
        assertEquals("1234567890", url.queryParameter("cliUserId"))
        assertEquals("{\"393594\":1,\"409647155\":1}", url.queryParameter("trialModes"))
    }

    @Test
    fun losslessKeepsFlacEncoding() {
        val url = buildSongUrlV1Url(
            GetSongUrlV1(ids = "[393594]", level = "lossless"),
            userId = "1234567890",
            json = Json,
        )

        assertEquals("flac", url.queryParameter("encodeType"))
    }
}
