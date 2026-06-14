package com.ljyh.mei.data.model

import android.os.Environment
import com.google.gson.annotations.SerializedName
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.utils.StringUtils.specialReplace


data class SimplePlaylist(
    val id: String,
    val name: String,
    val songs: ArrayList<Song>
) {

    data class Song(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        @SerializedName("pic_url")
        val picUrl: String,
        @SerializedName("file_type")
        val fileType: String = "",
        var url: String = "",
        var lyric: String = ""
    )
}


data class TPlaylist(val id:String="",val name:String="")
