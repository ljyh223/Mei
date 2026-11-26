package com.ljyh.mei.playback.log

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** At most one active upload and one successor covering writes racing its final read. */
internal class PlaybackLogScheduler(private val context: Context) {
    private val lock = Mutex()
    private var migrated = false

    suspend fun schedule() = lock.withLock {
        val manager = WorkManager.getInstance(context)
        if (!migrated) {
            // Old versions appended a work request for every event. Keep the durable outbox.
            manager.cancelUniqueWork("netease-playback-logs").result.await()
            migrated = true
        }
        val unfinished = manager.getWorkInfosForUniqueWork(WORK_NAME).await().count { !it.state.isFinished }
        if (unfinished >= 2) return@withLock
        val work = OneTimeWorkRequestBuilder<PlaybackLogWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        manager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work).result.await()
    }

    companion object { const val WORK_NAME = "netease-playback-logs-v2" }
}
