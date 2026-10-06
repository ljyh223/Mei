package com.ljyh.mei.data.model.api
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetSongDetails(
    @SerialName("c")
    var c: String
){
    init {
        c = Json.encodeToString(c.split(",").map { mapOf("id" to it) })
    }
}
