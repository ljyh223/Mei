package com.ljyh.mei.playback.log

import android.content.Context
import android.os.Process
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.PauseListenHistoryKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.auth.extractMusicU
import com.ljyh.mei.data.network.netease.NcblCodec
import com.ljyh.mei.data.network.netease.PlaybackLogAccount
import com.ljyh.mei.data.network.netease.PlaybackLogClient
import com.ljyh.mei.di.NeteaseInterceptor
import com.ljyh.mei.utils.netease.getDeviceId
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
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
    private val drainer = PlaybackLogDrainer(outbox, ::account, client::upload)
    private val scheduler = PlaybackLogScheduler(context)
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

    internal suspend fun drain(): PlaybackLogDrain = drainer.drain()

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

    internal suspend fun schedule() = scheduler.schedule()

    private suspend fun safely(block: suspend () -> Unit) {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            Timber.tag("PlaybackLog").w("Persistence deferred: %s", failure.javaClass.simpleName)
        }
    }
}
