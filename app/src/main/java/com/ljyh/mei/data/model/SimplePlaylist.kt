package com.ljyh.mei.data.model

import android.os.Environment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.utils.StringUtils.specialReplace

@Serializable

data class SimplePlaylist(
    val id: String,
    val name: String,
    val songs: ArrayList<Song>
) {

    @Serializable

    data class Song(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        @SerialName("pic_url")
        val picUrl: String,
        @SerialName("file_type")
        val fileType: String = "",
        var url: String = "",
        var lyric: String = ""
    )
}

@Serializable

data class TPlaylist(val id:String="",val name:String="")
