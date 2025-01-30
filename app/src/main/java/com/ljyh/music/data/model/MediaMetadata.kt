package com.ljyh.music.data.model

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.util.UnstableApi


@Immutable
data class MediaMetadata(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val artists: List<Artist>,
    val duration: Int,
    val album: Album,
    val explicit: Boolean = false,
) {
    data class Artist(
        val id: Long,
        val name: String,
    )

    data class Album(
        val id: Long,
        val title: String,
    )

    fun toSongEntity() = SongEntity(
        id = id,
        title = title,
        duration = duration,
        albumId = album.id,
        albumName = album.title,
        artistId = artists.firstOrNull()?.id ?: 0,
        artistName = artists.firstOrNull()?.name ?: "",
        coverUrl = coverUrl
    )
}

fun PlaylistDetail.Playlist.Track.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl=al.picUrl,
    artists = ar.map {
        MediaMetadata.Artist(
            id = it.Id,
            name = it.name
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = al.Id,
        title = al.name
    )

)


fun PlaylistDetail.Playlist.Track.toMediaItem() =MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(id.toString())
    .setCustomCacheKey(id.toString())
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(name)
            .setSubtitle(ar.joinToString { it.name })
            .setArtist(ar.joinToString { it.name })
            .setAlbumTitle(al.name)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setArtworkUri(Uri.parse(al.picUrl))
            .build()
    )
    .build()


fun MediaMetadata.toMediaItem() = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(id.toString())
    .setCustomCacheKey(id.toString())
    .setTag(this)
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setAlbumTitle(album.title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setArtworkUri(Uri.parse(coverUrl))
            .build()
    )
    .build()
val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

data class SongEntity(
    val id: Long,
    val title: String,
    val duration: Int,
    val albumId: Long,
    val albumName: String,
    val artistId: Long ,
    val artistName: String,
    val coverUrl: String,
    val prepare: Boolean = false,
){
    fun toMediaItem() = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(id.toString())
        .setCustomCacheKey(id.toString())
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(artistName)
                .setArtist(artistName)
                .setAlbumTitle(albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setArtworkUri(Uri.parse(coverUrl))
                .build()
        )
        .build()
}