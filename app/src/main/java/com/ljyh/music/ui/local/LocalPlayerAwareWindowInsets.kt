package com.ljyh.music.ui.local

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf

val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }