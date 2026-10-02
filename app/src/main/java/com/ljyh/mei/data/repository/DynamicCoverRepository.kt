package com.ljyh.mei.data.repository

import android.content.Context
import android.util.Base64
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.constants.AppleMusicWebTokenKey
import com.ljyh.mei.constants.AppleMusicWebTokenRefreshAtKey
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.playback.CacheManager
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CoverPalette(
    val bgColor: String?,
    val textColor1: String?,
    val textColor2: String?,
    val textColor3: String?,
    val textColor4: String?
)

data class MotionArtworkClip(
    val url: String,
    val palette: CoverPalette?,
    val previewUrl: String?
)

data class AppleAlbumCandidate(
    val id: String,
    val name: String,
    val artist: String,
    val artworkUrl: String? = null
)

data class AppleMusicCoverDiagnostic(
    val query: String,
    val candidates: List<AppleAlbumCandidate>,
    val match: AppleAlbumCandidate?,
    val square: MotionArtworkClip?,
    val portrait: MotionArtworkClip?
)

private fun normalized(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
    .lowercase().replace(Regex("[\\p{P}\\p{Z}\\p{S}]"), "")

internal fun formatAppleArtworkUrl(url: String?, size: Int): String? = url
    ?.takeIf { it.startsWith("https://") }
    ?.replace("{w}", size.toString())
    ?.replace("{h}", size.toString())
    ?.replace("{f}", "jpg")

internal fun matchAppleAlbum(
    candidates: List<AppleAlbumCandidate>, album: String, artist: String
): AppleAlbumCandidate? {
    val expectedAlbum = normalized(album)
    val expectedArtist = normalized(artist)
    if (expectedAlbum.isBlank() || expectedArtist.isBlank()) return null
    return candidates.firstOrNull {
        normalized(it.name) == expectedAlbum && normalized(it.artist).contains(expectedArtist)
    }
}

data class DynamicCover(
    val songId: Long,
    val url: String,
    val source: Source,
    val cacheKey: String,
    val palette: CoverPalette? = null,
    val previewUrl: String? = null,
    val portraitClip: MotionArtworkClip? = null
) {
    enum class Source { APPLE_MUSIC, NETEASE }

    fun portraitVariant(): DynamicCover? = portraitClip?.let {
        copy(url = it.url, cacheKey = "$cacheKey:portrait", palette = it.palette,
            previewUrl = it.previewUrl, portraitClip = null)
    }
}

/** Decorative data: any network, parsing or matching failure falls back to the next source. */
@Singleton
class DynamicCoverRepository @Inject constructor(
    private val api: ApiService,
    @param:ApplicationContext private val context: Context
) {
    private val webClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    private val tokenMutex = Mutex()
    private var webToken: String? = null
    private var webTokenUntil = 0L
    private val memory = mutableMapOf<Long, Pair<Long, DynamicCover?>>()
    private val neteaseMemory = mutableMapOf<Long, Pair<Long, DynamicCover>>()

    /** Called at app start when the user has enabled motion artwork. */
    suspend fun warmWebToken() = withContext(Dispatchers.IO) {
        optional { getWebToken() }
        Unit
    }

    suspend fun resolve(song: MediaMetadata): DynamicCover? = withContext(Dispatchers.IO) {
        if (!song.coverUrl.startsWith("http")) return@withContext null
        val now = System.currentTimeMillis()
        synchronized(memory) {
            memory[song.id]?.takeIf { it.first > now }
        }?.let { return@withContext it.second }

        val result = optional { appleCover(song) } ?: optional { neteaseCover(song) }
        synchronized(memory) {
            val validUntil = when (result?.source) {
                DynamicCover.Source.NETEASE -> {
                    neteaseCacheUntil(result.url, now)
                }
                DynamicCover.Source.APPLE_MUSIC -> now + 60 * 60_000L
                null -> now + 20 * 60_000L
            }
            memory[song.id] = validUntil to result
        }
        result
    }

    suspend fun neteaseFallback(song: MediaMetadata): DynamicCover? = withContext(Dispatchers.IO) {
        optional { neteaseCover(song.id) }
    }

    suspend fun prefetchNetease(songId: Long) = withContext(Dispatchers.IO) {
        optional { neteaseCover(songId)?.let { prefetch(it) } }
    }

    @OptIn(UnstableApi::class)
    suspend fun prefetch(cover: DynamicCover) = withContext(Dispatchers.IO) {
        if (cover.url.contains(".m3u8", ignoreCase = true)) {
            // Warm the manifest and first media segment; segment URLs are the cache keys used by HLS playback.
            optional {
                val master = String(getWebBytes(cover.url, 1024 * 1024))
                val variantUrl = if (master.contains("#EXT-X-STREAM-INF")) {
                    val variant = master.lineSequence().map(String::trim)
                        .firstOrNull { it.isNotEmpty() && !it.startsWith('#') } ?: return@optional
                    cover.url.toHttpUrl().resolve(variant)?.toString() ?: return@optional
                } else cover.url
                val playlist = if (variantUrl == cover.url) master else String(getWebBytes(variantUrl, 1024 * 1024))
                optional { cachePart(cover.url, null, null) }
                optional { cachePart(variantUrl, null, null) }
                val initPath = Regex("#EXT-X-MAP:[^\\n]*URI=\"([^\"]+)\"")
                    .find(playlist)?.groupValues?.getOrNull(1)
                initPath?.let { path ->
                    variantUrl.toHttpUrl().resolve(path)?.toString()?.let { url ->
                        optional { cachePart(url, null, null) }
                    }
                }
                val segment = playlist.lineSequence().map(String::trim)
                    .firstOrNull { it.isNotEmpty() && !it.startsWith('#') } ?: return@optional
                val segmentUrl = variantUrl.toHttpUrl().resolve(segment)?.toString() ?: return@optional
                optional { cachePart(segmentUrl, null, 2L * 1024 * 1024) }
            }
        } else {
            optional { cachePart(cover.url, cover.cacheKey, 2L * 1024 * 1024) }
        }
    }

    @OptIn(UnstableApi::class)
    private fun cachePart(url: String, key: String?, bytes: Long?) {
        val spec = DataSpec.Builder().setUri(url).apply {
            if (bytes != null) setLength(bytes)
            if (key != null) setKey(key)
        }.build()
        CacheWriter(CacheManager.getCacheDataSourceFactory(context).createDataSource(), spec, null, null).cache()
    }

    private suspend fun neteaseCover(song: MediaMetadata): DynamicCover? = neteaseCover(song.id)

    private suspend fun neteaseCover(songId: Long): DynamicCover? {
        val now = System.currentTimeMillis()
        synchronized(neteaseMemory) {
            neteaseMemory[songId]?.takeIf { it.first > now }
        }?.let { return it.second }
        val url = api.getDynamicCover(mapOf("songId" to songId))
            .takeIf { it.code == 200 }?.data?.videoPlayUrl
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: return null
        val validUntil = neteaseCacheUntil(url, now)
        if (validUntil <= now) return null
        val cover = DynamicCover(songId, url, DynamicCover.Source.NETEASE, "dynamic:netease:$songId")
        synchronized(neteaseMemory) {
            if (neteaseMemory.size >= 256) neteaseMemory.clear()
            neteaseMemory[songId] = validUntil to cover
        }
        return cover
    }

    private fun neteaseCacheUntil(url: String, now: Long): Long {
        val signedExpiry = url.toHttpUrlOrNull()
            ?.queryParameter("wsTime")?.toLongOrNull()?.times(1000L)
            ?.minus(60_000L) ?: Long.MAX_VALUE
        return minOf(now + 10 * 60_000L, signedExpiry).coerceAtLeast(now)
    }

    suspend fun inspectAppleMusic(album: String, artist: String): AppleMusicCoverDiagnostic =
        withContext(Dispatchers.IO) { appleLookup(album.trim(), artist.trim()) }

    private suspend fun appleCover(song: MediaMetadata): DynamicCover? {
        val result = appleLookup(song.album.title, song.artists.firstOrNull()?.name.orEmpty())
        val id = result.match?.id ?: return null
        val primary = result.square ?: result.portrait ?: return null
        return DynamicCover(song.id, primary.url, DynamicCover.Source.APPLE_MUSIC,
            "dynamic:apple:$id", primary.palette, primary.previewUrl, result.portrait)
    }

    private suspend fun appleLookup(album: String, artist: String): AppleMusicCoverDiagnostic {
        val query = "$album $artist".trim()
        if (album.isBlank() || artist.isBlank()) {
            return AppleMusicCoverDiagnostic(query, emptyList(), null, null, null)
        }
        // This is also the production matching path, so diagnostics reflect playback decisions.
        val searchUrl = "https://amp-api.music.apple.com/v1/catalog/cn/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("types", "albums")
            .addQueryParameter("limit", "10")
            .addQueryParameter("l", "zh-Hans-CN")
            .build()
        val albums = appleGet(searchUrl.toString()).optJSONObject("results")
            ?.optJSONObject("albums")?.optJSONArray("data")
        val candidates = buildList {
            if (albums != null) for (i in 0 until albums.length()) {
                val item = albums.optJSONObject(i) ?: continue
                val attrs = item.optJSONObject("attributes") ?: continue
                val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                add(AppleAlbumCandidate(
                    id = id,
                    name = attrs.optString("name"),
                    artist = attrs.optString("artistName"),
                    artworkUrl = formatAppleArtworkUrl(
                        attrs.optJSONObject("artwork")?.optString("url"), 240
                    )
                ))
            }
        }
        val match = matchAppleAlbum(candidates, album, artist)
            ?: return AppleMusicCoverDiagnostic(query, candidates, null, null, null)
        val id = match.id
        val motionUrl = "https://amp-api.music.apple.com/v1/catalog/cn/albums/$id".toHttpUrl()
            .newBuilder().addQueryParameter("extend", "editorialVideo")
            .addQueryParameter("l", "zh-Hans-CN").build()
        val video = appleGet(motionUrl.toString()).optJSONArray("data")
            ?.optJSONObject(0)?.optJSONObject("attributes")?.optJSONObject("editorialVideo")
        val square = parseMotionClip(video?.optJSONObject("motionDetailSquare"))
            ?: parseMotionClip(video?.optJSONObject("motionSquareVideo1x1"))
        val portrait = parseMotionClip(video?.optJSONObject("motionDetailTall"))
            ?: parseMotionClip(video?.optJSONObject("motionTallVideo3x4"))
        return AppleMusicCoverDiagnostic(query, candidates, match, square, portrait)
    }

    private fun parseMotionClip(clip: JSONObject?): MotionArtworkClip? {
        val url = clip?.optString("video")?.takeIf { it.startsWith("https://") } ?: return null
        val artwork = clip.optJSONObject("previewFrame")
        val palette = artwork?.let {
            CoverPalette(it.optString("bgColor"), it.optString("textColor1"),
                it.optString("textColor2"), it.optString("textColor3"), it.optString("textColor4"))
        }
        val previewUrl = formatAppleArtworkUrl(artwork?.optString("url"), 1000)
        return MotionArtworkClip(url, palette, previewUrl)
    }

    private suspend fun appleGet(url: String): JSONObject {
        for (attempt in 0..1) {
            val token = getWebToken()
            val request = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .header("Origin", "https://music.apple.com")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            webClient.newCall(request).execute().use { response ->
                if (response.code == 401 && attempt == 0) {
                    tokenMutex.withLock {
                        webToken = null
                        webTokenUntil = 0L
                        context.dataStore.edit {
                            it.remove(AppleMusicWebTokenKey)
                            it.remove(AppleMusicWebTokenRefreshAtKey)
                        }
                    }
                } else {
                    if (!response.isSuccessful) throw IOException("Apple catalog: HTTP ${response.code}")
                    return JSONObject(String(readLimited(response.body.byteStream(), 2 * 1024 * 1024)))
                }
            }
        }
        throw IOException("Apple catalog authorization failed")
    }

    private suspend fun getWebToken(): String = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        webToken?.takeIf { webTokenUntil > now }?.let { return@withLock it }
        val stored = context.dataStore.data.first()
        val savedToken = stored[AppleMusicWebTokenKey]
        val savedRefreshAt = stored[AppleMusicWebTokenRefreshAtKey] ?: 0L
        val savedExpiry = savedToken?.let { runCatching { tokenExpiryMillis(it) }.getOrNull() } ?: 0L
        if (savedToken != null && savedExpiry > now && savedRefreshAt > now) {
            webToken = savedToken
            webTokenUntil = savedRefreshAt
            return@withLock savedToken
        }
        try {
            val html = String(getWebBytes("https://music.apple.com", 16 * 1024 * 1024))
            val jsPath = Regex("/assets/index~[^\\\"']+\\.js").find(html)?.value
                ?: throw IOException("Apple web player script not found")
            val js = String(getWebBytes("https://music.apple.com$jsPath", 64 * 1024 * 1024))
            val token = Regex("eyJ[A-Za-z0-9_=-]+\\.eyJ[A-Za-z0-9_=-]+\\.[A-Za-z0-9_=-]+")
                .find(js)?.value ?: throw IOException("Apple web token not found")
            val expiryMillis = tokenExpiryMillis(token)
            if (expiryMillis <= now) throw IOException("Apple web token expired")
            val refreshAt = now + (expiryMillis - now) / 2
            context.dataStore.edit {
                it[AppleMusicWebTokenKey] = token
                it[AppleMusicWebTokenRefreshAtKey] = refreshAt
            }
            webToken = token
            webTokenUntil = refreshAt
            token
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // A token past its refresh point is still usable until exp if renewal is offline.
            if (savedToken == null || savedExpiry <= now) throw error
            webToken = savedToken
            webTokenUntil = minOf(savedExpiry, now + 6 * 60 * 60_000L)
            savedToken
        }
    }

    private fun tokenExpiryMillis(token: String): Long {
        val payload = token.split('.').getOrNull(1) ?: throw IOException("Invalid Apple web token")
        val claims = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
        return claims.optLong("exp") * 1000L
    }

    private fun getWebBytes(url: String, limit: Int): ByteArray {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        webClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Apple web player: HTTP ${response.code}")
            return readLimited(response.body.byteStream(), limit)
        }
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray = input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (out.size() + count > limit) throw IOException("Apple response too large")
            out.write(buffer, 0, count)
        }
        out.toByteArray()
    }

    private suspend inline fun <T> optional(block: () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
