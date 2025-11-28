package com.ljyh.mei.data.model.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    // 设置外键：如果 Song 表里的这首歌被删了，历史记录也自动删除 (CASCADE)
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 加索引，查询速度更快
    indices = [Index(value = ["songId"])]
)
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0, // 自增ID，每一条历史记录都是独立的

    val songId: String,      // 关联 Song 表的 id

    val playedAt: Long       // 播放时间戳 (System.currentTimeMillis())
)

data class HistoryItem(
    @Embedded val song: Song, // 包含 Song 的所有字段
    val playedAt: Long        // 包含历史记录的时间
)