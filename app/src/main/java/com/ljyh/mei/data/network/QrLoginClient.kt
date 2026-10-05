package com.ljyh.mei.data.network

import com.google.gson.Gson
import com.ljyh.mei.data.model.auth.QrLoginCheck
import com.ljyh.mei.data.model.auth.QrLoginCheckResponse
import com.ljyh.mei.data.model.auth.QrLoginKeyResponse
import com.ljyh.mei.data.model.auth.extractMusicU
import com.ljyh.mei.utils.encrypt.encryptWeAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
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
    private val client: OkHttpClient,
    private val baseUrl: String,
) {
    @Inject
    constructor() : this(newHttpClient(), BASE_URL)

    internal constructor(baseUrl: String) : this(newHttpClient(), baseUrl.trimEnd('/'))

    private val gson = Gson()
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
        return gson.fromJson(json, QrLoginKeyResponse::class.java)
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
        val body = gson.fromJson(json, QrLoginCheckResponse::class.java)
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

    internal fun buildRequest(path: String, payload: Map<String, Any>): Request {
        val generation = synchronized(cookieLock) { sessionGeneration }
        return buildRequest(generation, path, payload)
    }

    private fun buildRequest(
        generation: Long,
        path: String,
        payload: Map<String, Any>,
    ): Request {
        val cookieSnapshot = synchronized(cookieLock) {
            if (generation != sessionGeneration) throw CancellationException("QR login session replaced")
            cookies.toMap()
        }
        val csrf = cookieSnapshot["__csrf"].orEmpty()
        val encrypted = encryptWeAPI(gson.toJson(payload + ("csrf_token" to csrf)))
        return Request.Builder()
            .url("$baseUrl/weapi$path")
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
            .header("Cookie", cookieHeader(cookieSnapshot))
            .post(
                FormBody.Builder()
                    .add("params", encrypted.params)
                    .add("encSecKey", encrypted.encSecKey)
                    .build(),
            )
            .build()
    }

    private suspend fun perform(
        generation: Long,
        path: String,
        payload: Map<String, Any>,
    ): String {
        val request = buildRequest(generation, path, payload)
        val call = client.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            if (!continuation.isActive) return
                            val responseCookies = Cookie.parseAll(response.request.url, response.headers)
                            if (!absorbCookies(generation, response.request.url, responseCookies)) {
                                throw CancellationException("QR login session replaced")
                            }
                            if (!response.isSuccessful) {
                                error("QR login request failed with HTTP ${response.code}")
                            }
                            val body = response.body.string()
                            if (!isCurrentSession(generation)) {
                                throw CancellationException("QR login session replaced")
                            }
                            if (continuation.isActive) {
                                continuation.resumeWith(Result.success(body))
                            }
                        }
                    } catch (throwable: Throwable) {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(throwable))
                        }
                    }
                }
            })
        }
    }

    private fun isCurrentSession(generation: Long): Boolean = synchronized(cookieLock) {
        generation == sessionGeneration
    }

    private fun absorbCookies(
        generation: Long,
        url: okhttp3.HttpUrl,
        parsed: List<Cookie>,
    ): Boolean = synchronized(cookieLock) {
        if (generation != sessionGeneration) return@synchronized false
        parsed.forEach { cookie ->
            if (cookie.matches(url) && cookie.value.isNotEmpty() && cookie.value != "\"\"") {
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

        fun newHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
