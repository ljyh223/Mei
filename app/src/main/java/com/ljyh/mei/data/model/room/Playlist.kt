package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey val id: String,
    val title:String,
    val cover:String,
    val author:String,
    val count:Int
)