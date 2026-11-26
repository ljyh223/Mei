package com.ljyh.mei.data.model.room

import androidx.room.Entity

/** A missing row means unknown, rather than not subscribed. */
@Entity(tableName = "album_subscription", primaryKeys = ["userId", "albumId"])
data class AlbumSubscription(
    val userId: String,
    val albumId: Long,
    val subscribed: Boolean,
    val updatedAt: Long,
)
