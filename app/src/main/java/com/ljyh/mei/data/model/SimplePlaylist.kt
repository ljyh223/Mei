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


        fun toSongDB(): com.ljyh.mei.data.model.room.Song {
            return Song(
                id = id,
                title = name,
                artist = artist,
                album = album,
                cover = picUrl,
                duration = 0,
                path="",
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


    fun toSongDB(): List<com.ljyh.mei.data.model.room.Song> {
        val relativePath=Environment.DIRECTORY_MUSIC
        val downloadsDir=Environment.getExternalStoragePublicDirectory(relativePath)
        return this.songs.map { s->
            Song(
                id = s.id,
                title = s.name,
                artist = s.artist,
                album =s. album,
                cover = s.picUrl,
                duration = 0,
                path= "$downloadsDir/${this.name}/${specialReplace(s.name)}.${s.fileType}"
            )
        }
    }
}


data class TPlaylist(val id:String="",val name:String="")
