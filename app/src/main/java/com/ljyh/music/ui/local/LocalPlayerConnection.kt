package com.ljyh.music.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.ljyh.music.playback.PlayerConnection

val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }