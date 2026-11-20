package com.ljyh.mei.data.network

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AppleMusicWebTokenKey
import com.ljyh.mei.constants.AppleMusicWebTokenRefreshAtKey
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Shared web token and authenticated catalogue transport for album and artist artwork. */
@Singleton
class AppleMusicCatalogClient internal constructor(
    private val context: Context,
    private val webClient: OkHttpClient,
    private val webOrigin: String,
    private val catalogOrigin: String,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(
        context, OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build(),
        "https://music.apple.com", "https://amp-api.music.apple.com",
    )
    private val tokenMutex = Mutex()
    private var webToken: String? = null
    private var webTokenUntil = 0L

    suspend fun warmToken() = withContext(Dispatchers.IO) { getWebToken(); Unit }

    suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        val target = url.toHttpUrl()
        val origin = catalogOrigin.toHttpUrl()
        require(target.scheme == origin.scheme && target.host == origin.host && target.port == origin.port)
        appleGet(url)
    }

    private suspend fun appleGet(url: String): JSONObject {
        for (attempt in 0..1) {
            val token = getWebToken()
            val request = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .header("Origin", webOrigin)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            executeCancelable(request).use { response ->
                if (response.code == 401 && attempt == 0) {
                    tokenMutex.withLock {
                        webToken = null
                        webTokenUntil = 0L
                        context.dataStore.edit {
                            it.remove(AppleMusicWebTokenKey)
                            it.remove(AppleMusicWebTokenRefreshAtKey)
                        }
                    }
                } else {
                    if (!response.isSuccessful) throw IOException("Apple catalog: HTTP ${response.code}")
                    return JSONObject(String(readLimited(response.body.byteStream(), 2 * 1024 * 1024)))
                }
            }
        }
        throw IOException("Apple catalog authorization failed")
    }

    private suspend fun executeCancelable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = webClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) continuation.resume(response)
                    else response.close()
                }
            })
        }

    private suspend fun getWebToken(): String = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        webToken?.takeIf { webTokenUntil > now }?.let { return@withLock it }
        val stored = context.dataStore.data.first()
        val savedToken = stored[AppleMusicWebTokenKey]
        val savedRefreshAt = stored[AppleMusicWebTokenRefreshAtKey] ?: 0L
        val savedExpiry = savedToken?.let { runCatching { tokenExpiryMillis(it) }.getOrNull() } ?: 0L
        if (savedToken != null && savedExpiry > now && savedRefreshAt > now) {
            webToken = savedToken
            webTokenUntil = savedRefreshAt
            return@withLock savedToken
        }
        try {
            val html = String(getWebBytes(webOrigin, 16 * 1024 * 1024))
            val jsPath = Regex("/assets/index~[^\\\"']+\\.js").find(html)?.value
                ?: throw IOException("Apple web player script not found")
            val js = String(getWebBytes("$webOrigin$jsPath", 64 * 1024 * 1024))
            val token = Regex("eyJ[A-Za-z0-9_=-]+\\.eyJ[A-Za-z0-9_=-]+\\.[A-Za-z0-9_=-]+")
                .find(js)?.value ?: throw IOException("Apple web token not found")
            val expiryMillis = tokenExpiryMillis(token)
            if (expiryMillis <= now) throw IOException("Apple web token expired")
            val refreshAt = now + (expiryMillis - now) / 2
            context.dataStore.edit {
                it[AppleMusicWebTokenKey] = token
                it[AppleMusicWebTokenRefreshAtKey] = refreshAt
            }
            webToken = token
            webTokenUntil = refreshAt
            token
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // A token past its refresh point is still usable until exp if renewal is offline.
            if (savedToken == null || savedExpiry <= now) throw error
            webToken = savedToken
            webTokenUntil = minOf(savedExpiry, now + 6 * 60 * 60_000L)
            savedToken
        }
    }

    private fun tokenExpiryMillis(token: String): Long {
        val payload = token.split('.').getOrNull(1) ?: throw IOException("Invalid Apple web token")
        val claims = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
        return claims.optLong("exp") * 1000L
    }

    private suspend fun getWebBytes(url: String, limit: Int): ByteArray {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        executeCancelable(request).use { response ->
            if (!response.isSuccessful) throw IOException("Apple web player: HTTP ${response.code}")
            return readLimited(response.body.byteStream(), limit)
        }
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray = input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (out.size() + count > limit) throw IOException("Apple response too large")
            out.write(buffer, 0, count)
        }
        out.toByteArray()
    }
}
