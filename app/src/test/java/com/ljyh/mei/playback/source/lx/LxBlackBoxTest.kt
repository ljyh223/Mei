package com.ljyh.mei.playback.source.lx

import com.dokar.quickjs.QuickJsException
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.ljyh.unblockneteasemusic.model.*
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import com.ljyh.unblockneteasemusic.kuwo.KuwoMusicProvider
import com.ljyh.unblockneteasemusic.migu.MiguMusicProvider
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Explicitly opt-in. Samples are loaded only by the test process; never print source or raw errors. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class LxBlackBoxTest {
    private val hostStatuses = java.util.concurrent.ConcurrentLinkedQueue<Int>()
    private val scriptClient = okhttp3.OkHttpClient.Builder().connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS).callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request()).also { hostStatuses.add(it.code) } }.build()
    private val runtime get() = LxRuntime(File("src/main/assets/lx/preload.js").readText()) { LxHttpTransport(scriptClient) }

    private fun samples(): List<File> {
        val path = System.getenv("MEI_LX_SAMPLE_DIR")
        assumeTrue("External sample execution requires an explicit sample directory", !path.isNullOrBlank())
        val files = File(path!!).listFiles { file -> file.isFile && file.extension.equals("js", true) }.orEmpty().sortedBy { it.name }
        val filter = System.getenv("MEI_LX_SAMPLE_FILTER").orEmpty()
            .split(',').map(String::trim).filter(String::isNotEmpty).toSet()
        val selected = if (filter.isEmpty()) files else files.filter { it.name in filter }
        assertTrue("No JS samples found in the selected directory", selected.isNotEmpty())
        return selected
    }

    private fun metadata(file: File, code: String): LxScript {
        val header = code.take(16_384).trimStart().takeIf { it.startsWith("/*") }?.substringBefore("*/").orEmpty()
        fun tag(name: String): String = Regex("(?m)^\\s*\\*?\\s*@${name}\\s+([^\\r\\n]+)")
            .find(header)?.groupValues?.get(1)?.trim()?.take(512).orEmpty()
        return LxScript("blackbox", tag("name").ifBlank { file.nameWithoutExtension }.take(128),
            tag("version"), tag("author"), tag("description"), tag("homepage"))
    }

    private fun failure(error: Exception): String = when {
        error is TimeoutCancellationException -> "TIMEOUT"
        error.message.orEmpty().contains("out of memory", true) -> "MEMORY_LIMIT"
        error.message.orEmpty().contains("not a function", true) -> "MISSING_FUNCTION"
        error.message.orEmpty().contains("is not defined", true) -> "MISSING_GLOBAL"
        error.message.orEmpty().contains("cannot read propert", true) -> "MISSING_OBJECT_PROPERTY"
        error.message.orEmpty().contains("ReferenceError", true) -> "MISSING_HOST_API"
        error.message.orEmpty().contains("SyntaxError", true) -> "JS_SYNTAX_ERROR"
        error.message.orEmpty().contains("Source did not return an audio URL", true) -> "NO_AUDIO_URL"
        error.message.orEmpty().contains("Script did not initialize", true) -> "NO_INIT_EVENT"
        error.message.orEmpty().contains("Script has no musicUrl", true) -> "NO_SUPPORTED_PLATFORM"
        error is QuickJsException -> "JS_ERROR"
        else -> error.javaClass.simpleName
    }

    @Test fun initialization() = runBlocking {
        for (file in samples()) {
            hostStatuses.clear()
            val started = System.nanoTime()
            val report = buildJsonObject {
                put("sample", file.name)
                try {
                    // Opaque input to the production runtime; no source, script logs, URLs or raw exceptions leave this process.
                    val code = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
                    runtime.use(metadata(file, code), code) {
                        put("status", "INIT_OK")
                        put("platforms", buildJsonObject {
                            sources.toSortedMap().forEach { (platform, capability) ->
                                put(platform, JsonArray(capability.qualitys.filter { it in listOf("128k", "320k", "flac", "flac24bit") }.map(::JsonPrimitive)))
                            }
                        })
                    }
                } catch (c: CancellationException) {
                    if (c !is TimeoutCancellationException) throw c
                    put("status", "INIT_FAILED"); put("reason", failure(c))
                } catch (error: Exception) {
                    put("status", "INIT_FAILED"); put("reason", failure(error))
                    if (error is QuickJsException) {
                        error.lineNumber?.let { put("scriptLine", it) }
                        error.columnNumber?.let { put("scriptColumn", it) }
                    }
                }
                put("scriptHttpStatuses", JsonArray(hostStatuses.toList().map(::JsonPrimitive)))
                put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
            }
            println("LX_BLACKBOX $report")
        }
    }
    // Metadata was verified using the anonymous, public NetEase song-detail endpoint.
    private val publicTracks = listOf(
        MusicTrack(TrackId("netease", "347230"), "海阔天空", listOf(MusicArtist("Beyond")), MusicAlbum("海阔天空"), 326_000),
        MusicTrack(TrackId("netease", "186016"), "晴天", listOf(MusicArtist("周杰伦")), MusicAlbum("叶惠美"), 269_000),
    )

    private fun comparisonTrack(): MusicTrack = publicTracks.single {
        it.id.value == (System.getenv("MEI_LX_TRACK_ID") ?: "347230")
    }

    @Test fun publicTrackUrls() = runBlocking {
        val tracks = publicTracks
        val qq = QqMusicProvider()
        val kuwo = KuwoMusicProvider()
        val migu = MiguMusicProvider()
        val catalog = LxCatalog(qq, kuwo, migu)
        val probeEvents = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val probe = AudioUrlProbe { probeEvents.add(it) }
        try {
            for (file in samples()) {
                val code = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
                for (track in tracks) {
                    val report = buildJsonObject {
                        put("sample", file.name); put("track", track.title); put("stage", "URL")
                        val started = System.nanoTime()
                        hostStatuses.clear()
                        try {
                            runtime.use(metadata(file, code), code) {
                                val source = if ("wy" in sources) "wy" else sources.keys.firstOrNull { it in setOf("tx", "kw", "kg", "mg") }
                                    ?: error("No supported catalog")
                                put("platform", source)
                                val candidate = withTimeoutOrNull(7000) { catalog.candidates(track, source).firstOrNull() }
                                if (candidate == null) { put("status", "NO_CATALOG_MATCH"); return@use }
                                val quality = PreviewPolicy.qualities(System.getenv("MEI_LX_TARGET_QUALITY") ?: "exhigh", sources.getValue(source).qualitys).firstOrNull()
                                    ?: error("No supported quality")
                                put("quality", quality)
                                val url = musicUrl(candidate, quality)
                                put("urlReturned", true)
                                probeEvents.clear()
                                val duration = probe.durationMs(url)
                                put("probeEvents", JsonArray(probeEvents.toList().distinct().map(::JsonPrimitive)))
                                put("responseCheck", inspectAudioResponse(url))
                                if (duration != null) put("audioDurationMs", duration)
                                put("status", when {
                                    duration == null -> "DURATION_UNVERIFIED"
                                    PreviewPolicy.isPreview(duration) -> "PREVIEW_REJECTED"
                                    kotlin.math.abs(track.durationMs - duration) > 15_000 -> "DURATION_MISMATCH"
                                    else -> "AUDIO_VALIDATED"
                                })
                            }
                        } catch (c: CancellationException) {
                            if (c !is TimeoutCancellationException) throw c
                            put("status", "FAILED"); put("reason", failure(c))
                        } catch (error: Exception) {
                            put("status", "FAILED"); put("reason", failure(error))
                        }
                        put("scriptHttpStatuses", JsonArray(hostStatuses.toList().map(::JsonPrimitive)))
                        put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
                    }
                    println("LX_BLACKBOX $report")
                }
            }
        } finally { qq.close(); kuwo.close(); migu.close() }
    }


    @Test fun otherPlatforms() = runBlocking {
        val target = comparisonTrack()
        val qq = QqMusicProvider()
        val kuwo = KuwoMusicProvider()
        val migu = MiguMusicProvider()
        val catalog = LxCatalog(qq, kuwo, migu)
        val events = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val probe = AudioUrlProbe { events.add(it) }
        try {
            for (file in samples()) {
                val code = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
                for (platform in listOf("kw", "tx", "kg", "mg")) {
                    val report = buildJsonObject {
                        put("sample", file.name); put("track", target.title); put("platform", platform)
                        val started = System.nanoTime()
                        hostStatuses.clear()
                        try {
                            runtime.use(metadata(file, code), code) {
                                val capability = sources[platform]
                                if (capability == null) { put("status", "UNSUPPORTED_PLATFORM"); return@use }
                                val candidate = withTimeoutOrNull(7_000) { catalog.candidates(target, platform).firstOrNull() }
                                if (candidate == null) { put("status", "NO_CATALOG_MATCH"); return@use }
                                val quality = PreviewPolicy.qualities(System.getenv("MEI_LX_TARGET_QUALITY") ?: "exhigh", capability.qualitys).firstOrNull()
                                    ?: error("No supported quality")
                                put("quality", quality)
                                val url = musicUrl(candidate, quality)
                                put("urlReturned", true)
                                events.clear()
                                val duration = probe.durationMs(url)
                                put("probeEvents", JsonArray(events.toList().distinct().map(::JsonPrimitive)))
                                if (duration == null) put("responseCheck", inspectAudioResponse(url)) else put("audioDurationMs", duration)
                                put("status", when {
                                    duration == null -> "DURATION_UNVERIFIED"
                                    PreviewPolicy.isPreview(duration) -> "PREVIEW_REJECTED"
                                    kotlin.math.abs(target.durationMs - duration) > 15_000 -> "DURATION_MISMATCH"
                                    else -> "AUDIO_VALIDATED"
                                })
                            }
                        } catch (c: CancellationException) {
                            if (c !is TimeoutCancellationException) throw c
                            put("status", "FAILED"); put("reason", failure(c))
                        } catch (error: Exception) { put("status", "FAILED"); put("reason", failure(error)) }
                        put("scriptHttpStatuses", JsonArray(hostStatuses.toList().map(::JsonPrimitive)))
                        put("elapsedMs", (System.nanoTime() - started) / 1_000_000)
                    }
                    println("LX_BLACKBOX $report")
                }
            }
        } finally { qq.close(); kuwo.close(); migu.close() }
    }


    @Test fun lowerQualityAndPlaybackHeaders() = runBlocking {
        val target = comparisonTrack()
        val qq = QqMusicProvider()
        val kuwo = KuwoMusicProvider()
        val migu = MiguMusicProvider()
        val catalog = LxCatalog(qq, kuwo, migu)
        try {
            for (file in samples()) {
                val code = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
                val caseFilter = System.getenv("MEI_LX_HEADER_CASE")
                for ((platform, quality) in listOf("wy" to "320k", "wy" to "128k", "tx" to "128k").filter {
                    caseFilter.isNullOrBlank() || "${it.first}:${it.second}" == caseFilter
                }) {
                    val report = buildJsonObject {
                        put("sample", file.name); put("track", target.title); put("platform", platform); put("quality", quality)
                        try {
                            hostStatuses.clear()
                            runtime.use(metadata(file, code), code) {
                                if (quality !in sources[platform]?.qualitys.orEmpty()) {
                                    put("status", "UNSUPPORTED_QUALITY"); return@use
                                }
                                val candidate = withTimeoutOrNull(7000) { catalog.candidates(target, platform).firstOrNull() }
                                if (candidate == null) { put("status", "NO_CATALOG_MATCH"); return@use }
                                val url = musicUrl(candidate, quality)
                                val parsed = url.toHttpUrl()
                                put("urlScheme", parsed.scheme)
                                put("hostClass", when {
                                    parsed.host.endsWith("music.163.com") || parsed.host.endsWith("music.126.net") -> "NETEASE"
                                    parsed.host.endsWith("qq.com") || parsed.host.endsWith("qqmusic.qq.com") -> "QQ"
                                    else -> "CUSTOM"
                                })
                                put("containsUndefined", url.contains("undefined"))
                                put("defaultHeaders", inspectAudioResponse(url))
                                put("playbackHeaders", inspectAudioResponse(url, playbackHeaders = true))
                                if (platform == "wy" && quality == "128k" && parsed.scheme == "http" && parsed.port == 80) {
                                    val secureUrl = parsed.newBuilder().scheme("https").port(443).build().toString()
                                    put("httpsCheck", inspectAudioResponse(secureUrl, playbackHeaders = true))
                                }
                                put("status", "HEADERS_COMPARED")
                            }
                        } catch (c: CancellationException) {
                            if (c !is TimeoutCancellationException) throw c
                            put("status", "FAILED"); put("reason", failure(c))
                        } catch (error: Exception) { put("status", "FAILED"); put("reason", failure(error)) }
                        put("scriptHttpStatuses", JsonArray(hostStatuses.toList().map(::JsonPrimitive)))
                    }
                    println("LX_BLACKBOX $report")
                }
            }
        } finally { qq.close(); kuwo.close(); migu.close() }
    }

    private suspend fun inspectAudioResponse(url: String, playbackHeaders: Boolean = false): JsonObject = withContext(Dispatchers.IO) {
        val client = okhttp3.OkHttpClient.Builder().connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS).callTimeout(4, java.util.concurrent.TimeUnit.SECONDS).build()
        buildJsonObject {
            try {
                client.newCall(okhttp3.Request.Builder().url(url).header("Range", "bytes=0-63")
                    .header("Accept-Encoding", "identity").apply {
                        if (playbackHeaders) header("User-Agent", com.ljyh.mei.constants.UserAgent)
                    }.build()).execute().use { response ->
                    put("httpStatus", response.code)
                    put("bodyLength", response.body.contentLength())
                    response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()?.let { put("totalBytes", it) }
                    val prefix = response.body.byteStream().readLimited(64)
                    val type = response.header("Content-Type").orEmpty().lowercase()
                    put("contentType", when {
                        type.startsWith("audio/") -> "AUDIO"
                        type.startsWith("video/") -> "VIDEO"
                        type.contains("json") -> "JSON"
                        type.contains("html") -> "HTML"
                        else -> "OTHER"
                    })
                    put("signature", when {
                        prefix.take(3).toByteArray().toString(Charsets.US_ASCII) == "ID3" -> "MP3_ID3"
                        prefix.size >= 2 && prefix[0].toInt() and 255 == 255 && prefix[1].toInt() and 224 == 224 -> "MP3_FRAME"
                        prefix.take(4).toByteArray().toString(Charsets.US_ASCII) == "fLaC" -> "FLAC"
                        prefix.size > 8 && prefix.sliceArray(4..7).toString(Charsets.US_ASCII) == "ftyp" -> "MP4"
                        prefix.take(4).toByteArray().toString(Charsets.US_ASCII) == "OggS" -> "OGG"
                        else -> "UNKNOWN"
                    })
                }
            } catch (error: Exception) { put("networkFailure", error.javaClass.simpleName) }
        }
    }

}
