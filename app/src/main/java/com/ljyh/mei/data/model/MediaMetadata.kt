package com.ljyh.mei.data.model

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.utils.encrypt.getResourceLink
import androidx.core.net.toUri


@Immutable
data class MediaMetadata(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val artists: List<Artist>,
    val duration: Long,
    val album: Album,
    val explicit: Boolean = false,
    val tns:String?=null
) {
    data class Artist(
        val id: Long,
        val name: String,
        val picUrl: String? = null,
        val alias: List<String>? = null
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

const val PLACEHOLDER_URI = "https://placeholder.media"

fun PlaylistDetail.Playlist.Track.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl=al.picUrl,
    artists = ar.map {
        MediaMetadata.Artist(
            id = it.Id,
            name = it.name,
            alias = it.alias
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = al.Id,
        title = al.name,
    ),
    tns= tns?.get(0)
)



fun AlbumDetail.Song.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl= al.picStr,
    artists = ar.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name,
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = id,
        title = name
    )
)



fun EveryDaySongs.Data.DailySong.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl=al.picUrl,
    artists = ar.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name,
            alias = it.alias
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = al.id,
        title = al.name
    ),
    tns= tns?.get(0)
)




fun ArtistSong.HotSong.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl= getResourceLink(al.pic.toString()),
    artists = ar.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name,
            alias = it.alia
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = al.id,
        title = al.name
    )
)




@OptIn(UnstableApi::class)
    fun PlaylistDetail.Playlist.Track.toMediaItem() =MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(id.toString()) // 占位
    .setCustomCacheKey(id.toString())
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(name)
            .setSubtitle(ar.joinToString { it.name })
            .setArtist(ar.joinToString { it.name })
            .setAlbumTitle(al.name)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setArtworkUri(al.picUrl.toUri())
            .setExtras(Bundle().apply {
                putLong("duration", this@toMediaItem.dt.toLong())
            })
            .build()
    )
    .build()

@OptIn(UnstableApi::class)
fun createPlaceholder(id: String): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(PLACEHOLDER_URI) // 防止 NPE
        .setCustomCacheKey(id)   // 确保 ResolvingDataSource 能拿到 ID
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle("加载中...")
                .setArtist("")
                .build()
        )
        .build()
}


@OptIn(UnstableApi::class)
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
            .setArtworkUri(coverUrl.toUri())
            .build()
    )
    .build()



val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

data class SongEntity(
    val id: Long,
    val title: String,
    val duration: Long,
    val albumId: Long,
    val albumName: String,
    val artistId: Long ,
    val artistName: String,
    val coverUrl: String,
    val prepare: Boolean = false,
){
    @OptIn(UnstableApi::class)
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