package com.ljyh.mei.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.ljyh.mei.playback.PlayerConnection

val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }