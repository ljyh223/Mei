package com.ljyh.mei

import android.os.Build
import android.os.Bundle
import android.content.ComponentName
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ljyh.mei.playback.MusicService
import com.ljyh.mei.playback.handleMediaKey
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.FirstLaunchKey
import com.ljyh.mei.data.model.domain.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.ui.app.MeiApp
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import com.ljyh.mei.utils.log.CrashHandler
import com.ljyh.mei.utils.log.FileLoggingTree
import com.ljyh.mei.utils.netease.NeteaseUtils.getAndroidId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var keyboardControllerFuture: ListenableFuture<MediaController>? = null
    private var keyboardController: MediaController? = null
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var colorRepository: ColorRepository

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        configureLogging()
        initializeDeviceIdentity()

        setContent {
            MeiApp(
                database = database,
                colorRepository = colorRepository,
                userData = UserData.VISITOR,
                startInLibrary = intent?.action == ACTION_LIBRARY,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, MusicService::class.java)))
            .buildAsync()
        keyboardControllerFuture = future
        future.addListener({
            if (keyboardControllerFuture === future) {
                runCatching { future.get() }
                    .onSuccess { keyboardController = it }
                    .onFailure { Timber.w("Keyboard media controller connection failed") }
            }
        }, mainExecutor)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val controller = keyboardController
        return (controller != null && handleMediaKey(event, controller)) || super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        keyboardController = null
        keyboardControllerFuture?.let(MediaController::releaseFuture)
        keyboardControllerFuture = null
        super.onStop()
    }

    private fun configureLogging() {
        CrashHandler.init(this)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        Timber.plant(FileLoggingTree(this))
    }

    private fun initializeDeviceIdentity() {
        lifecycleScope.launch {
            val deviceId = getAndroidId(this@MainActivity)
            val firstLaunch = dataStore.get(FirstLaunchKey, true)
            dataStore.edit { settings ->
                settings[DeviceIdKey] = deviceId
                if (firstLaunch) settings[FirstLaunchKey] = false
            }
        }
    }

    companion object {
        const val ACTION_LIBRARY = "com.ljyh.mei.action.LIBRARY"
    }
}
