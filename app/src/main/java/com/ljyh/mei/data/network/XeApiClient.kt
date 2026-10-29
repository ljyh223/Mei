package com.ljyh.mei.data.network

import android.content.Context
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.response.SongUrl
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.playback.equalizer.auto.TrackAudioFeatures
import com.ljyh.mei.playback.equalizer.auto.TrackAudioFeaturesParser
import com.ljyh.mei.utils.encrypt.XeApiCrypto
import com.ljyh.mei.utils.encrypt.XeApiPublicKey
import com.ljyh.mei.utils.encrypt.XeApiSession
import com.ljyh.mei.utils.encrypt.decodeEApiResponse
import com.ljyh.mei.utils.netease.getDeviceId
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber

private const val KEY_REFRESH_LEAD_MS = 7L * 24 * 60 * 60 * 1000
private const val KEY_REFRESH_RETRY_MS = 30L * 60 * 1000

/** Android 9.6.05 XEAPI C/S/R transport shared by player URLs, lyrics and likes. */
class XeApiClient(
    private val context: Context,
    private val http: NeteaseHttp,
    private val json: Json,
    private val crypto: XeApiCrypto = XeApiCrypto(),
) {
    private val keyLock = Mutex()
    private val keyStore = context.getSharedPreferences("netease_xeapi", Context.MODE_PRIVATE)
    private var nextKeyRefreshAttemptAt = 0L
    @Volatile
    private var publicKey: XeApiPublicKey? = null
    @Volatile
    private var session: XeApiSession? = null
    // Keep this separate from NeteaseHttp: its interceptor adds account cookies to requests.
    private val antiCheatClient = HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }
    private val watchmanTokenProvider = WatchmanTokenProvider(context)

    suspend fun getSongUrlV1(body: GetSongUrlV1): SongUrl {
        val request = buildSongUrlV1Request(body, context.dataStore[UserIdKey].orEmpty(), json)
        return json.decodeFromString(requestRaw(request))
    }

    suspend fun getEmotionAudioEffect(songIds: List<Long>): List<TrackAudioFeatures> {
        if (songIds.isEmpty()) return emptyList()
        return TrackAudioFeaturesParser.parse(requestRaw(buildEmotionAudioEffectRequest(songIds)))
    }

    suspend fun getLyricV1(id: String): Lyric =
        json.decodeFromString(requestRaw(buildLyricV1Request(id)))

    suspend fun searchOverview(keyword: String): JsonObject =
        json.parseToJsonElement(requestRaw(buildSearchOverviewRequest(keyword))).jsonObject

    suspend fun like(body: Like): BaseResponse {
        return json.decodeFromString(requestRaw(buildLikeRequest(body)))
    }

    suspend fun subscribePlaylist(id: String): BaseResponse =
        decodePlaylistSubscribeResponse(requestRaw(buildSubscribePlaylistRequest(id)), json)

    private suspend fun requestRaw(
        request: XeApiRequest,
        retryOnRefresh: Boolean = true,
    ): String {
        val key = getPublicKey()
        val encryptedBody = crypto.encrypt(request.encryptionPlaintext(), key, session)
        val antiCheatToken = when (request.antiCheatTokenVersion) {
            AntiCheatTokenVersion.V2 -> watchmanTokenProvider.getToken()
            AntiCheatTokenVersion.V3 -> fetchAntiCheatTokenV3()
            null -> null
        }
        val response = http.client.post(request.url.toString()) {
            header("X-Netease-Crypto", "xeapi")
            header("cmpageid", request.pageId)
            if (antiCheatToken != null) header("X-antiCheatToken", antiCheatToken)
            setBody(
                TextContent(
                    encryptedBody,
                    ContentType.parse("application/x-www-form-urlencoded;charset=utf-8"),
                )
            )
        }
        val status = response.headers["x-ud-sts"]
        val sessionId = response.headers["x-encr-ssid"]
        val sessionKey = response.headers["x-encr-sskey"]
        val sessionUpdated = !sessionId.isNullOrBlank() && !sessionKey.isNullOrBlank()
        if (!sessionId.isNullOrBlank() && !sessionKey.isNullOrBlank()) {
            session = XeApiSession(sessionId, sessionKey)
        }
        if (status == "10000" || status == "20002") {
            publicKey = null
            session = null
            keyStore.edit().remove("public_key").apply()
            if (retryOnRefresh) {
                getPublicKey(forceRefresh = true)
                return requestRaw(request, retryOnRefresh = false)
            }
        }
        if (status == "8888" || status == "10000" || status == "20000" || status == "20002") {
            throw IOException("XEAPI server status $status")
        }
        val bytes = response.bodyAsBytes()
        Timber.tag("XEAPI").d(
            "path=%s status=%s bytes=%d sessionUpdated=%s antiCheatSent=%s",
            request.url.encodedPath, status, bytes.size, sessionUpdated, !antiCheatToken.isNullOrBlank(),
        )
        if (bytes.isEmpty()) throw IOException("XEAPI returned an empty response (status=$status)")
        return decodeEApiResponse(bytes)
    }

    private suspend fun fetchAntiCheatTokenV3(): String? {
        try {
            val response = antiCheatClient.get("https://ac.dun.163yun.com/v3/b?pn=YD00000558929251")
            if (response.status.value !in 200..299) return null
            return parseAntiCheatTokenV3(response.bodyAsText())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun getPublicKey(forceRefresh: Boolean = false): XeApiPublicKey =
        keyLock.withLock {
            val now = System.currentTimeMillis()
            val cached = publicKey?.takeIf { it.isUsableAt(now) }
                ?: keyStore.getString("public_key", null)
                    ?.let { stored -> runCatching { json.decodeFromString<XeApiPublicKey>(stored) }.getOrNull() }
                    ?.takeIf { it.isUsableAt(now) }
                    ?.also { publicKey = it }
            if (!forceRefresh && cached != null &&
                (!cached.needsRefreshAt(now) || now < nextKeyRefreshAttemptAt)
            ) {
                return@withLock cached
            }
            try {
                val fresh = registerPublicKey(cached?.version.orEmpty())
                publicKey = fresh
                session = null
                nextKeyRefreshAttemptAt = now + KEY_REFRESH_RETRY_MS
                keyStore.edit().putString("public_key", json.encodeToString(fresh)).apply()
                fresh
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (forceRefresh) throw error
                cached?.also {
                    nextKeyRefreshAttemptAt = now + KEY_REFRESH_RETRY_MS
                    Timber.tag("XEAPI").w(error, "Public-key registration failed; using valid cached key")
                } ?: throw error
            }
        }

    private suspend fun registerPublicKey(currentVersion: String): XeApiPublicKey {
        val nonce = crypto.nonce()
        val timestamp = System.currentTimeMillis().toString()
        val deviceId = context.dataStore[DeviceIdKey] ?: getDeviceId(context)
        val values = buildPublicKeyRegistrationParams(
            currentVersion = currentVersion,
            timestamp = timestamp,
            nonce = nonce,
            signature = crypto.sign(timestamp, nonce),
            deviceId = deviceId,
            userId = context.dataStore[UserIdKey].orEmpty(),
        )
        val response: JsonObject = http.post(
            XEAPI_BASE,
            "/eapi/bsr/sk/get",
            values,
        )
        require(response["code"]?.jsonPrimitive?.content == "200") {
            "XEAPI public-key registration failed"
        }
        val data = response["data"]?.jsonObject ?: error("XEAPI public-key response missing data")
        val responseTimestamp = data["timestamp"]?.jsonPrimitive?.content
            ?: error("XEAPI public-key response missing timestamp")
        val signature = data["signature"]?.jsonPrimitive?.content
            ?: error("XEAPI public-key response missing signature")
        require(
            MessageDigest.isEqual(
                signature.toByteArray(StandardCharsets.US_ASCII),
                crypto.sign(responseTimestamp, nonce).toByteArray(StandardCharsets.US_ASCII),
            )
        ) { "XEAPI public-key response signature mismatch" }
        val encryptedData = data["encryptedData"]?.jsonPrimitive?.content
            ?: error("XEAPI public-key response missing encryptedData")
        val key = json.decodeFromString<XeApiPublicKey>(crypto.decryptPublicKey(encryptedData))
        require(key.isUsableAt(System.currentTimeMillis())) {
            "XEAPI public-key response is invalid or expired"
        }
        return key
    }

}

internal fun decodePlaylistSubscribeResponse(raw: String, json: Json): BaseResponse {
    val fields = json.parseToJsonElement(raw).jsonObject
    val response = BaseResponse(
        code = (fields["code"] as? JsonPrimitive)?.intOrNull
            ?: throw IOException("XEAPI 收藏歌单响应缺少 code"),
    )
    if (response.code != 200) {
        val reason = sequenceOf("message", "msg")
            .mapNotNull { (fields[it] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.isNotBlank() && it != "null" }
            ?.take(160)
        throw IOException("XEAPI 收藏歌单失败：${response.code}${reason?.let { "，$it" }.orEmpty()}")
    }
    return response
}

internal fun XeApiPublicKey.isUsableAt(now: Long): Boolean =
    nextUpdateTime > now && version.isNotBlank() && sk.isNotBlank() &&
        runCatching { Base64.getDecoder().decode(publicKey).size == 32 }.getOrDefault(false)

internal fun XeApiPublicKey.needsRefreshAt(now: Long): Boolean =
    nextUpdateTime - now <= KEY_REFRESH_LEAD_MS

internal fun parseAntiCheatTokenV3(response: String): String? =
    Regex("""null\(\[200,\d+,"([^"]+)"\]\)""").find(response)?.groupValues?.get(1)
