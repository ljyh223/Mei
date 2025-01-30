package com.ljyh.music.data.model

import com.google.gson.annotations.SerializedName
import com.ljyh.music.data.model.room.Song


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
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "id" to id,
                "name" to name,
                "artist" to artist,
                "album" to album,
                "pic_url" to picUrl,
                "file_type" to fileType
            )
        }


        fun toSongDB(): com.ljyh.music.data.model.room.Song {
            return Song(
                id = id,
                title = name,
                artist = artist,
                album = album,
                cover = picUrl,
                duration = 0,
                lyric = ""
            )
        }
    }
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "songs" to songs.map { it.toMap() }
        )
    }
}


data class TPlaylist(val id:String="",val name:String="")
