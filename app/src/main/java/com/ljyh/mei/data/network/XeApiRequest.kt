package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.api.GetSongUrlV1
import com.ljyh.mei.data.model.weapi.Like
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal const val XEAPI_BASE = "https://interface3.music.163.com"

/** Original request body, before the XEAPI interceptor replaces it with C/S/R. */
internal sealed interface XeApiOriginalBody {
    data class Form(val encodedContent: String = "") : XeApiOriginalBody
    data class JsonBody(val text: String) : XeApiOriginalBody
}

internal enum class AntiCheatTokenVersion { V2, V3 }

internal data class XeApiRequest(
    val url: Url,
    val originalBody: XeApiOriginalBody = XeApiOriginalBody.Form(),
    val antiCheatTokenVersion: AntiCheatTokenVersion? = null,
)

internal fun xeApiUrl(path: String): Url = Url("$XEAPI_BASE/xeapi/${path.trimStart('/')}")

internal fun buildPublicKeyRegistrationParams(
    currentVersion: String,
    timestamp: String,
    nonce: String,
    signature: String,
    deviceId: String,
    userId: String,
): Map<String, String> = linkedMapOf(
    "currentKeyVersion" to currentVersion,
    "timestamp" to timestamp,
    "nonce" to nonce,
    "requestType" to "active",
    "signature" to signature,
    "t1" to "",
    "t2" to "",
    "os" to "android",
    "appVersion" to "9.6.05",
    "deviceId" to deviceId,
    "uid" to userId,
)

internal fun buildLyricV1Request(id: String): XeApiRequest = XeApiRequest(
    url = buildLyricV1Url(id),
    originalBody = XeApiOriginalBody.JsonBody("{}"),
)

internal fun buildLyricV1Url(id: String): Url =
    URLBuilder().takeFrom(xeApiUrl("song/lyric/v1")).apply {
        parameters.append("id", id)
        parameters.append("cp", "false")
        parameters.append("lv", "0")
        parameters.append("tv", "0")
        parameters.append("kv", "0")
        parameters.append("rv", "0")
        parameters.append("yv", "0")
        parameters.append("ytv", "0")
        parameters.append("yrv", "0")
    }.build()

internal fun buildLikeRequest(body: Like): XeApiRequest = XeApiRequest(
    url = xeApiUrl("v1/radio/like"),
    originalBody = XeApiOriginalBody.Form(encodeLikeContent(body)),
    antiCheatTokenVersion = AntiCheatTokenVersion.V3,
)

internal fun buildSubscribePlaylistRequest(id: String): XeApiRequest = XeApiRequest(
    url = xeApiUrl("playlist/subscribe"),
    originalBody = XeApiOriginalBody.Form(listOf("id" to id).formUrlEncode()),
    antiCheatTokenVersion = AntiCheatTokenVersion.V2,
)

internal fun encodeLikeContent(body: Like): String {
    return listOf(
        "alg" to body.alg,
        "trackId" to body.trackId,
        "like" to body.like.toString(),
        "time" to body.time,
    ).formUrlEncode()
}

internal fun buildSongUrlV1Request(body: GetSongUrlV1, userId: String, json: Json): XeApiRequest =
    XeApiRequest(buildSongUrlV1Url(body, userId, json))

internal fun buildSongUrlV1Url(body: GetSongUrlV1, userId: String, json: Json): Url {
    val ids = json.parseToJsonElement(body.ids).let { element ->
        (element as? JsonArray)?.map { it.jsonPrimitive.content.removeSuffix("_0") }
            ?: error("Invalid song IDs")
    }
    require(ids.isNotEmpty()) { "Empty song IDs" }
    val wireIds = buildJsonArray { ids.forEach { add(JsonPrimitive("${it}_0")) } }.toString()
    val trials = buildJsonObject { ids.forEach { put(it, 1) } }.toString()
    val encodeType = if (body.level in setOf("standard", "exhigh")) "aac" else body.encodeType
    return URLBuilder().takeFrom(xeApiUrl("song/enhance/player/url/v1")).apply {
        parameters.append("ids", wireIds)
        parameters.append("level", body.level)
        parameters.append("encodeType", encodeType)
        parameters.append("trialMode", "1")
        parameters.append("faql", "0")
        parameters.append("immerseType", body.immerseType ?: "ste2")
        parameters.append("degradeImmerseType", "aac")
        parameters.append("expParams", "{\"hy-zyyinzhi\":\"t1\"}")
        parameters.append("cliUserId", userId)
        parameters.append("cliVipTypes", "[1,6,15]")
        parameters.append("trialModes", trials)
        parameters.append("supportDolby", "false")
        parameters.append("volumeBalance", "0")
        parameters.append("djVolumeBalance", "1")
    }.build()
}
