package com.ljyh.mei.data.network.netease

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request

/** Only the playback-log transport may recover a blocked system DNS answer. */
internal class PlaybackLogDns(
    private val system: Dns = Dns.SYSTEM,
    private val resolve: () -> PlaybackLogDnsAnswer = PlaybackLogDoh()::lookup,
    private val nowNanos: () -> Long = System::nanoTime,
) : Dns {
    private var cached: PlaybackLogDnsAnswer? = null
    private var expiresAt = 0L
    private var retryAt = Long.MIN_VALUE

    override fun lookup(hostname: String): List<InetAddress> {
        if (!hostname.equals(HOST, ignoreCase = true)) return system.lookup(hostname)
        // Coalesce concurrent lookups; the independent request has a short, bounded timeout.
        synchronized(this) {
            val now = nowNanos()
            cached?.takeIf { now < expiresAt }?.let { return it.addresses }
            val local = try { system.lookup(hostname).filter(::isPublicAddress) }
                catch (_: UnknownHostException) { emptyList() }
            if (local.isNotEmpty()) return local
            if (now < retryAt) throw UnknownHostException("Playback log DNS temporarily unavailable")
            try {
                val answer = resolve()
                val addresses = answer.addresses.filter(::isPublicAddress).distinct()
                if (addresses.isEmpty()) throw UnknownHostException("Playback log DNS has no public addresses")
                cached = answer.copy(addresses = addresses)
                expiresAt = nowNanos() + TimeUnit.SECONDS.toNanos(answer.ttlSeconds.coerceIn(0, 600))
                retryAt = Long.MIN_VALUE
                return addresses
            } catch (failure: IOException) {
                retryAt = nowNanos() + TimeUnit.SECONDS.toNanos(10)
                throw UnknownHostException("Playback log DNS unavailable").apply { initCause(failure) }
            }
        }
    }

    companion object { const val HOST = "clientlogsf.music.163.com" }
}

internal data class PlaybackLogDnsAnswer(val addresses: List<InetAddress>, val ttlSeconds: Long)

/** No account headers, cookies, EAPI interception, redirects or relaxed TLS validation. */
internal class PlaybackLogDoh {
    private val client = OkHttpClient.Builder()
        .dns { hostname ->
            if (hostname != "dns.alidns.com") throw UnknownHostException(hostname)
            listOf(
                InetAddress.getByAddress(byteArrayOf(223.toByte(), 5, 5, 5)),
                InetAddress.getByAddress(byteArrayOf(223.toByte(), 6, 6, 6)),
            )
        }
        .callTimeout(6, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun lookup(): PlaybackLogDnsAnswer {
        val request = Request.Builder()
            .url("https://dns.alidns.com/resolve?name=${PlaybackLogDns.HOST}&type=A")
            .header("Accept", "application/dns-json")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("DNS HTTP ${response.code}")
            val source = response.body?.source() ?: throw IOException("Empty DNS response")
            if (source.request(65537)) throw IOException("Oversized DNS response")
            parse(source.readUtf8())
        }
    }

    companion object {
        internal fun parse(body: String): PlaybackLogDnsAnswer {
            try {
                val root = Json.parseToJsonElement(body) as JsonObject
                require((root["Status"] as? JsonPrimitive)?.intOrNull == 0)
                require((root["TC"] as? JsonPrimitive)?.booleanOrNull != true)
                val questions = when (val question = root["Question"]) {
                    is JsonObject -> listOf(question)
                    is JsonArray -> question.filterIsInstance<JsonObject>()
                    else -> emptyList()
                }
                require(questions.any {
                    (it["name"] as? JsonPrimitive)?.contentOrNull?.trimEnd('.')
                        ?.equals(PlaybackLogDns.HOST, ignoreCase = true) == true &&
                        (it["type"] as? JsonPrimitive)?.intOrNull == 1
                })
                val answers = (root["Answer"] as JsonArray).filterIsInstance<JsonObject>()
                val ttl = answers.minOf { (it["TTL"] as? JsonPrimitive)?.longOrNull ?: 0 }
                val addresses = answers.mapNotNull { record ->
                    if ((record["type"] as? JsonPrimitive)?.intOrNull != 1) return@mapNotNull null
                    val parts = (record["data"] as? JsonPrimitive)?.contentOrNull?.split('.') ?: return@mapNotNull null
                    if (parts.size != 4) return@mapNotNull null
                    val octets = parts.map { it.toIntOrNull()?.takeIf { n -> n in 0..255 } ?: return@mapNotNull null }
                    InetAddress.getByAddress(PlaybackLogDns.HOST, octets.map(Int::toByte).toByteArray())
                        .takeIf(::isPublicAddress)
                }.distinct()
                require(addresses.isNotEmpty())
                return PlaybackLogDnsAnswer(addresses, ttl)
            } catch (failure: Exception) {
                throw IOException("Invalid DNS response", failure)
            }
        }
    }
}

private fun isPublicAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
        address.isSiteLocalAddress || address.isMulticastAddress) return false
    val bytes = address.address
    val first = bytes[0].toInt() and 255
    return if (bytes.size == 4) {
        val second = bytes[1].toInt() and 255
        first != 0 && first < 224 && !(first == 100 && second in 64..127)
    } else first and 0xfe != 0xfc // IPv6 unique-local addresses are not upload destinations.
}
