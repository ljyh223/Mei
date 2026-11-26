package com.ljyh.mei.data.network.netease

import com.ljyh.mei.playback.log.PendingPlaybackLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal enum class PlaybackLogUpload { ACCEPTED, RETRY, REJECTED }

/** Multipart NCBL must bypass the EAPI/XEAPI interceptor and its mutable account cookies. */
@Singleton
class PlaybackLogClient internal constructor(private val client: HttpClient) {
    @Inject constructor() : this(HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        engine { config {
            dns(PlaybackLogDns())
            followRedirects(false)
            followSslRedirects(false)
        } }
    })

    internal suspend fun upload(
        log: PendingPlaybackLog,
        channel: String,
        account: PlaybackLogAccount,
    ): PlaybackLogUpload {
        require(channel == "ua" || channel == "bi")
        require(log.owner == account.owner)
        val profile = account.profile
        val meta = buildJsonObject {
            put("MUSIC_U", account.musicU)
            profile["URS_APPID"]?.let { put("URS_APPID", it) }
            put("appver", profile.getValue("appver"))
            put("buildver", profile.getValue("buildver"))
        }.toString().toByteArray()
        val record = if (channel == "ua") log.ua else log.bi
        require(log.entropy.size == 100)
        val entropy = if (channel == "ua") log.entropy.copyOfRange(0, 50) else log.entropy.copyOfRange(50, 100)
        val payload = try {
            NcblCodec.encode(meta, record.toByteArray(), entropy)
        } catch (_: LinkageError) {
            throw IOException("NCBL compression unavailable")
        }
        val filename = log.fileName(channel)
        val response = client.post(ENDPOINT) {
            header(HttpHeaders.UserAgent, profile.getValue("ua"))
            header(HttpHeaders.Cookie, buildList {
                add("MUSIC_U=${account.musicU}")
                add("deviceId=${account.deviceId}")
                add("sDeviceId=${account.deviceId}")
                for (key in listOf("os", "osver", "appver", "buildver", "versioncode", "channel", "mobilename", "URS_APPID")) {
                    profile[key]?.let { add("$key=${it.replace(" ", "+")}") }
                }
            }.joinToString("; "))
            header("x-music-u", account.musicU)
            header("x-deviceid", account.deviceId)
            header("x-sdeviceid", account.deviceId)
            header("x-os", "android")
            for (key in listOf("osver", "appver", "buildver")) header("x-$key", profile.getValue(key))
            setBody(MultiPartFormDataContent(formData {
                append("file", payload, Headers.build {
                    append(HttpHeaders.ContentType, "multipart/form-data")
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }))
        }
        val status = response.status.value
        if (status == 429 || status >= 500) return PlaybackLogUpload.RETRY
        if (status !in 200..299) return PlaybackLogUpload.REJECTED
        val body = runCatching { Json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()
            ?: return PlaybackLogUpload.RETRY
        val code = (body["code"] as? JsonPrimitive)?.intOrNull
        val successes = ((body["data"] as? JsonObject)?.get("successfiles") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
        return when {
            code == 200 && filename in successes -> PlaybackLogUpload.ACCEPTED
            code in listOf(301, 400, 401, 403) -> PlaybackLogUpload.REJECTED
            else -> PlaybackLogUpload.RETRY
        }
    }

    internal companion object {
        const val ENDPOINT = "https://clientlogsf.music.163.com/api/clientlog/encrypt/upload?multiupload=true"
    }
}
