package com.ljyh.mei.playback.log

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.util.concurrent.SettableFuture
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class PlaybackLogSchedulerTest {
    private lateinit var context: Context
    private lateinit var manager: WorkManager
    private val running = mutableMapOf<UUID, SettableFuture<ListenableWorker.Result>>()

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                object : ListenableWorker(appContext, workerParameters) {
                    override fun startWork(): SettableFuture<Result> = SettableFuture.create<Result>().also { running[id] = it }
                }
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder()
            .setExecutor(SynchronousExecutor()).setWorkerFactory(factory).build())
        manager = WorkManager.getInstance(context)
    }

    private fun work() = manager.getWorkInfosForUniqueWork(PlaybackLogScheduler.WORK_NAME).get(5, TimeUnit.SECONDS)

    @Test fun burstsCoalesceAndSuccessorCoversWritesDuringAnUpload() = runBlocking {
        val scheduler = PlaybackLogScheduler(context)
        repeat(30) { scheduler.schedule() }
        assertEquals(2, work().count { !it.state.isFinished })
        val head = work().single { it.state == WorkInfo.State.ENQUEUED }
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        driver.setAllConstraintsMet(head.id)
        driver.setInitialDelayMet(head.id)
        assertEquals(WorkInfo.State.RUNNING, manager.getWorkInfoById(head.id).get()!!.state)
        repeat(30) { scheduler.schedule() }
        assertEquals(2, work().count { !it.state.isFinished })
        running.getValue(head.id).set(ListenableWorker.Result.success())
        assertEquals(1, work().count { !it.state.isFinished })
        scheduler.schedule() // A write racing the successor's final read still has a future worker.
        assertEquals(2, work().count { !it.state.isFinished })
    }

    @Test fun oldBackoffChainIsCancelledWithoutCancellingReplacementWork() = runBlocking {
        val legacy = OneTimeWorkRequestBuilder<PlaybackLogWorker>().setInitialDelay(1, TimeUnit.DAYS).build()
        manager.enqueueUniqueWork("netease-playback-logs", ExistingWorkPolicy.APPEND_OR_REPLACE, legacy).result.get()
        val scheduler = PlaybackLogScheduler(context)
        scheduler.schedule()
        assertEquals(WorkInfo.State.CANCELLED, manager.getWorkInfoById(legacy.id).get()!!.state)
        assertEquals(1, work().count { !it.state.isFinished })
        scheduler.schedule()
        assertEquals(2, work().count { !it.state.isFinished })
    }
}
