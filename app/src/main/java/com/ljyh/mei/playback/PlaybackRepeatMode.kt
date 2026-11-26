package com.ljyh.mei.playback

import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import com.ljyh.mei.constants.LoopPlaybackKey

/** Keep the unset preference consistent with the enabled-by-default settings switch. */
internal fun Preferences.loopPlaybackEnabled(): Boolean = this[LoopPlaybackKey] ?: true

internal fun playbackRepeatMode(
    loopPlayback: Boolean,
    currentRepeatMode: Int = Player.REPEAT_MODE_OFF,
    isFmMode: Boolean = false,
): Int = when {
    isFmMode -> Player.REPEAT_MODE_ALL
    currentRepeatMode == Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
    loopPlayback -> Player.REPEAT_MODE_ALL
    else -> Player.REPEAT_MODE_OFF
}

internal fun Player.applyLoopPlayback(loopPlayback: Boolean, isFmMode: Boolean = false) {
    val mode = playbackRepeatMode(loopPlayback, repeatMode, isFmMode)
    if (repeatMode != mode) repeatMode = mode
}
