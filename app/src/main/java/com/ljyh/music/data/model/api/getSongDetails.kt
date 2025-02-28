package com.ljyh.music.data.model.api
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class GetSongDetails(
    @SerializedName("c")
    var c: String
){
    init {
        c = Gson().toJson(c.split(",").map { mapOf("id" to it) })
    }
}
