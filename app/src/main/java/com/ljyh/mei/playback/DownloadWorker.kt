package com.ljyh.mei.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljyh.mei.MainActivity
import com.ljyh.mei.R
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.utils.ImageUtils
import com.ljyh.mei.utils.SongMate
import com.ljyh.mei.utils.StringUtils.specialReplace
import com.ljyh.mei.utils.lyric.DownloadLyricProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.jaudiotagger.audio.AudioFileIO
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadWorkerEntryPoint {
    fun downloadLyricProvider(): DownloadLyricProvider
}

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
        private const val CONCURRENCY = 3

        @Volatile
        private var sharedClient: OkHttpClient? = null

        fun getDownloadClient(): OkHttpClient {
            return sharedClient ?: synchronized(this) {
                sharedClient ?: OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build().also { sharedClient = it }
            }
        }

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

    private val okHttpClient = getDownloadClient()
    private val lyricProvider: DownloadLyricProvider by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        ).downloadLyricProvider()
    }
    private val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
    private val failedCount = java.util.concurrent.atomic.AtomicInteger(0)

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

        val sanitizedPlaylistName = specialReplace(playlistName).trim()
        val relativePath = "Music/Mei/$sanitizedPlaylistName"
        val tempDir = File(applicationContext.cacheDir, "download")
        if (!tempDir.exists()) tempDir.mkdirs()

        showNotification("准备下载...", 0)

        val semaphore = Semaphore(CONCURRENCY)
        coroutineScope {
            songIds.map { songId ->
                async(Dispatchers.IO) {
                    if (isStopped) return@async

                    semaphore.withPermit {
                        if (isStopped) return@withPermit

                        processSong(songId, db, tempDir, relativePath)
                    }

                    val done = completedCount.get() + failedCount.get()
                    showNotification("正在下载 ($done/$totalCount)", done * 100 / totalCount)
                }
            }.awaitAll()
        }

        if (isStopped) {
            showNotification("下载已取消", 0, ongoing = false)
            return Result.failure()
        }

        val done = completedCount.get()
        val failed = failedCount.get()
        val statusText = if (failed > 0) "完成 $done, 失败 $failed" else "全部下载完成"
        showNotification(statusText, 100, ongoing = false)

        return Result.success()
    }

    private suspend fun processSong(
        songId: String,
        db: AppDatabase,
        tempDir: File,
        relativePath: String
    ) {
        val task = db.downloadDao().getBySongId(songId)
        if (task == null || task.url.isBlank() || task.status == DownloadStatus.PAUSED) {
            failedCount.incrementAndGet()
            updateTask(db, songId, DownloadStatus.FAILED, 0)
            return
        }

        val existingSong = db.songDao().getSong(songId).first()
        if (existingSong != null && existingSong.path != null) {
            val isValid = if (existingSong.path.startsWith("content://")) {
                try {
                    applicationContext.contentResolver.openInputStream(
                        existingSong.path.toUri()
                    )?.close()
                    true
                } catch (_: Exception) { false }
            } else {
                File(existingSong.path).exists()
            }
            if (isValid) {
                try {
                    repairTagsIfNeeded(songId, task, existingSong.path, tempDir)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Tag repair failed for ${task.songTitle}")
                }
                updateTask(db, songId, DownloadStatus.COMPLETED, 100)
                completedCount.incrementAndGet()
                return
            }
        }

        updateTask(db, songId, DownloadStatus.DOWNLOADING, 0)

        val suffix = task.fileType.ifBlank {
            val pathWithoutQuery = task.url.substringBefore("?")
            val lastSegment = pathWithoutQuery.substringAfterLast("/")
            lastSegment.substringAfterLast(".", "")
        }
        if (suffix.isBlank()) {
            failedCount.incrementAndGet()
            updateTask(db, songId, DownloadStatus.FAILED, 0)
            return
        }

        val fileName = "${specialReplace("${task.songTitle} - ${task.songArtist}")}.$suffix"
        val tempFile = File(tempDir, fileName)

        val existingMedia = withContext(Dispatchers.IO) {
            findMediaStoreAudio(applicationContext, fileName, relativePath)
        }
        if (existingMedia != null) {
            try {
                repairTagsIfNeeded(songId, task, existingMedia.uri.toString(), tempDir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Tag repair failed for existing MediaStore file ${task.songTitle}")
            }
            saveDownloadedSong(
                db = db,
                songId = songId,
                task = task,
                uri = existingMedia.uri,
                durationMs = existingMedia.durationMs ?: existingSong?.duration ?: 0L,
                relativePath = relativePath
            )
            updateTask(db, songId, DownloadStatus.COMPLETED, 100)
            completedCount.incrementAndGet()
            return
        }

        try {
            coroutineScope {
            val lyricDeferred = async(Dispatchers.IO) {
                lyricProvider.getEmbeddedLyric(songId)
            }
            val coverDeferred = async(Dispatchers.IO) {
                if (task.songCover.isNotBlank()) {
                    ImageUtils.downloadImageBytes(task.songCover)
                } else null
            }

            val success = downloadFile(task.url, tempFile) { progress ->
                updateTask(db, songId, DownloadStatus.DOWNLOADING, progress)
            }

            if (success && tempFile.exists()) {
                try {
                    val lyric = lyricDeferred.await()
                    val coverBytes = coverDeferred.await()
                    val tagStatus = SongMate.writeTagsWithCoverBytes(
                        task.songTitle, task.songArtist, task.songAlbum,
                        coverBytes, tempFile.absolutePath, lyric
                    )
                    if (lyric != null && tagStatus?.hasLyric != true) {
                        Timber.w("Lyric tag verification failed for ${task.songTitle}")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "writeTags failed for ${task.songTitle}")
                }

                val audioDuration = withContext(Dispatchers.IO) {
                    try {
                        AudioFileIO.read(tempFile).audioHeader.trackLength
                    } catch (_: Exception) { 0 }
                }

                val mediaStoreUri = withContext(Dispatchers.IO) {
                    insertToMediaStore(applicationContext, tempFile, fileName, suffix, relativePath)
                }

                if (mediaStoreUri != null) {
                    saveDownloadedSong(
                        db = db,
                        songId = songId,
                        task = task,
                        uri = mediaStoreUri,
                        durationMs = audioDuration.toLong() * 1_000L,
                        relativePath = relativePath
                    )
                    updateTask(db, songId, DownloadStatus.COMPLETED, 100)
                    completedCount.incrementAndGet()
                } else {
                    failedCount.incrementAndGet()
                    updateTask(db, songId, DownloadStatus.FAILED, 0)
                }
                tempFile.delete()
            } else {
                failedCount.incrementAndGet()
                updateTask(db, songId, DownloadStatus.FAILED, 0)
            }
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Download failed for ${task.songTitle}")
            failedCount.incrementAndGet()
            updateTask(db, songId, DownloadStatus.FAILED, 0)
        }
    }

    private suspend fun saveDownloadedSong(
        db: AppDatabase,
        songId: String,
        task: DownloadTask,
        uri: Uri,
        durationMs: Long,
        relativePath: String
    ) {
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
                duration = durationMs,
                path = uri.toString(),
                sourceType = SourceType.DOWNLOAD,
                folderPath = relativePath
            )
        )
    }

    private suspend fun repairTagsIfNeeded(
        songId: String,
        task: DownloadTask,
        path: String,
        tempDir: File
    ) = withContext(Dispatchers.IO) {
        val contentUri = path.takeIf { it.startsWith("content://") }?.toUri()
        val workingFile = if (contentUri != null) {
            val suffix = task.fileType.ifBlank { "mp3" }
            File(tempDir, "repair-$songId.$suffix").also { file ->
                val input = applicationContext.contentResolver.openInputStream(contentUri)
                    ?: error("Unable to open downloaded song for tag repair: $contentUri")
                input.use { source ->
                    file.outputStream().use { target -> source.copyTo(target, 64 * 1024) }
                }
            }
        } else {
            File(path)
        }

        try {
            val currentStatus = SongMate.checkTags(workingFile.absolutePath) ?: return@withContext
            // A non-empty lyric tag may still contain NetEase contributor JSON or
            // a lower-quality LRC. Resolve it again so old downloads are upgraded.
            val lyric = lyricProvider.getEmbeddedLyric(songId)
            if (currentStatus.isBasicComplete && lyric == null) return@withContext

            val updatedStatus = if (currentStatus.isBasicComplete) {
                SongMate.writeTagsWithCoverBytes(
                    task.songTitle,
                    task.songArtist,
                    task.songAlbum,
                    coverBytes = null,
                    filePath = workingFile.absolutePath,
                    lyric = lyric
                )
            } else {
                SongMate.writeTags(
                    task.songTitle,
                    task.songArtist,
                    task.songAlbum,
                    task.songCover,
                    workingFile.absolutePath,
                    lyric
                )
            } ?: return@withContext

            if (lyric != null && !updatedStatus.hasLyric) {
                Timber.w("Lyric tag verification failed while repairing ${task.songTitle}")
                return@withContext
            }

            if (contentUri != null) {
                val output = applicationContext.contentResolver.openOutputStream(contentUri, "rwt")
                    ?: error("Unable to update downloaded song after tag repair: $contentUri")
                output.use { target ->
                    workingFile.inputStream().use { source -> source.copyTo(target, 64 * 1024) }
                }
            }
        } finally {
            if (contentUri != null) workingFile.delete()
        }
    }

    private fun insertToMediaStore(
        context: Context,
        srcFile: File,
        displayName: String,
        fileType: String,
        relativePath: String
    ): Uri? {
        findMediaStoreAudio(context, displayName, relativePath)?.let { return it.uri }

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
                srcFile.inputStream().use { input -> input.copyTo(os, 64 * 1024) }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            return uri
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to write to MediaStore")
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }

    private fun findMediaStoreAudio(
        context: Context,
        displayName: String,
        relativePath: String
    ): MediaStoreAudio? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        val selection =
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(displayName, "$relativePath/")
        return context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            while (cursor.moveToNext()) {
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else 0L
                if (size <= 0L) continue
                val id = cursor.getLong(idIndex)
                val duration = if (durationIndex >= 0 && !cursor.isNull(durationIndex)) {
                    cursor.getLong(durationIndex)
                } else null
                return@use MediaStoreAudio(
                    uri = ContentUris.withAppendedId(collection, id),
                    durationMs = duration
                )
            }
            null
        }
    }

    private data class MediaStoreAudio(
        val uri: Uri,
        val durationMs: Long?
    )

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
                val read = source.read(buffer, 64 * 1024)
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
        } catch (e: CancellationException) {
            throw e
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
