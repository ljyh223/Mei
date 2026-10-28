package com.ljyh.mei.utils.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.Extras
import coil3.Image
import coil3.ImageLoader
import coil3.getExtra
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val OriginalArtwork = Extras.Key(false)
private val PlayerArtwork = Extras.Key(false)

/** Explicit opt-in; ordinary image requests always use a bounded CDN thumbnail. */
fun ImageRequest.Builder.originalArtwork(url: String?): ImageRequest.Builder = apply {
    data(url?.originalArtworkUrl())
    extras[OriginalArtwork] = true
}

/** Full-player requests opt out of component-based thumbnail sizing. */
fun ImageRequest.Builder.playerArtwork(url: String?, original: Boolean): ImageRequest.Builder = apply {
    data(url?.playerArtworkUrl(original))
    extras[OriginalArtwork] = original
    extras[PlayerArtwork] = true
    if (!original) size(1600)
}

/** Runs before Coil's cache lookup, so the normalized URL is also the disk cache key. */
class ArtworkRequestInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val source = when (val data = request.data) {
            is String -> data
            is android.net.Uri -> data.toString()
            else -> return chain.proceed()
        }
        if (source.neteaseImageUrl() == null) return chain.proceed()
        val normalized = if (request.getExtra(OriginalArtwork)) {
            source.originalArtworkUrl()
        } else if (request.getExtra(PlayerArtwork)) {
            source.playerArtworkUrl(original = false)
        } else {
            val size = ArtworkSize.forBounds(
                (chain.size.width as? Dimension.Pixels)?.px,
                (chain.size.height as? Dimension.Pixels)?.px,
            )
            source.artworkImage(size)
        }
        return chain.withRequest(request.newBuilder().data(normalized).build()).proceed()
    }
}

private fun artworkCacheCandidates(url: String): List<String> {
    val canonical = listOf(url.playerArtworkUrl(original = false)) + ArtworkSize.entries.reversed().map { url.artworkImage(it) }
    val legacy = url.neteaseImageUrl()?.let { parsed ->
        listOf(1600, 500, 300, 100).map { side ->
            parsed.newBuilder().query(null).addQueryParameter("param", "${side}y${side}").build().toString()
        }
    }.orEmpty()
    return (canonical + legacy + url + url.originalArtworkUrl()).distinct()
}

private fun cachedArtworkKey(loader: ImageLoader, url: String): MemoryCache.Key? {
    val candidates = artworkCacheCandidates(url)
    val cache = loader.memoryCache ?: return null
    // Keep Coil's key extras (transformations, decode size) instead of guessing their structure.
    return cache.keys.filter { it.key in candidates }
        .maxByOrNull { cache[it]?.image?.let { image -> image.width.toLong() * image.height } ?: 0L }
}

/**
 * High-resolution artwork uses any existing memory/disk thumbnail as its placeholder. Cache misses never
 * start a thumbnail download. Disk lookup finishes before the original request is submitted, so
 * finding a placeholder cannot restart an in-flight original download.
 */
@Composable
fun rememberOriginalArtworkRequest(url: String?): ImageRequest? =
    rememberHighResolutionArtworkRequest(url, original = true, player = false)

@Composable
fun rememberPlayerArtworkRequest(url: String?, original: Boolean): ImageRequest? =
    rememberHighResolutionArtworkRequest(url, original, player = true)

@Composable
private fun rememberHighResolutionArtworkRequest(url: String?, original: Boolean, player: Boolean): ImageRequest? {
    val context = LocalContext.current
    val loader = context.imageLoader
    val targetUrl = if (player) url?.playerArtworkUrl(original) else url?.originalArtworkUrl()
    fun builder() = ImageRequest.Builder(context).apply {
        if (player) playerArtwork(url, original) else originalArtwork(url)
    }
    val initial = remember(context, loader, url, original, player) {
        url?.let { cachedArtworkKey(loader, it) }?.let { key ->
            builder().placeholderMemoryCacheKey(key)
                .error(loader.memoryCache?.get(key)?.image)
                .build()
        }
    }
    val request by produceState(initial, context, loader, url, original, player) {
        // produceState retains its previous value when keys change; never expose the old song.
        value = initial
        if (initial != null) return@produceState
        val placeholder = withContext(Dispatchers.IO) {
            var cached: Image? = null
            if (url != null) {
                for (candidate in artworkCacheCandidates(url)) {
                    val result = loader.execute(
                        ImageRequest.Builder(context).data(candidate)
                            .size(ArtworkSize.Detail.pixels)
                            // Explicit keys also allow existing pre-normalization disk entries.
                            .memoryCacheKey(candidate)
                            .diskCacheKey(candidate)
                            .memoryCachePolicy(CachePolicy.READ_ONLY)
                            .diskCachePolicy(CachePolicy.READ_ONLY)
                            .networkCachePolicy(CachePolicy.DISABLED)
                            .build(),
                    )
                    if (result is SuccessResult) {
                        cached = result.image
                        break
                    }
                }
            }
            cached
        }
        value = builder().placeholder(placeholder).error(placeholder).build()
    }
    // A new composition key must not show the request retained for the previous URL.
    return request?.takeIf { it.data == targetUrl }
}
