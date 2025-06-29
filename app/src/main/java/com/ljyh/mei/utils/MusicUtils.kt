package com.ljyh.mei.utils

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.data.model.room.Song

object MusicUtils {
    // 扫描 Music文件夹下的.json文件，反序列化，获取歌曲信息，保存到数据库中
    fun getLocalMusic(): List<Song> {
        val relativePath: String = Environment.DIRECTORY_MUSIC
        val downloadsDir = Environment.getExternalStoragePublicDirectory(relativePath)
        Log.d("MusicUtils", "getLocalMusic: $downloadsDir")
        val files = downloadsDir.listFiles() ?: return listOf()
        Log.d("MusicUtils", "getLocalMusic: ${files.size}")
        val songs = mutableListOf<Song>()
        for (file in files) {
            Log.d("MusicUtils", "getLocalMusic: ${file.name}")
            if (file.name.endsWith(".json")) {
                Log.d("MusicUtils", "getLocalMusic: ${file.name}")
                val json = file.readText()
                try {
                    val simplePlaylist = Gson().fromJson(json, SimplePlaylist::class.java)
                    songs.addAll(simplePlaylist.toSongDB())
                }catch (e: Exception){
                    Log.d("MusicUtils", "getLocalMusic: ${e.message}")
                    continue
                }



            }
        }
        return songs
    }
}