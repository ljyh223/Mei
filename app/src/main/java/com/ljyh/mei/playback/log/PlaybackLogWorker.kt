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
        if (repository.drain()) Result.retry() else Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}
