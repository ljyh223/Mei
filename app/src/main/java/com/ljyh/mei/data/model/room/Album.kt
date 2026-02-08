package com.ljyh.mei.data.model.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation


@Entity(
    tableName = "album_artist_cross_ref",
    primaryKeys = ["albumId", "artistId"],
    indices = [
        Index("albumId"),
        Index("artistId")
    ]
)
data class AlbumArtistCrossRef(
    val albumId: Long,
    val artistId: Long
)

data class AlbumWithArtists(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "artistId",
        associateBy = Junction(AlbumArtistCrossRef::class)
    )
    val artists: List<ArtistEntity>
)



@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val albumId: Long,
    val name: String,
    val cover: String,
    val publishTime:Long,
    val songCount: Int
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val artistId: Long,
    val name: String,
    val avatarUrl: String?
)
