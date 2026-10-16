package com.ljyh.mei.playback.sleep

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val SLEEP_CALLBACK_ACTION = "com.ljyh.mei.action.SLEEP_CLASSIFICATION"

internal data class SleepSample(val timestampMillis: Long, val confidence: Int)

private object SleepClassificationUpdates {
    val events = MutableSharedFlow<List<SleepSample>>(extraBufferCapacity = 4)
}

/** Receives Google Play services callbacks without keeping an activity alive. */
class SleepClassificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SLEEP_CALLBACK_ACTION || !SleepClassifyEvent.hasEvents(intent)) return
        val samples = SleepClassifyEvent.extractEvents(intent).map {
            SleepSample(it.timestampMillis, it.confidence)
        }
        if (samples.isEmpty()) return
        if (SleepClassificationUpdates.events.subscriptionCount.value == 0) {
            // Playback service is gone, so no music remains to pause.
            ActivityRecognition.getClient(context).removeSleepSegmentUpdates(sleepPendingIntent(context))
            return
        }
        SleepClassificationUpdates.events.tryEmit(samples)
    }
}

class SleepDetectionController(
    private val context: Context,
    scope: CoroutineScope,
    private val timer: SleepTimer,
) {
    private val client by lazy { ActivityRecognition.getClient(context) }
    private val pendingIntent by lazy { sleepPendingIntent(context) }
    private var requestId = 0
    private var pending = false
    private var gate: SleepConfidenceGate? = null
    private val eventsJob: Job = scope.launch {
        SleepClassificationUpdates.events.collect { samples ->
            if (!timer.sleepDetectionActive) return@collect
            val detector = gate ?: return@collect
            val now = System.currentTimeMillis()
            for (sample in samples.sortedBy(SleepSample::timestampMillis)) {
                if (detector.onClassification(sample.timestampMillis, sample.confidence, now)) {
                    timer.onSleepDetected()
                    break
                }
            }
        }
    }

    fun availabilityError(): String? = when {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) -> "手表暂不支持此功能"
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS ->
            "此设备无法使用 Google Play 服务的睡眠检测"
        else -> null
    }

    fun start(maxMinutes: Int, onResult: (String?) -> Unit) {
        if (timer.sleepDetectionActive) {
            timer.updateSleepDetectionDeadline(maxMinutes)
            onResult(null)
            return
        }
        availabilityError()?.let { onResult(it); return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            onResult("请先授予活动识别权限")
            return
        }

        cancelPending()
        val currentRequest = ++requestId
        pending = true
        try {
            client.requestSleepSegmentUpdates(
                pendingIntent,
                SleepSegmentRequest(SleepSegmentRequest.CLASSIFY_EVENTS_ONLY),
            ).addOnSuccessListener {
                if (currentRequest != requestId) {
                    if (!pending && !timer.sleepDetectionActive) unregister()
                    return@addOnSuccessListener
                }
                pending = false
                gate = SleepConfidenceGate(System.currentTimeMillis())
                timer.startSleepDetection(maxMinutes) { unregister() }
                onResult(null)
            }.addOnFailureListener { error ->
                if (currentRequest != requestId) return@addOnFailureListener
                pending = false
                Timber.tag("SleepDetection").e(error, "Sleep API registration failed")
                onResult("入睡检测启动失败，请检查 Google Play 服务")
            }
        } catch (error: Exception) {
            if (currentRequest != requestId) return
            pending = false
            Timber.tag("SleepDetection").e(error, "Sleep API registration failed")
            onResult("入睡检测启动失败，请检查 Google Play 服务")
        }
    }

    fun cancelPending() {
        if (!pending) return
        pending = false
        requestId++
        if (!timer.sleepDetectionActive) unregister()
    }

    fun close() {
        cancelPending()
        timer.clear()
        unregister()
        eventsJob.cancel()
    }

    private fun unregister() {
        gate = null
        try {
            client.removeSleepSegmentUpdates(pendingIntent)
                .addOnFailureListener { Timber.tag("SleepDetection").w(it, "Failed to remove sleep updates") }
        } catch (error: Exception) {
            Timber.tag("SleepDetection").w(error, "Failed to remove sleep updates")
        }
    }
}

private fun sleepPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, SleepClassificationReceiver::class.java).setAction(SLEEP_CALLBACK_ACTION),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
)
