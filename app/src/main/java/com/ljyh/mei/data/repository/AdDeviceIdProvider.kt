package com.ljyh.mei.data.repository

import android.content.Context
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.utils.netease.getDeviceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Keeps advertising identity separate from the normal session device identity. */
internal class AdDeviceIdProvider(
    context: Context,
    private val api: EApiService,
    private val refreshScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val preferences = context.getSharedPreferences("home_ad_device_id", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    @Volatile private var lastFailure = 0L
    private val refreshing = AtomicBoolean(false)

    /** Advertising identity is optional; home content must not wait for its network refresh. */
    fun cachedAndRefresh(): String? {
        val now = System.currentTimeMillis()
        val cached = preferences.getString("id", null)?.takeIf(String::isNotBlank)
        val timestamp = preferences.getLong("time", 0)
        val fresh = cached != null && now >= timestamp && now - timestamp < CACHE_TTL_MS
        val retryBlocked = lastFailure > 0 && now >= lastFailure && now - lastFailure < RETRY_INTERVAL_MS
        if (!fresh && !retryBlocked && refreshing.compareAndSet(false, true)) {
            refreshScope.launch {
                try { get() } finally { refreshing.set(false) }
            }
        }
        return cached
    }

    suspend fun get(): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = preferences.getString("id", null)?.takeIf(String::isNotBlank)
        val timestamp = preferences.getLong("time", 0)
        if (cached != null && now >= timestamp && now - timestamp < CACHE_TTL_MS) return@withLock cached
        if (now >= lastFailure && now - lastFailure < RETRY_INTERVAL_MS) return@withLock cached
        val id = try {
            withTimeoutOrNull(5_000) {
                api.getAdDeviceId(mapOf(
                    "adDeviceId" to cached.orEmpty(),
                    "oaid" to "",
                    "androidId" to getDeviceId(appContext),
                    "imei" to "null",
                )).let { response ->
                    response.data?.adDeviceId?.takeIf { response.code == 200 && it.isNotBlank() }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (id == null) {
            lastFailure = now
            return@withLock cached
        }
        preferences.edit().putString("id", id).putLong("time", now).apply()
        lastFailure = 0L
        id
    }

    private companion object {
        const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        const val RETRY_INTERVAL_MS = 10 * 60 * 1000L
    }
}
