package com.ljyh.mei.playback.log

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaybackLogWorkerEntryPoint {
    fun playbackLogRepository(): PlaybackLogRepository
}

class PlaybackLogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val repository = EntryPointAccessors.fromApplication(
            applicationContext, PlaybackLogWorkerEntryPoint::class.java,
        ).playbackLogRepository()
        val outcome = try {
            repository.drain()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            PlaybackLogDrain.RETRY
        }
        when {
            outcome == PlaybackLogDrain.COMPLETE -> Result.success()
            outcome == PlaybackLogDrain.RETRY && runAttemptCount < 3 -> Result.retry()
            else -> {
                // Hand off to the successor instead of keeping the entire chain in long backoff.
                repository.schedule()
                Result.success()
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // A scheduling/database failure must not fail the successor chain.
        Result.retry()
    }
}
