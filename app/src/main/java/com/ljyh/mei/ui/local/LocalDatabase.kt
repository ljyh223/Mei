package com.ljyh.mei.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.ljyh.mei.di.AppDatabase

val LocalDatabase = staticCompositionLocalOf<AppDatabase> { error("No database provided") }