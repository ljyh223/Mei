package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.ArtistEntity

data class Album(
    val id: Long,
    val title: String,
    val cover: String,
    val size: Int,
    var artist: List<Artist>
){
    data class Artist(
        val id: Long,
        val name: String
    )
}



fun UserAlbumList.Data.toAlbum(): Album {
    return Album(
        id = id,
        title = name,
        cover = picUrl,
        size = size,
        artist = artists.map {
            Album.Artist(
                id = it.id,
                name = it.name
            )
        })
}


fun UserAlbumList.Data.toAlbumEntity(): Pair<AlbumEntity, List<ArtistEntity>>{
    return AlbumEntity(
        albumId = id,
        name = name,
        cover = picUrl,
        publishTime = subTime,
        songCount = size
    ) to artists.map { ArtistEntity(
        artistId = id,
        name= name,
        avatarUrl = null
    ) }
}