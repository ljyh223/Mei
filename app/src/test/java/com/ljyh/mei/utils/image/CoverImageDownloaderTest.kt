package com.ljyh.mei.utils.image

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverImageDownloaderTest {
    @Test
    fun downloadedPngIsKeptWithoutDecodeAndReencode() = runBlocking {
        val png = requireNotNull(javaClass.getResourceAsStream("/audio-tags/cover.png"))
            .use { it.readBytes() }
        val client = fakeClient(200, png)
        assertArrayEquals(png, CoverImageDownloader.downloadImageBytes("https://example.test/cover.png", client))
    }

    @Test
    fun oversizedAndFailedCoverRequestsAreIgnored() = runBlocking {
        assertNull(CoverImageDownloader.downloadImageBytes(
            "https://example.test/cover.png", fakeClient(200, ByteArray(8 * 1024 * 1024 + 1)),
        ))
        assertNull(CoverImageDownloader.downloadImageBytes(
            "https://example.test/missing.png", fakeClient(404, ByteArray(0)),
        ))
    }

    private fun fakeClient(code: Int, bytes: ByteArray): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Not Found")
                .body(bytes.toResponseBody())
                .build()
        }
        .build()
}
