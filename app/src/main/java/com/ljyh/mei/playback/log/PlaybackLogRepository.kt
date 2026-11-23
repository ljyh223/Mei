package com.ljyh.mei.playback.log

import android.content.Context
import android.os.Process
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.PauseListenHistoryKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.auth.extractMusicU
import com.ljyh.mei.data.network.netease.NcblCodec
import com.ljyh.mei.data.network.netease.PlaybackLogAccount
import com.ljyh.mei.data.network.netease.PlaybackLogClient
import com.ljyh.mei.data.network.netease.PlaybackLogUpload
import com.ljyh.mei.di.NeteaseInterceptor
import com.ljyh.mei.utils.netease.getDeviceId
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/** Process-owned persistence: service teardown does not cancel its final write. */
@Singleton
class PlaybackLogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: PlaybackLogClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val outbox = PlaybackLogOutbox(File(context.noBackupFilesDir, "playback-logs/outbox.json"))
    private val input = Channel<PlaybackLogEvent>(capacity = 256)
    private val drainLock = Mutex()
    private val _identity = MutableStateFlow<String?>(null)
    val identity = _identity.asStateFlow()
    val appSessionId = "${System.currentTimeMillis()}#${SecureRandom().nextInt(900) + 100}#com.ljyh.mei"

    init {
        scope.launch {
            var initialized = false
            context.dataStore.data.collect {
                val owner = account()?.owner
                if (!initialized || _identity.value != owner) {
                    initialized = true
                    _identity.value = owner
                    safely {
                        outbox.pending(owner, System.currentTimeMillis())
                        if (owner != null) schedule()
                    }
                }
            }
        }
        scope.launch {
            for (event in input) safely {
                if (account()?.owner != event.owner) return@safely
                val (ua, bi) = event.records(Process.myPid(), System.currentTimeMillis())
                outbox.add(PendingPlaybackLog(
                    owner = event.owner, createdAtMs = event.atMs,
                    ua = ua, bi = bi, entropy = NcblCodec.entropy() + NcblCodec.entropy(),
                ))
                schedule()
            }
        }
    }

    internal fun record(event: PlaybackLogEvent) {
        if (input.trySend(event).isFailure) Timber.tag("PlaybackLog").w("Persistence queue is full")
    }

    /** True asks WorkManager to retry with backoff. Each accepted channel is committed separately. */
    internal suspend fun drain(): Boolean = drainLock.withLock {
        val current = account()
        val rows = outbox.pending(current?.owner, System.currentTimeMillis())
        if (current == null) return@withLock false
        for (row in rows) {
            for (channel in PlaybackLogOutbox.CHANNELS - row.accepted) {
                val fresh = account()
                if (fresh?.owner != row.owner) {
                    outbox.pending(fresh?.owner, System.currentTimeMillis())
                    return@withLock false
                }
                val result = try {
                    client.upload(row, channel, fresh)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Timber.tag("PlaybackLog").w("Upload deferred: %s", failure.javaClass.simpleName)
                    PlaybackLogUpload.RETRY
                }
                when (result) {
                    PlaybackLogUpload.ACCEPTED -> outbox.accepted(row.id, channel)
                    PlaybackLogUpload.REJECTED -> {
                        outbox.remove(row.id)
                        Timber.tag("PlaybackLog").w("Upload rejected")
                        break
                    }
                    PlaybackLogUpload.RETRY -> {
                        outbox.failed(row.id)
                        return@withLock true
                    }
                }
            }
        }
        false
    }

    private suspend fun account(): PlaybackLogAccount? {
        val prefs = context.dataStore.data.first()
        if (prefs[PauseListenHistoryKey] == true) return null
        val userId = prefs[UserIdKey]?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true } ?: return null
        val raw = prefs[CookieKey]?.trim().orEmpty()
        val token = if ('=' in raw) extractMusicU(listOf(raw)) else raw
        if (token.isNullOrBlank() || token.any { it.isWhitespace() || it == ';' }) return null
        return PlaybackLogAccount(
            userId, token, prefs[DeviceIdKey]?.takeIf(String::isNotBlank) ?: getDeviceId(context),
            NeteaseInterceptor.playbackLogProfile(),
        )
    }

    private fun schedule() {
        val work = OneTimeWorkRequestBuilder<PlaybackLogWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Appending avoids losing an enqueue racing the last drain of a running worker.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            Timber.tag("PlaybackLog").w("Persistence deferred: %s", failure.javaClass.simpleName)
        }
    }

    private companion object { const val WORK_NAME = "netease-playback-logs" }
}
