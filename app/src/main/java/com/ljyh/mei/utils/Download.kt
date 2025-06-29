package com.ljyh.mei.utils


import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.utils.StringUtils.specialReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File


object DownloadManager {
    private const val TAG = "DownloadUtils"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient()
    private val relativePath: String = Environment.DIRECTORY_MUSIC
    fun downloadSongs(
        playlist: SimplePlaylist, onProgress: (Int, Int, Int) -> Unit, onComplete: () -> Unit
    ) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(relativePath)
        val saveInfo = File(downloadsDir, "${playlist.id}.json")
        if (!saveInfo.exists()) {
            saveInfo.createNewFile()
        }
        var mSimplePlaylist: SimplePlaylist
        saveInfo.readText().let {
            if (it == "") mSimplePlaylist = SimplePlaylist(
                playlist.id,
                playlist.name,
                ArrayList()
            )
            mSimplePlaylist = Gson().fromJson(it, SimplePlaylist::class.java)
        }

        //判断downloadDir 下是否有name
        val playlistDir = File(downloadsDir, playlist.name)
        if (!playlistDir.exists()) {
            playlistDir.mkdir()
        }

        var downloadedCount = 0
        var lose = 0
        scope.launch {
            playlist.songs.forEach { song ->
                try {
                    val suffix = song.url.substringBeforeLast("?").substringAfterLast(".")
                    val fileName = "${specialReplace("${song.name} - ${song.artist}")}.$suffix"
                    val songFile = File(playlistDir, fileName)
                    val status = downloadFile(song.url, songFile)
                    if (!status) lose++
                    if (songFile.exists()) {
                        when (suffix) {
                            "flac" -> SongMate.writeFALC(song, songFile.path)
                            "mp3" -> SongMate.writeMP3(song, songFile.path)
                            else -> {}
                        }
                    }
                    mSimplePlaylist.songs.add(song)
                    saveInfo.writeText(Gson().toJson(mSimplePlaylist.toMap()))
                    downloadedCount++
                    onProgress(downloadedCount, playlist.songs.size, lose)

                } catch (e: Exception) {
                    saveInfo.writeText(Gson().toJson(mSimplePlaylist))
                    Log.d(TAG, "Download failed: ${e.message}")
                }
            }
            onComplete()
        }
    }


    private suspend fun downloadFile(url: String, file: File): Boolean =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Downloading file from URL: $url")
            Log.d(TAG, "File path: ${file.absolutePath}")
            Log.d(TAG, file.path.substringBeforeLast("/"))
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                response.use {
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Download failed: ${response.code}")
                        return@withContext false
                    }


                    val inputStream = response.body?.byteStream()

                    inputStream?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                true
            } catch (e: IOException) {
                Log.e(TAG, "IOException while downloading file", e)
                false
            }
        }

    fun isExist(pid: String, name: String, id: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(relativePath)
        val playlistDir = File(downloadsDir, name)
        if (!playlistDir.exists()) return ""
        val saveInfo = File(downloadsDir, "${pid}.json")
        if (!saveInfo.exists()) {
            println("jsonFile not exist")
            return ""
        } else {
            val mSimplePlaylist: SimplePlaylist =
                Gson().fromJson(saveInfo.readText(), SimplePlaylist::class.java)
            val song = mSimplePlaylist.songs.find { it.id == id }
            if (song == null) {
                println("song is null")
                return ""
            }
            val name = specialReplace("${song.name} - ${song.artist}")
            val name1 = specialReplace(song.name)
            val name2=specialReplace("${song.artist} - ${song.name}")
            return playlistDir.listFiles()?.find {
                println(name)
                println(name1)
                println(name2)
                name == it.name.substringBeforeLast(".") || name1 == it.name.substringBeforeLast(".")
                        || name2 == it.name.substringBeforeLast(".")
            }?.path
                ?: ""
        }
    }
}





