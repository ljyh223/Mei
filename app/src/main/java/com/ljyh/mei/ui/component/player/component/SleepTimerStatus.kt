package com.ljyh.mei.ui.component.player.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ljyh.mei.playback.sleep.SleepTimer
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import kotlinx.coroutines.delay

/** Shared timer label for the player toolbar and the More sheet. */
@Composable
internal fun rememberSleepTimerStatus(timer: SleepTimer): String? {
    val active = timer.isActive
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer, active) {
        if (active) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    return when {
        !active -> null
        timer.pauseWhenSongEnd -> "本曲结束"
        else -> makeTimeString((timer.triggerTime - nowMillis).coerceAtLeast(0L))
    }
}
