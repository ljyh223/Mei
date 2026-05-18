package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_lyric")
data class CachedLyric(
    @PrimaryKey val songId: String,
    val content: String,
    val translation: String?,
    val isVerbatim: Boolean,
    val isPureMusic: Boolean,
    val sourceName: String,
    val parserType: String,
    val aiProcessed: Boolean,
    val updatedAt: Long
)
