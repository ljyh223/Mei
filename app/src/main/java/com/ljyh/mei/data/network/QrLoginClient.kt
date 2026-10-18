package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.auth.QrLoginCheck
import com.ljyh.mei.data.model.auth.QrLoginCheckResponse
import com.ljyh.mei.data.model.auth.QrLoginKeyResponse
import com.ljyh.mei.data.model.auth.extractMusicU
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.matches
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolated NetEase PC-Web session used only for QR login.
 *
 * It deliberately bypasses [com.ljyh.mei.di.NeteaseInterceptor] so login requests do not mix
 * Android device cookies, a desktop browser user agent, or forged IP headers in one fingerprint.
 */
@Singleton
class QrLoginClient internal constructor(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    @Inject
    constructor() : this(newHttpClient(), BASE_URL)

    internal constructor(baseUrl: String) : this(newHttpClient(), baseUrl.trimEnd('/'))

    private val json = Json { ignoreUnknownKeys = true }
    private val cookieLock = Any()
    private val cookies = linkedMapOf<String, String>()
    private var sessionGeneration = 0L

    suspend fun createQrLoginKey(): QrLoginKeyResponse {
        val generation = synchronized(cookieLock) {
            cookies.clear()
            ++sessionGeneration
        }
        val json = perform(
            generation = generation,
            path = "/login/qrcode/unikey",
            payload = mapOf("type" to 1),
        )
        return this.json.decodeFromString<QrLoginKeyResponse>(json)
    }

    suspend fun checkQrLogin(unikey: String): QrLoginCheck {
        val generation = synchronized(cookieLock) { sessionGeneration }
        val json = perform(
            generation = generation,
            path = "/login/qrcode/client/login",
            payload = mapOf(
                "key" to unikey,
                "type" to 1,
            ),
        )
        val body = this.json.decodeFromString<QrLoginCheckResponse>(json)
        val storedMusicU = synchronized(cookieLock) {
            cookies["MUSIC_U"].takeIf { generation == sessionGeneration }
        }
        return QrLoginCheck(
            code = body.code,
            message = body.message,
            nickname = body.nickname,
            avatarUrl = body.avatarUrl,
            musicU = extractMusicU(
                listOf(
                    body.cookie,
                    storedMusicU?.let { "MUSIC_U=$it" },
                ),
            ),
        )
    }

    internal fun buildRequest(path: String, payload: Map<String, Any>): QrLoginRequest {
        val generation = synchronized(cookieLock) { sessionGeneration }
        return buildRequest(generation, path, payload)
    }

    private fun buildRequest(
        generation: Long,
        path: String,
        payload: Map<String, Any>,
    ): QrLoginRequest {
        val cookieSnapshot = synchronized(cookieLock) {
            if (generation != sessionGeneration) throw CancellationException("QR login session replaced")
            cookies.toMap()
        }
        val csrf = cookieSnapshot["__csrf"].orEmpty()
        val requestPayload = buildJsonObject {
            payload.forEach { (key, value) ->
                put(key, when (value) {
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    else -> JsonPrimitive(value.toString())
                })
            }
            put("csrf_token", JsonPrimitive(csrf))
        }
        val encrypted = encryptWeAPI(requestPayload.toString())
        return QrLoginRequest(
            url = "$baseUrl/weapi$path",
            headers = mapOf(
                HttpHeaders.UserAgent to USER_AGENT,
                HttpHeaders.Referrer to BASE_URL,
                HttpHeaders.Cookie to cookieHeader(cookieSnapshot),
            ),
            form = Parameters.build {
                append("params", encrypted.params)
                append("encSecKey", encrypted.encSecKey)
            },
        )
    }

    private suspend fun perform(
        generation: Long,
        path: String,
        payload: Map<String, Any>,
    ): String {
        val request = buildRequest(generation, path, payload)
        val response = client.post(request.url) {
            request.headers.forEach { (name, value) -> header(name, value) }
            setBody(FormDataContent(request.form))
        }
        val responseUrl = response.request.url
        val responseCookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            .mapNotNull { raw -> runCatching { parseServerSetCookieHeader(raw) }.getOrNull() }
        if (!absorbCookies(generation, responseUrl, responseCookies)) {
            throw CancellationException("QR login session replaced")
        }
        if (response.status.value !in 200..299) {
            error("QR login request failed with HTTP ${response.status.value}")
        }
        val body = response.bodyAsText()
        if (!isCurrentSession(generation)) {
            throw CancellationException("QR login session replaced")
        }
        return body
    }

    private fun isCurrentSession(generation: Long): Boolean = synchronized(cookieLock) {
        generation == sessionGeneration
    }

    private fun absorbCookies(
        generation: Long,
        url: Url,
        parsed: List<Cookie>,
    ): Boolean = synchronized(cookieLock) {
        if (generation != sessionGeneration) return@synchronized false
        parsed.forEach { cookie ->
            val defaultPath = url.encodedPath.substringBeforeLast('/', "").ifEmpty { "/" }
            val scoped = cookie.copy(
                domain = cookie.domain ?: url.host,
                path = cookie.path?.takeIf { it.startsWith('/') } ?: defaultPath,
            )
            if (scoped.matches(url) && scoped.value.isNotEmpty() && scoped.value != "\"\"") {
                cookies[cookie.name] = cookie.value
            }
        }
        true
    }

    private fun cookieHeader(cookieSnapshot: Map<String, String>): String =
        buildMap {
            put("os", "pc")
            put("appver", "3.1.17")
            putAll(cookieSnapshot)
        }.entries.joinToString("; ") { (name, value) -> "$name=$value" }

    private companion object {
        const val BASE_URL = "https://music.163.com"
        const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

        fun newHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }
    }
}

internal data class QrLoginRequest(
    val url: String,
    val headers: Map<String, String>,
    val form: Parameters,
)
