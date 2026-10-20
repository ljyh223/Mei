package com.ljyh.mei.di

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        try {
            Json.decodeFromString<List<String>>(value)
        } catch (_: Exception) { emptyList() }
}
