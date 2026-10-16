package com.ljyh.mei.data.network

import android.content.Context
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.response.SongUrl
import com.ljyh.mei.data.model.weapi.Like
import com.ljyh.mei.utils.encrypt.XeApiCrypto
import com.ljyh.mei.utils.encrypt.XeApiPublicKey
import com.ljyh.mei.utils.encrypt.XeApiSession
import com.ljyh.mei.utils.encrypt.decodeEApiResponse
import com.ljyh.mei.utils.netease.getDeviceId
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import timber.log.Timber

private const val XEAPI_BASE = "https://interface3.music.163.com"

/** Android 9.6.05 XEAPI C/S/R transport shared by player URLs, lyrics and likes. */
class XeApiClient(
    private val context: Context,
    private val http: NeteaseHttp,
    private val json: Json,
    private val crypto: XeApiCrypto = XeApiCrypto(),
) {
    private val keyLock = Mutex()
    private val keyStore = context.getSharedPreferences("netease_xeapi", Context.MODE_PRIVATE)
    @Volatile private var publicKey: XeApiPublicKey? = null
    @Volatile private var session: XeApiSession? = null
    private val antiCheatClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getSongUrlV1(body: GetSongUrlV1): SongUrl {
        val url = buildSongUrlV1Url(body, context.dataStore[UserIdKey].orEmpty(), json)
        return json.decodeFromString(requestRaw(url))
    }

    suspend fun getLyricV1(id: String): Lyric =
        json.decodeFromString(requestRaw(buildLyricV1Url(id)))

    suspend fun like(body: Like): BaseResponse {
        return json.decodeFromString(requestRaw(
            xeApiUrl("v1/radio/like"),
            encodedContent = encodeLikeContent(body),
            requireAntiCheatToken = true,
        ))
    }

    private suspend fun requestRaw(
        url: HttpUrl,
        encodedContent: String = "",
        retryOnRefresh: Boolean = true,
        requireAntiCheatToken: Boolean = false,
    ): String {
        val key = getPublicKey()
        val requestBody = crypto.encrypt(url.encodedQuery.orEmpty(), key, session, encodedContent)
        val antiCheatToken = if (requireAntiCheatToken) fetchAntiCheatTokenV3() else null
        val response = http.client.post(url.toString()) {
            header("X-Netease-Crypto", "xeapi")
            if (antiCheatToken != null) header("X-antiCheatToken", antiCheatToken)
            setBody(TextContent(
                requestBody,
                ContentType.parse("application/x-www-form-urlencoded;charset=utf-8"),
            ))
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
            if (retryOnRefresh) {
                getPublicKey(forceRefresh = true)
                return requestRaw(url, encodedContent, retryOnRefresh = false,
                    requireAntiCheatToken = requireAntiCheatToken)
            }
        }
        if (status == "8888" || status == "10000" || status == "20000" || status == "20002") {
            throw IOException("XEAPI server status $status")
        }
        val bytes = response.bodyAsBytes()
        Timber.tag("XEAPI").d(
            "path=%s status=%s bytes=%d sessionUpdated=%s",
            url.encodedPath, status, bytes.size, sessionUpdated,
        )
        if (bytes.isEmpty()) throw IOException("XEAPI returned an empty response (status=$status)")
        return decodeEApiResponse(bytes)
    }

    private suspend fun fetchAntiCheatTokenV3(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://ac.dun.163yun.com/v3/b?pn=YD00000558929251")
                .get()
                .build()
            antiCheatClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                parseAntiCheatTokenV3(response.body.string())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getPublicKey(forceRefresh: Boolean = false): XeApiPublicKey =
        keyLock.withLock {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                publicKey?.takeIf {
                    it.nextUpdateTime > now &&
                        (it != BOOTSTRAP_KEY || it.nextUpdateTime > now + KEY_REFRESH_LEAD_MS)
                }?.let { return@withLock it }
                keyStore.getString("public_key", null)?.let { stored ->
                    runCatching { json.decodeFromString<XeApiPublicKey>(stored) }
                        .getOrNull()?.takeIf { it.nextUpdateTime > now }
                        ?.let { publicKey = it; return@withLock it }
                }
                if (BOOTSTRAP_KEY.nextUpdateTime > now + KEY_REFRESH_LEAD_MS) {
                    publicKey = BOOTSTRAP_KEY
                    return@withLock BOOTSTRAP_KEY
                }
            }
            try {
                val fresh = registerPublicKey(publicKey?.version.orEmpty())
                publicKey = fresh
                keyStore.edit().putString("public_key", json.encodeToString(fresh)).apply()
                fresh
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (forceRefresh) throw error
                // The public snapshot used by the successful local test expires in March 2027.
                BOOTSTRAP_KEY.takeIf { it.nextUpdateTime > now }?.also {
                    publicKey = it
                    Timber.tag("XEAPI").w(error, "Public-key registration failed; using valid bootstrap key")
                } ?: throw error
            }
        }

    private suspend fun registerPublicKey(currentVersion: String): XeApiPublicKey {
        val nonce = crypto.nonce()
        val timestamp = System.currentTimeMillis().toString()
        val deviceId = context.dataStore[DeviceIdKey] ?: getDeviceId(context)
        val values = linkedMapOf(
            "currentKeyVersion" to currentVersion,
            "timestamp" to timestamp,
            "nonce" to nonce,
            "requestType" to "active",
            "signature" to crypto.sign(timestamp, nonce),
            "t1" to "",
            "t2" to "",
            "os" to "android",
            "appVersion" to "9.6.05",
            "deviceId" to deviceId,
            "uid" to context.dataStore[UserIdKey].orEmpty(),
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
        require(MessageDigest.isEqual(
            signature.toByteArray(StandardCharsets.US_ASCII),
            crypto.sign(responseTimestamp, nonce).toByteArray(StandardCharsets.US_ASCII),
        )) { "XEAPI public-key response signature mismatch" }
        val encryptedData = data["encryptedData"]?.jsonPrimitive?.content
            ?: error("XEAPI public-key response missing encryptedData")
        val key = json.decodeFromString<XeApiPublicKey>(crypto.decryptPublicKey(encryptedData))
        require(key.nextUpdateTime > System.currentTimeMillis()) {
            "XEAPI public key has expired"
        }
        return key
    }

    private companion object {
        const val KEY_REFRESH_LEAD_MS = 7L * 24 * 60 * 60 * 1000
        val BOOTSTRAP_KEY = XeApiPublicKey(
            publicKey = "3m5wN9om11qRESjEV+5EoFf9qLEylO6gyThMbl1XxEk=",
            version = "1000000000000",
            nextUpdateTime = 1803882269000,
            sk = "FCSGJukj1781616326176",
        )
    }
}

internal fun xeApiUrl(path: String): HttpUrl = "$XEAPI_BASE/xeapi/${path.trimStart('/')}".toHttpUrl()

internal fun buildLyricV1Url(id: String): HttpUrl =
    xeApiUrl("song/lyric/v1").newBuilder().addQueryParameter("id", id).build()

internal fun encodeLikeContent(body: Like): String {
    val form = FormBody.Builder()
        .add("alg", body.alg)
        .add("trackId", body.trackId)
        .add("like", body.like.toString())
        .add("time", body.time)
        .build()
    return Buffer().apply { form.writeTo(this) }.readUtf8()
}

internal fun parseAntiCheatTokenV3(response: String): String? =
    Regex("""null\(\[200,\d+,"([^"]+)"\]\)""").find(response)?.groupValues?.get(1)

internal fun buildSongUrlV1Url(body: GetSongUrlV1, userId: String, json: Json): HttpUrl {
    val ids = json.parseToJsonElement(body.ids).let { element ->
        (element as? kotlinx.serialization.json.JsonArray)?.map {
            it.jsonPrimitive.content.removeSuffix("_0")
        } ?: error("Invalid song IDs")
    }
    require(ids.isNotEmpty()) { "Empty song IDs" }
    val wireIds = buildJsonArray { ids.forEach { add(JsonPrimitive("${it}_0")) } }.toString()
    val trials = buildJsonObject { ids.forEach { put(it, 1) } }.toString()
    val encodeType = if (body.level in setOf("standard", "exhigh")) "aac" else body.encodeType
    return "$XEAPI_BASE/xeapi/song/enhance/player/url/v1".toHttpUrl().newBuilder()
        .addQueryParameter("ids", wireIds)
        .addQueryParameter("level", body.level)
        .addQueryParameter("encodeType", encodeType)
        .addQueryParameter("trialMode", "1")
        .addQueryParameter("faql", "0")
        .addQueryParameter("immerseType", body.immerseType ?: "ste2")
        .addQueryParameter("degradeImmerseType", "aac")
        .addQueryParameter("expParams", "{\"hy-zyyinzhi\":\"t1\"}")
        .addQueryParameter("cliUserId", userId)
        .addQueryParameter("cliVipTypes", "[1,6,15]")
        .addQueryParameter("trialModes", trials)
        .addQueryParameter("supportDolby", "false")
        .addQueryParameter("volumeBalance", "0")
        .addQueryParameter("djVolumeBalance", "1")
        .build()
}
