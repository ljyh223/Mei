package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "like")
data class Like(
    @PrimaryKey val id: String,
)

