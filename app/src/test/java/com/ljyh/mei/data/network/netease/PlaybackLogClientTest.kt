package com.ljyh.mei.data.network.netease

import com.ljyh.mei.playback.log.PendingPlaybackLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlaybackLogClientTest {
    private val profile = mapOf(
        "os" to "android", "osver" to "14", "appver" to "test-version",
        "buildver" to "test-build", "ua" to "Mei-protocol-test",
    )
    private val account = PlaybackLogAccount("123", "fictional-token", "fictional-device", profile)
    private val log = PendingPlaybackLog(
        id = "test-record", owner = account.owner, createdAtMs = 1000,
        ua = "1\u0001_plv\u0001{}\n", bi = "1\u0001startplay\u0001{}\n", entropy = ByteArray(100) { it.toByte() },
    )

    @Test fun multipartUsesCapturedIdentityAndRequiresExactFilenameAcknowledgment() = runBlocking {
        val http = HttpClient(MockEngine { request ->
            assertEquals("clientlogsf.music.163.com", request.url.host)
            assertEquals("/api/clientlog/encrypt/upload", request.url.encodedPath)
            assertEquals("true", request.url.parameters["multiupload"])
            assertEquals("fictional-token", request.headers["x-music-u"])
            assertTrue(request.headers[HttpHeaders.Cookie]!!.contains("MUSIC_U=fictional-token"))
            assertEquals("Mei-protocol-test", request.headers[HttpHeaders.UserAgent])
            assertNull(request.headers["X-Netease-Crypto"])
            assertNull(request.headers["X-Forwarded-For"])
            assertNull(request.headers[HttpHeaders.Referrer])
            assertEquals("multipart", request.body.contentType?.contentType)
            respond(
                "{\"code\":200,\"data\":{\"successfiles\":[\"${log.fileName("ua")}\"]}}",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        try {
            assertEquals(PlaybackLogUpload.ACCEPTED, PlaybackLogClient(http).upload(log, "ua", account))
        } finally { http.close() }
    }

    @Test fun http200AndOtherFileSuccessAreNotAcceptance() = runBlocking {
        val http = HttpClient(MockEngine {
            respond("{\"code\":200,\"data\":{\"successfiles\":[\"another-file\"]}}",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            assertEquals(PlaybackLogUpload.RETRY, PlaybackLogClient(http).upload(log, "ua", account))
        } finally { http.close() }
    }

    @Test fun pendingRecordCannotBeSentWithAnotherAccountsCredentials() = runBlocking {
        var requests = 0
        val http = HttpClient(MockEngine { requests++; respond("{}") })
        try {
            val other = PlaybackLogAccount("456", "another-fictional-token", "fictional-device", profile)
            val failure = runCatching { PlaybackLogClient(http).upload(log, "bi", other) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertEquals(0, requests)
            assertFalse(account.toString().contains("fictional-token"))
        } finally { http.close() }
    }

    @Test fun serverFailureIsRetryableAndUnauthorizedIsRejected() = runBlocking {
        for ((status, result) in listOf(
            HttpStatusCode.ServiceUnavailable to PlaybackLogUpload.RETRY,
            HttpStatusCode.TooManyRequests to PlaybackLogUpload.RETRY,
            HttpStatusCode.Forbidden to PlaybackLogUpload.REJECTED,
        )) {
            val http = HttpClient(MockEngine { respond("{}", status) })
            try {
                assertEquals(result, PlaybackLogClient(http).upload(log, "bi", account))
            } finally { http.close() }
        }
    }
}
