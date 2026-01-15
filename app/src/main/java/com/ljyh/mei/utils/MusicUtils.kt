package com.ljyh.mei.utils

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.data.model.room.Song
import timber.log.Timber

object MusicUtils {
    // 扫描 Music文件夹下的.json文件，反序列化，获取歌曲信息，保存到数据库中
    fun getLocalMusic(): List<Song> {
        val relativePath: String = Environment.DIRECTORY_MUSIC
        val downloadsDir = Environment.getExternalStoragePublicDirectory(relativePath)
        Timber.tag("MusicUtils").d("getLocalMusic: $downloadsDir")
        val files = downloadsDir.listFiles() ?: return listOf()
        Timber.tag("MusicUtils").d("getLocalMusic: ${files.size}")
        val songs = mutableListOf<Song>()
        for (file in files) {
            Timber.tag("MusicUtils").d("getLocalMusic: ${file.name}")
            if (file.name.endsWith(".json")) {
                Timber.tag("MusicUtils").d("getLocalMusic: ${file.name}")
                val json = file.readText()
                try {
                    val simplePlaylist = Gson().fromJson(json, SimplePlaylist::class.java)
                    songs.addAll(simplePlaylist.toSongDB())
                }catch (e: Exception){
                    Timber.tag("MusicUtils").d("getLocalMusic: ${e.message}")
                    continue
                }



            }
        }
        return songs
    }
}