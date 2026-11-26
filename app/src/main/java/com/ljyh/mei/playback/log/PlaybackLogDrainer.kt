package com.ljyh.mei.playback.log

import com.ljyh.mei.data.network.netease.PlaybackLogAccount
import com.ljyh.mei.data.network.netease.PlaybackLogUpload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/** Delivery and acknowledgements are independent of service and WorkManager lifetimes. */
internal class PlaybackLogDrainer(
    private val outbox: PlaybackLogOutbox,
    private val account: suspend () -> PlaybackLogAccount?,
    private val upload: suspend (PendingPlaybackLog, String, PlaybackLogAccount) -> PlaybackLogUpload,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val drainLock = Mutex()
    /** Each channel is committed separately. A bounded batch fits within worker time limits. */
    suspend fun drain(): PlaybackLogDrain = drainLock.withLock {
        val current = account()
        val rows = outbox.pending(current?.owner, nowMs())
        if (current == null) return@withLock PlaybackLogDrain.COMPLETE
        for (row in rows.take(BATCH_SIZE)) {
            for (channel in PlaybackLogOutbox.CHANNELS - row.accepted) {
                val fresh = account()
                if (fresh?.owner != row.owner) {
                    outbox.pending(fresh?.owner, nowMs())
                    return@withLock PlaybackLogDrain.COMPLETE
                }
                val result = try {
                    upload(row, channel, fresh)
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
                        return@withLock PlaybackLogDrain.RETRY
                    }
                }
            }
        }
        if (rows.size > BATCH_SIZE) PlaybackLogDrain.MORE else PlaybackLogDrain.COMPLETE
    }

    companion object { const val BATCH_SIZE = 8 }
}

internal enum class PlaybackLogDrain { COMPLETE, MORE, RETRY }
