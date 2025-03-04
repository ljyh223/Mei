package com.ljyh.music.data.model.room

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ljyh.music.data.model.SongEntity

@Entity(tableName = "song")
data class Song(
    @PrimaryKey val id: String,
    val title:String,
    val artist: String,
    val album: String,
    val cover: String,
    val duration: Int,
    val path:String,
    val lyric:String?

)

@Entity(tableName = "qqSong")
data class QQSong(
    @PrimaryKey val id: String,
    val title:String,
    val artist: String,
    val album: String,
    val duration: Int,
)