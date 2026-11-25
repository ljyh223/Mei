package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.domain.ArtistReleaseCategory
import com.ljyh.mei.ui.model.Album

internal fun ArtistAlbum.HotAlbum.toArtistAlbum() = Album(
    id, name, picUrl, size, artists.map { Album.Artist(it.id, it.name) }, type, publishTime,
)

internal data class ArtistDiscography(
    val latest: Album?,
    val albums: List<Album>,
    val singles: List<Album>,
)

internal fun artistDiscography(releases: List<Album>): ArtistDiscography {
    // Unknown dates keep server order after dated releases, without inventing release dates.
    val ordered = releases.distinctBy { it.id }.sortedByDescending { it.publishTime.coerceAtLeast(0) }
    return ArtistDiscography(ordered.firstOrNull(),
        ordered.filter { ArtistReleaseCategory.Albums.matches(it.releaseType) },
        ordered.filter { ArtistReleaseCategory.SinglesAndEps.matches(it.releaseType) })
}
