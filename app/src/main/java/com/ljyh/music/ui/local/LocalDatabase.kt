package com.ljyh.music.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.ljyh.music.di.AppDatabase

val LocalDatabase = staticCompositionLocalOf<AppDatabase> { error("No database provided") }