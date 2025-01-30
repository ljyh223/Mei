package com.ljyh.music.data.model.room

import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey

@Entity(tableName = "color")
data class Color(
    @PrimaryKey val url: String,
    val color: Int,
)

