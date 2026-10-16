package com.ljyh.mei.playback.sleep

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    val player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    private var onSleepDetectionEnded: (() -> Unit)? = null
    var triggerTime by mutableLongStateOf(-1L)
    var pauseWhenSongEnd by mutableStateOf(false)
    var sleepDetectionActive by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int) {
        clear()
        if (minute == -1) {
            pauseWhenSongEnd = true
        } else {
            startCountdown(minute)
        }
    }

    fun startSleepDetection(maxMinutes: Int, onEnded: () -> Unit) {
        clear()
        sleepDetectionActive = true
        onSleepDetectionEnded = onEnded
        startCountdown(maxMinutes)
    }

    fun updateSleepDetectionDeadline(maxMinutes: Int) {
        if (!sleepDetectionActive) return
        sleepTimerJob?.cancel()
        startCountdown(maxMinutes)
    }

    fun onSleepDetected() {
        if (!sleepDetectionActive) return
        player.pause()
        clear()
    }

    private fun startCountdown(minute: Int) {
        triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
        sleepTimerJob = scope.launch {
            delay(minute.minutes)
            player.pause()
            clear()
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
        sleepDetectionActive = false
        val cleanup = onSleepDetectionEnded
        onSleepDetectionEnded = null
        cleanup?.invoke()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }
}
