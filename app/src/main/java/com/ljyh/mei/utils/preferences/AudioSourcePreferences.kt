package com.ljyh.mei.utils.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/** One stored mode makes LX and legacy unblocking mutually exclusive. */
enum class AudioSourceMode(val label: String) {
    Off("关闭"),
    Lx("LX 自定义音源"),
    Unblock("解灰"),
}

val AudioSourceModeKey = stringPreferencesKey("audioSourceMode")

// The previous production path always used LX, regardless of the old test switch.
fun Preferences.audioSourceMode(): AudioSourceMode = this[AudioSourceModeKey].toEnum(AudioSourceMode.Lx)
