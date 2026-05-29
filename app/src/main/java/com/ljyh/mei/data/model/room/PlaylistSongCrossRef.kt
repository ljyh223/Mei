package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
