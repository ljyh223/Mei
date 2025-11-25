package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "color")
data class CacheColor(
    @PrimaryKey val url: String,
    val color: Int,
)

