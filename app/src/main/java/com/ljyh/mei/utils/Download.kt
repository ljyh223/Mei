package com.ljyh.mei.utils

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.ljyh.mei.AppContext
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.DownloadWorker
import com.ljyh.mei.playback.SongDownloadInfo
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

object DownloadManager {
    private const val WORK_NAME_PREFIX = "download_playlist_"

    fun getDefaultDownloadPath(): String {
        return "Music/Mei"
    }

    suspend fun enqueue(
        context: Context,
        songs: List<SongDownloadInfo>,
        playlistName: String,
        playlistId: String = "",
        downloadPath: String = getDefaultDownloadPath()
    ) {
        DownloadWorker.createNotificationChannel(context)

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)

            val uniqueWorkName = WORK_NAME_PREFIX + playlistId.ifEmpty { System.currentTimeMillis().toString() }

            val tasks = songs.map { info ->
                DownloadTask(
                    songId = info.songId,
                    url = info.url ?: "",
                    fileName = "",
                    fileType = info.fileType.ifBlank {
                        val pathWithoutQuery = (info.url ?: "").substringBefore("?")
                        val lastSegment = pathWithoutQuery.substringAfterLast("/")
                        lastSegment.substringAfterLast(".", "")
                    },
                    status = DownloadStatus.PENDING,
                    progress = 0,
                    songTitle = info.songTitle,
                    songArtist = info.songArtist.joinToString("/"),
                    songAlbum = info.songAlbum,
                    songCover = info.songCover,
                    quality = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            db.downloadDao().insertAll(tasks)

            val songIdsJson = Gson().toJson(songs.map { it.songId })

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag("download")
                .addTag(uniqueWorkName)
                .setInputData(
                    androidx.work.Data.Builder()
                        .putString(DownloadWorker.KEY_SONG_IDS, songIdsJson)
                        .putString(DownloadWorker.KEY_PLAYLIST_NAME, playlistName)
                        .putString(DownloadWorker.KEY_DOWNLOAD_PATH, downloadPath)
                        .build()
                )
                .build()

            val wm = WorkManager.getInstance(context)
            wm.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
            Timber.tag("DownloadManager").d("Work enqueued: name=$uniqueWorkName, songs=${songs.size}")
        }
    }

    fun pauseSong(context: Context, songId: String) {
        kotlinx.coroutines.runBlocking {
            AppDatabase.getDatabase(context).downloadDao().updateStatus(songId, DownloadStatus.PAUSED)
        }
    }

    suspend fun resumeSong(
        context: Context,
        songId: String,
        playlistName: String,
        downloadPath: String = getDefaultDownloadPath()
    ) {
        val db = AppDatabase.getDatabase(context)
        val task = db.downloadDao().getBySongId(songId) ?: return
        db.downloadDao().updateStatus(songId, DownloadStatus.PENDING)
        enqueue(
            context = context,
            songs = listOf(
                SongDownloadInfo(
                    songId = task.songId,
                    url = task.url,
                    songTitle = task.songTitle,
                    songArtist = task.songArtist.split("/").map { it.trim() }.filter { it.isNotBlank() },
                    songAlbum = task.songAlbum,
                    songCover = task.songCover,
                    duration = 0
                )
            ),
            playlistName = playlistName,
            playlistId = "resume_${System.currentTimeMillis()}",
            downloadPath = downloadPath
        )
    }

    fun deleteTask(context: Context, songId: String) {
        kotlinx.coroutines.runBlocking {
            AppDatabase.getDatabase(context).downloadDao().delete(songId)
        }
    }

    fun deleteAll(context: Context) {
        kotlinx.coroutines.runBlocking {
            AppDatabase.getDatabase(context).downloadDao().deleteAll()
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("download")
    }

    fun isSongDownloaded(songId: String): Boolean {
        val db = AppDatabase.getDatabase(AppContext.instance)
        val song = kotlinx.coroutines.runBlocking { db.songDao().getSong(songId).first() }
        val path = song?.path ?: return false
        if (path.startsWith("content://")) return true
        return File(path).exists()
    }

    suspend fun isSongDownloading(songId: String): Boolean {
        val db = AppDatabase.getDatabase(AppContext.instance)
        val task = db.downloadDao().getBySongId(songId)
        return task?.status == DownloadStatus.DOWNLOADING || task?.status == DownloadStatus.PENDING
    }
}
