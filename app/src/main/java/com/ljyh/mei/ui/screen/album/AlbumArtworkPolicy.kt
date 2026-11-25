package com.ljyh.mei.ui.screen.album

import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.ui.component.artwork.album.AlbumMediaStyle
import com.ljyh.mei.ui.component.artwork.album.albumMediaStyle
import com.ljyh.mei.ui.component.artwork.album.albumReleaseYear

internal enum class AlbumArtworkKind { Model, Static, Motion }

/** Forced modes stay fixed; an unavailable video never prevents browsing the album. */
internal fun albumArtworkKind(mode: AlbumArtworkMode, publishTime: Long?, hasMotion: Boolean, modelMode: AlbumModelMode = AlbumModelMode.Auto, allowMotion: Boolean = true): AlbumArtworkKind = when (mode) {
    AlbumArtworkMode.Static -> AlbumArtworkKind.Static
    AlbumArtworkMode.Motion -> when {
        !allowMotion -> albumEraArtworkKind(publishTime)
        hasMotion && allowMotion -> AlbumArtworkKind.Motion
        else -> AlbumArtworkKind.Static
    }
    AlbumArtworkMode.Model -> if (albumArtworkModelStyle(mode, publishTime, modelMode) == AlbumMediaStyle.Plain) AlbumArtworkKind.Static else AlbumArtworkKind.Model
    AlbumArtworkMode.Auto -> when {
        hasMotion && allowMotion -> AlbumArtworkKind.Motion
        else -> albumEraArtworkKind(publishTime)
    }
}

private fun albumEraArtworkKind(publishTime: Long?): AlbumArtworkKind =
    if (albumReleaseYear(publishTime)?.let { it < 2010 } == true) AlbumArtworkKind.Model else AlbumArtworkKind.Static

/** Fixed models apply only inside Model mode; automatic artwork keeps its original era policy. */
internal fun albumArtworkModelStyle(
    mode: AlbumArtworkMode,
    publishTime: Long?,
    modelMode: AlbumModelMode,
): AlbumMediaStyle = if (mode != AlbumArtworkMode.Model) albumMediaStyle(publishTime) else when (modelMode) {
    AlbumModelMode.Auto -> albumMediaStyle(publishTime)
    AlbumModelMode.Vinyl -> AlbumMediaStyle.Vinyl
    AlbumModelMode.Cassette -> AlbumMediaStyle.Cassette
    AlbumModelMode.CD -> AlbumMediaStyle.CD
}
