package com.ljyh.mei.ui.component.artwork.album

import java.time.Instant
import java.time.ZoneId

internal enum class AlbumMediaStyle { Plain, Vinyl, Cassette, CD }
internal data class AlbumPackageLabel(
    val title: String,
    val artist: String,
    val year: Int?,
    val tracks: List<AlbumPackageTrack> = emptyList(),
    val trackTotal: Int = tracks.size,
    val page: Int = 0,
    val pageCount: Int = 1,
    val rowsPerPage: Int = 8,
)

/** NetEase dates use epoch milliseconds; pre-1970 releases are valid negative timestamps. */
internal fun albumReleaseYear(publishTime: Long?): Int? {
    if (publishTime == null || publishTime == 0L) return null
    return runCatching {
        Instant.ofEpochMilli(publishTime).atZone(ZoneId.of("Asia/Shanghai")).year
    }.getOrNull()?.takeIf { it in 1..9999 }
}

internal fun albumMediaStyle(publishTime: Long?): AlbumMediaStyle = when (albumReleaseYear(publishTime)) {
    null -> AlbumMediaStyle.Plain
    in 1..1979 -> AlbumMediaStyle.Vinyl
    in 1980..1989 -> AlbumMediaStyle.Cassette
    else -> AlbumMediaStyle.CD
}
