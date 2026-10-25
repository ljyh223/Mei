package com.ljyh.mei.data.model.domain

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.response.AlbumDetail
import com.ljyh.mei.data.model.response.PlaylistDetail
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.utils.netease.NeteaseUtils.getResourceLink
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MediaMetadata(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val artists: List<Artist>,
    val duration: Long,
    val album: Album,
    val explicit: Boolean = false,
    val tns:String?=null,
    // Local library IDs are not public catalogue IDs. Zero means no confirmed public ID;
    // a non-null sentinel also survives queue codecs that omit null properties.
    val neteaseId: Long = id.takeIf { it > 0 } ?: 0L,
) {
    @Serializable
    data class Artist(
        val id: Long,
        val name: String,
        val picUrl: String? = null,
        val alias: List<String>? = null
    )

    @Serializable
    data class Album(
        val id: Long,
        val title: String
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
            name = it.name?:"",
            alias = it.alias
        )
    },
    duration = dt,
    album = MediaMetadata.Album(
        id = al.Id,
        title = al.name?:"",
    ),
    tns = tns?.firstOrNull()
)



fun AlbumDetail.Song.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl= getResourceLink(album.pictureId.toString()),
    artists = artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name,
        )
    },
    duration = duration,
    album = MediaMetadata.Album(
        id = album.id,
        title = album.name
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
    tns = tns?.firstOrNull()
)




fun ArtistSong.HotSong.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl = album.picUrl.ifBlank { getResourceLink(album.pictureId.toString()) },
    artists = artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name,
            alias = it.aliases
        )
    },
    duration = duration,
    album = MediaMetadata.Album(
        id = album.id,
        title = album.name
    )
)


// Unavailable intelligence recommendations may have no songInfo; never enqueue invalid IDs.
fun com.ljyh.mei.data.model.api.Data.toMediaMetadata(): MediaMetadata? {
    val song = songInfo?.takeIf { it.id > 0 } ?: return null
    return MediaMetadata(
        id = song.id,
        title = song.name,
        coverUrl = song.al?.picUrl.orEmpty(),
        artists = song.ar.map {
            MediaMetadata.Artist(id = it.id, name = it.name.orEmpty(), alias = it.tns)
        },
        duration = song.dt,
        album = MediaMetadata.Album(id = song.al?.id ?: 0, title = song.al?.name.orEmpty()),
        tns = song.tns?.firstOrNull(),
    )
}

// For FM mode - converts Radio.Data to MediaMetadata
fun com.ljyh.mei.data.model.weapi.Data.toMediaMetadata() = MediaMetadata(
    id = id,
    title = name,
    coverUrl = album.picUrl,
    artists = artists.map {
        MediaMetadata.Artist(id = it.id.toLong(), name = it.name, alias = it.alias)
    },
    duration = duration.toLong(),
    album = MediaMetadata.Album(id = album.id.toLong(), title = album.name),
    tns = transNames?.firstOrNull()
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
            .setSubtitle(ar.joinToString { it.name?:"" })
            .setArtist(ar.joinToString { it.name?:"" })
            .setAlbumTitle(al.name)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setArtworkUri(al.picUrl.toUri())
            .setExtras(Bundle().apply {
                putLong("duration", this@toMediaItem.dt)
                putStringArrayList("artist_list", ArrayList(ar.map { it.name ?: "" }))
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


fun com.ljyh.mei.data.model.room.Song.toMediaMetadata(): MediaMetadata {
    val songId = id.removePrefix("local_").let { sid ->
        sid.toLongOrNull() ?: sid.hashCode().toLong().let { if (it < 0) -it else it }
    }
    return MediaMetadata(
        id = songId,
        title = title,
        coverUrl = cover,
        artists = artist.map { name ->
            MediaMetadata.Artist(id = name.hashCode().toLong().let { if (it < 0) -it else it }, name = name)
        },
        duration = duration * 1000,
        album = MediaMetadata.Album(id = album.hashCode().toLong().let { if (it < 0) -it else it }, title = album),
        neteaseId = if (sourceType == SourceType.LOCAL || id.startsWith("local_")) {
            matchedNeteaseId?.takeIf { it > 0 } ?: 0L
        } else id.toLongOrNull()?.takeIf { it > 0 } ?: 0L,
    )
}

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
