package com.ljyh.mei.data.network.qqandroid

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QqAndroidTransportTest {
    @Test
    fun ktorTransportSendsSignedCompressedPayloadAndParsesModuleResult() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("https://u6.y.qq.com/cgi-bin/musics.fcg", request.url.toString())
            assertEquals("m1", request.headers["M-Encoding"])
            assertEquals(QqAndroidTransport.USER_AGENT, request.headers["User-Agent"])
            val content = request.body as ByteArrayContent
            assertEquals(ContentType.Application.FormUrlEncoded, content.contentType)
            val compressed = content.bytes()
            val plaintext = InflaterInputStream(ByteArrayInputStream(compressed, 5, compressed.size - 5))
                .readBytes()
            val signed = Base64.getDecoder().decode(request.headers["sign"])
            assertEquals(request.headers["sign"], QqAndroidCrypto.sign(plaintext, signed.copyOfRange(0, 12)))
            assertTrue(plaintext.decodeToString().contains("\"module\":\"music.search.SearchCgiService\""))
            respond(
                """{"code":0,"music.search.SearchCgiService.DoSearchForQQMusicDesktop":{"code":0}}""",
                HttpStatusCode.OK,
            )
        })
        val response = QqAndroidTransport(client).call(
            QqAndroidDevice.synthetic(),
            "music.search.SearchCgiService",
            "DoSearchForQQMusicDesktop",
            QqAndroidParams.search("晴天"),
        )
        assertEquals("0", response["code"]?.jsonPrimitive?.content)
        assertEquals(
            "0",
            response["music.search.SearchCgiService.DoSearchForQQMusicDesktop"]
                ?.jsonObject?.get("code")?.jsonPrimitive?.content,
        )
        client.close()
    }
}
