package com.ljyh.mei.data.network.qqandroid

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * `musics.fcg` 的传输层：组包 → sign/mask → M-Encoding → POST → 解析 JSON。
 *
 * 刻意不依赖 Android（设备档案由外部传入），这样 JVM 单测能直接打真实接口。
 */
class QqAndroidTransport(
    private val http: HttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    private val endpoint: String = DEFAULT_ENDPOINT,
) {

    /** 发一次请求，返回解析后的顶层 JSON；非 0 code 抛异常。 */
    suspend fun call(
        device: QqAndroidDevice,
        module: String,
        method: String,
        param: JsonObject,
    ): JsonObject {
        val fresh = device.refreshed()
        val payloadObject = buildJsonObject {
            put("comm", JsonObject(fresh.comm.mapValues { JsonPrimitive(it.value) }))
            putJsonObject("$module.$method") {
                put("module", module)
                put("method", method)
                put("param", param)
            }
        }
        val payload = payloadObject.toString().toByteArray(Charsets.UTF_8)

        val sign = QqAndroidCrypto.sign(payload)
        val mask = QqAndroidCrypto.mask(sign, fresh.custom())
        val body = QqAndroidCrypto.mEncode(payload)

        val response = http.post(endpoint) {
            header("Cookie", "")
            header("Accept", "*/*")
            header("sign", sign)
            header("User-Agent", USER_AGENT)
            header("M-Encoding", "m1")
            header("Accept-Encoding", "")
            header("x-sign-data-type", "json")
            header("mask", mask)
            setBody(ByteArrayContent(body, ContentType.Application.FormUrlEncoded))
        }
        if (response.status.value !in 200..299) error("musics.fcg HTTP ${response.status.value}")
        val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
        requireSuccessfulQqResponse(root, module, method)
        return root
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://u6.y.qq.com/cgi-bin/musics.fcg"
        val USER_AGENT = "QQMusic $QQ_ANDROID_VERSION_CODE(android 16)"

        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 20_000
            }
        }
    }
}

internal fun requireSuccessfulQqResponse(root: JsonObject, module: String, method: String) {
    val code = root["code"]?.jsonPrimitive?.intOrNull ?: -1
    if (code != 0) error("musics.fcg code=$code")
    val callKey = "$module.$method"
    val moduleResult = root[callKey]?.jsonObject
        ?: error("musics.fcg missing module result: $callKey")
    val moduleCode = moduleResult["code"]?.jsonPrimitive?.intOrNull
        ?: error("musics.fcg missing module code: $callKey")
    if (moduleCode != 0) error("musics.fcg $callKey code=$moduleCode")
}
