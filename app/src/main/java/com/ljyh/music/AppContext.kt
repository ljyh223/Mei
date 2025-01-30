package com.ljyh.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppContext : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @JvmStatic
        lateinit var instance: AppContext

    }
}

