package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: List<String>,
    val album: String,
    val cover: String,
    val duration: Long,
    val path: String? = null,
    val sourceType: SourceType = SourceType.STREAM,
    val fileHash: String? = null,
    val fileSize: Long = 0,
    val fileFormat: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val folderPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
