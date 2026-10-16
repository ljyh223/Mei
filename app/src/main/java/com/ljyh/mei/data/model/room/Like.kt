package com.ljyh.mei.data.model.room

import androidx.room.Entity

@Entity(tableName = "like", primaryKeys = ["userId", "id"])
data class Like(
    val userId: String,
    val id: String,
)
