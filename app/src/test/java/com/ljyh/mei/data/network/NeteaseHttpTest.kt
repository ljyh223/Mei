package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.di.NeteaseHeader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeteaseHttpTest {
    @Test
    fun eapiHeaderIncludesAuthenticatedCookieValue() {
        val header = NeteaseHeader(
            osver = "14", deviceId = "device", os = "android", appver = "1",
            versioncode = "1", mobilename = "phone", buildver = "1", resolution = "1080x1920",
        ).apply { MUSIC_U = "token" }
        val encoded = Json { encodeDefaults = true; explicitNulls = false }.encodeToString(header)
        assertTrue(encoded.contains("\"MUSIC_U\":\"token\""))
    }

    @Test
    fun postsToTheSelectedHostAndPreservesCryptoMode() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("music.163.com", request.url.host)
            assertEquals("/api/music-vip-membership/front/vip/info", request.url.encodedPath)
            assertEquals("weapi", request.headers["X-Netease-Crypto"])
            respond(
                """{"code":200,"extra":"ignored"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        try {
            val http = NeteaseHttp(client, Json { ignoreUnknownKeys = true })
            val result: BaseResponse = http.post(
                "https://music.163.com",
                "/api/music-vip-membership/front/vip/info",
                mapOf("x" to "y"),
                cryptoMode = "weapi",
            )
            assertEquals(200, result.code)
        } finally {
            client.close()
        }
    }
}
