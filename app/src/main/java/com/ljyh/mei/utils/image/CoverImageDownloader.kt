package com.ljyh.mei.utils.image

import com.ljyh.mei.download.DownloadWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

object CoverImageDownloader {
    private const val MAX_COVER_BYTES = 8L * 1024 * 1024
    private val httpClient = DownloadWorker.getDownloadClient().newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun downloadImageBytes(imageUrl: String): ByteArray? =
        downloadImageBytes(imageUrl, httpClient)

    internal suspend fun downloadImageBytes(
        imageUrl: String,
        client: OkHttpClient,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(imageUrl)
            .build()
        val call = client.newCall(request)
        val cancellation = coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val source = response.body.source()
                source.request(MAX_COVER_BYTES + 1)
                if (source.buffer.size > MAX_COVER_BYTES) null else source.buffer.readByteArray()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Timber.w(e, "Cover download failed")
            null
        } finally {
            cancellation?.dispose()
        }
    }
}
