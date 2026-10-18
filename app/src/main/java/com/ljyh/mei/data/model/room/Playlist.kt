package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.exp
import kotlin.math.ln1p

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey val id: String,
    val title: String,
    val cover: String,
    val author: String,
    val authorName: String,
    val authorAvatar: String,
    val count: Int,
    val type: PlaylistType = PlaylistType.NETEAST,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val playCount: Long = 0L,
    val lastPlayTime: Long = 0L,
    val localPlayCount: Int = 0
) {
    fun sortScore(
        maxLocalPlayCount: Int = 1,
        now: Long = System.currentTimeMillis()
    ): Double {
        val daysSinceLastPlay = if (lastPlayTime > 0L) {
            ((now - lastPlayTime).coerceAtLeast(0L)) / (1000.0 * 60 * 60 * 24)
        } else Double.MAX_VALUE

        val recent = exp(-daysSinceLastPlay / 30.0)
        val frequency = ln1p(localPlayCount.coerceAtLeast(0).toDouble()) /
            ln1p(maxLocalPlayCount.coerceAtLeast(1).toDouble())

        return 50.0 * recent + 50.0 * frequency
    }
}

fun List<Playlist>.sortedBySmartScore(now: Long): List<Playlist> {
    val maxLocalPlayCount = maxOfOrNull { it.localPlayCount } ?: 0
    return sortedWith(
        compareByDescending<Playlist> { it.localPlayCount > 0 && it.lastPlayTime > 0L }
            .thenByDescending { it.sortScore(maxLocalPlayCount, now) }
            .thenByDescending {
                if (it.localPlayCount > 0 && it.lastPlayTime > 0L) 0L else it.playCount
            }
            .thenBy { it.id }
    )
}
