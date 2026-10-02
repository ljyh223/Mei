package com.ljyh.mei.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.ljyh.mei.data.repository.DynamicCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

private val coverDownloadClient = OkHttpClient.Builder()
    .callTimeout(2, TimeUnit.MINUTES)
    .build()

internal fun isDirectMp4(url: String): Boolean = url.toHttpUrlOrNull()
    ?.encodedPath?.endsWith(".mp4", ignoreCase = true) == true

internal fun dynamicCoverFileName(title: String, source: DynamicCover.Source, variant: String): String {
    val safeTitle = title.replace(Regex("[\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim().trim('.').take(80).ifBlank { "动态封面" }
    val sourceName = if (source == DynamicCover.Source.APPLE_MUSIC) "AppleMusic" else "网易云"
    return "${safeTitle}_${sourceName}_${variant}.mp4"
}

/** Saves direct MP4 assets as-is and converts Apple Music HLS artwork into one silent MP4. */
@OptIn(UnstableApi::class)
suspend fun saveDynamicCoverToMovies(
    context: Context,
    cover: DynamicCover,
    title: String,
    variant: String,
): Uri {
    val appContext = context.applicationContext
    val tempFile = File(appContext.cacheDir, "motion-export-${UUID.randomUUID()}.mp4")
    try {
        if (isDirectMp4(cover.url)) downloadMp4(cover.url, tempFile)
        else exportMp4(appContext, cover.url, tempFile)
        return withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, dynamicCoverFileName(title, cover.source, variant))
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/MeiMusic/")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("无法创建视频文件")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IOException("无法写入视频文件")
                val updated = resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }, null, null)
                if (updated == 0) throw IOException("无法完成视频保存")
                uri
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        }
    } finally {
        tempFile.delete()
    }
}

private suspend fun downloadMp4(url: String, output: File) = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).build()
    coverDownloadClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("下载失败：HTTP ${response.code}")
        val body = response.body
        output.outputStream().use { destination -> body.byteStream().use { it.copyTo(destination) } }
        if (output.length() == 0L) throw IOException("视频文件为空")
    }
}

@OptIn(UnstableApi::class)
private suspend fun exportMp4(context: Context, url: String, output: File) =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val mediaItem = MediaItem.Builder().setUri(url).apply {
                if (url.contains(".m3u8", ignoreCase = true)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }.build()
            val editedItem = EditedMediaItem.Builder(mediaItem).setRemoveAudio(true).build()
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                if (Looper.myLooper() == Looper.getMainLooper()) transformer.cancel()
                else Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }
            try {
                transformer.start(editedItem, output.absolutePath)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }
