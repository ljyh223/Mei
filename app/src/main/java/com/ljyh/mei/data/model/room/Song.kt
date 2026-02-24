package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String,
    val duration: Long,
    val path: String? = null
)


@Entity(tableName = "qqSong")
data class QQSong(
    @PrimaryKey val id: String,
    val qid: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
)