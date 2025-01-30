package com.ljyh.music.ui.local

import androidx.compose.runtime.compositionLocalOf
import com.ljyh.music.data.model.TPlaylist

val LocalTPlaylist = compositionLocalOf {
    TPlaylist()
}