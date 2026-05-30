package com.ljyh.mei.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljyh.mei.MainActivity
import com.ljyh.mei.R
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.utils.SongMate
import com.ljyh.mei.utils.StringUtils.specialReplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SONG_IDS = "song_ids_json"
        const val KEY_PLAYLIST_NAME = "playlist_name"
        const val KEY_DOWNLOAD_PATH = "download_path"
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "歌曲下载进度通知"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun doWork(): Result {
        Timber.d("DownloadWorker started, runAttemptCount=$runAttemptCount")

        val songIdsJson = inputData.getString(KEY_SONG_IDS) ?: run {
            Timber.e("KEY_SONG_IDS not found in inputData")
            return Result.failure()
        }
        val playlistName = inputData.getString(KEY_PLAYLIST_NAME) ?: "未分类"
        val downloadPath = inputData.getString(KEY_DOWNLOAD_PATH)
            ?: "Music/Mei"

        val songIds: List<String> = try {
            Gson().fromJson(songIdsJson, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse songIds")
            return Result.failure()
        }

        if (songIds.isEmpty()) return Result.success()

        Timber.d("DownloadWorker will process ${songIds.size} songs, path=$downloadPath")
        createNotificationChannel(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        val totalCount = songIds.size
        var completedCount = 0
        var failedCount = 0

        val sanitizedPlaylistName = specialReplace(playlistName).trim()
        val relativePath = "Music/Mei/$sanitizedPlaylistName"
        val tempDir = File(applicationContext.cacheDir, "download")
        if (!tempDir.exists()) tempDir.mkdirs()

        showNotification("准备下载...", 0)

        for (songId in songIds) {
            if (isStopped) {
                showNotification("下载已取消", 0, ongoing = false)
                return Result.failure()
            }

            val task = db.downloadDao().getBySongId(songId)
            if (task == null || task.url.isBlank() || task.status == DownloadStatus.PAUSED) {
                failedCount++
                updateTask(db, songId, DownloadStatus.FAILED, 0)
                showNotification("正在下载 ($completedCount/$totalCount)", completedCount * 100 / totalCount)
                continue
            }

            val existingSong = db.songDao().getSong(songId).first()
            if (existingSong != null && existingSong.path != null) {
                val isValid = if (existingSong.path.startsWith("content://")) {
                    try {
                        applicationContext.contentResolver.openInputStream(
                            Uri.parse(existingSong.path)
                        )?.close()
                        true
                    } catch (_: Exception) { false }
                } else {
                    File(existingSong.path).exists()
                }
                if (isValid) {
                    updateTask(db, songId, DownloadStatus.COMPLETED, 100)
                    completedCount++
                    showNotification("正在下载 ($completedCount/$totalCount)", completedCount * 100 / totalCount)
                    continue
                }
            }

            updateTask(db, songId, DownloadStatus.DOWNLOADING, 0)

            val suffix = task.fileType.ifBlank {
                val pathWithoutQuery = task.url.substringBefore("?")
                val lastSegment = pathWithoutQuery.substringAfterLast("/")
                lastSegment.substringAfterLast(".", "")
            }
            if (suffix.isBlank()) {
                failedCount++
                updateTask(db, songId, DownloadStatus.FAILED, 0)
                continue
            }

            val fileName = "${specialReplace("${task.songTitle} - ${task.songArtist}")}.$suffix"
            val tempFile = File(tempDir, fileName)

            try {
                val success = downloadFile(task.url, tempFile) { progress ->
                    updateTask(db, songId, DownloadStatus.DOWNLOADING, progress)
                }
                if (success && tempFile.exists()) {
                    try {
                        SongMate.writeTags(
                            task.songTitle, task.songArtist, task.songAlbum,
                            task.songCover, tempFile.absolutePath
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "writeTags failed for ${task.songTitle}")
                    }

                    val audioDuration = withContext(Dispatchers.IO) {
                        try {
                            org.jaudiotagger.audio.AudioFileIO.read(tempFile).audioHeader.trackLength
                        } catch (_: Exception) { 0 }
                    }

                    val mediaStoreUri = withContext(Dispatchers.IO) {
                        insertToMediaStore(applicationContext, tempFile, fileName, suffix, relativePath)
                    }

                    if (mediaStoreUri != null) {
                        db.songDao().insertSong(
                            Song(
                                id = songId,
                                title = task.songTitle,
                                artist = task.songArtist
                                    .split(Regex("[/、,;]"))
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .ifEmpty { listOf(task.songArtist.trim()) },
                                album = task.songAlbum,
                                cover = task.songCover,
                                duration = audioDuration.toLong(),
                                path = mediaStoreUri.toString(),
                                sourceType = SourceType.DOWNLOAD,
                                folderPath = relativePath
                            )
                        )
                        updateTask(db, songId, DownloadStatus.COMPLETED, 100)
                        completedCount++
                    } else {
                        failedCount++
                        updateTask(db, songId, DownloadStatus.FAILED, 0)
                    }
                    tempFile.delete()
                } else {
                    failedCount++
                    updateTask(db, songId, DownloadStatus.FAILED, 0)
                }
            } catch (e: Exception) {
                Timber.e(e, "Download failed for ${task.songTitle}")
                failedCount++
                updateTask(db, songId, DownloadStatus.FAILED, 0)
            }

            showNotification("正在下载 ($completedCount/$totalCount)", completedCount * 100 / totalCount)
        }

        val statusText = if (failedCount > 0) "完成 $completedCount, 失败 $failedCount" else "全部下载完成"
        showNotification(statusText, 100, ongoing = false)

        return Result.success()
    }

    private fun insertToMediaStore(
        context: Context,
        srcFile: File,
        displayName: String,
        fileType: String,
        relativePath: String
    ): android.net.Uri? {
        val mimeType = when (fileType.lowercase()) {
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            else -> "audio/mpeg"
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "$relativePath/")
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                srcFile.inputStream().use { input -> input.copyTo(os) }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            return uri
        } catch (e: Exception) {
            Timber.e(e, "Failed to write to MediaStore")
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }

    private suspend fun downloadFile(
        url: String,
        file: File,
        onProgress: suspend (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            val source = body.source()
            val buffer = okio.Buffer()

            file.parentFile?.mkdirs()
            val sink = file.sink().buffer()

            var lastProgress = -1
            while (true) {
                val read = source.read(buffer, 8192)
                if (read == -1L) break
                sink.write(buffer, read)
                downloadedBytes += read

                if (totalBytes > 0) {
                    val progress = (downloadedBytes * 100 / totalBytes).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }
            }

            sink.flush()
            sink.close()
            source.close()
            response.close()

            true
        } catch (e: Exception) {
            Timber.e(e, "downloadFile error")
            false
        }
    }

    private suspend fun updateTask(
        db: AppDatabase,
        songId: String,
        status: DownloadStatus,
        progress: Int
    ) {
        try {
            db.downloadDao().updateProgress(songId, status, progress, System.currentTimeMillis())
        } catch (e: Exception) {
            Timber.e(e, "updateTask error")
        }
    }

    private fun showNotification(title: String, progress: Int, ongoing: Boolean = progress < 100) {
        try {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Mei 音乐下载")
                .setSmallIcon(R.drawable.baseline_download_24)
                .setOngoing(ongoing)
                .setProgress(100, progress, !ongoing)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }
}

data class SongDownloadInfo(
    val songId: String,
    val url: String?,
    val songTitle: String,
    val songArtist: List<String>,
    val songAlbum: String,
    val songCover: String,
    val duration: Long,
    val fileType: String = "",
    val lyric: String = ""
)
