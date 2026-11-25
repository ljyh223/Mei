package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.domain.ArtistReleaseCategory
import com.ljyh.mei.data.network.Resource

/** Publish each page promptly; bounded expansion keeps recent singles from hiding older albums. */
internal suspend fun loadArtistAlbumPreview(
    loadPage: suspend (offset: Int, limit: Int) -> Resource<ArtistAlbum>,
    onPage: (ArtistAlbum) -> Unit,
): Resource<ArtistAlbum> {
    val releases = linkedMapOf<Long, ArtistAlbum.HotAlbum>()
    var offset = 0
    var latest: ArtistAlbum? = null
    repeat(4) {
        val result = loadPage(offset, 50)
        if (result !is Resource.Success || result.data.code != 200) {
            // Later failures preserve the usable preview; the full catalog can retry independently.
            return latest?.let { Resource.Success(it.copy(more = true)) }
                ?: if (result is Resource.Error) result else Resource.Error("发行作品暂未加载")
        }
        val page = result.data
        val before = releases.size
        page.hotAlbums.filter { it.id > 0 }.forEach { releases.putIfAbsent(it.id, it) }
        latest = page.copy(hotAlbums = releases.values.toList())
        onPage(checkNotNull(latest))
        val enoughAlbums = releases.values.count { ArtistReleaseCategory.Albums.matches(it.type) } >= 12
        val enoughSingles = releases.values.count { ArtistReleaseCategory.SinglesAndEps.matches(it.type) } >= 12
        if (!page.more || page.hotAlbums.isEmpty() || releases.size == before || (enoughAlbums && enoughSingles)) {
            return Resource.Success(checkNotNull(latest))
        }
        offset += page.hotAlbums.size
    }
    return Resource.Success(checkNotNull(latest))
}
