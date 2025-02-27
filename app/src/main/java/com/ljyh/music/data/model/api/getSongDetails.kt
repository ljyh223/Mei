package com.ljyh.music.data.model.api
import com.google.gson.Gson

data class GetSongDetails(
    var c: String
){
    init {
        c = Gson().toJson(c.split(",").map { mapOf("id" to it) })
    }
}
