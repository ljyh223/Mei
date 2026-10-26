package com.ljyh.mei.playback.source

import com.ljyh.mei.utils.preferences.AudioSourceMode

/** Failure in the selected mode must never fall through to the other resolver. */
internal suspend fun <T> AudioSourceMode.resolveAlternative(
    lx: suspend () -> T?,
    unblock: suspend () -> T?,
): T? = when (this) {
    AudioSourceMode.Off -> null
    AudioSourceMode.Lx -> lx()
    AudioSourceMode.Unblock -> unblock()
}

internal fun AudioSourceMode.recoveryKey(mediaId: String): String = "$mediaId|$name"
