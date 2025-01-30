package com.ljyh.music.ui.local

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

val LocalNavController = compositionLocalOf<NavController> {
    error("Did not init yet!")
}