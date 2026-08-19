package com.ljyh.mei

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.ljyh.mei.constants.DeviceIdKey
import com.ljyh.mei.constants.FirstLaunchKey
import com.ljyh.mei.constants.UserAgent
import com.ljyh.mei.data.model.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.ui.app.MeiApp
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.log.CrashHandler
import com.ljyh.mei.utils.log.FileLoggingTree
import com.ljyh.mei.utils.netease.NeteaseUtils.getAndroidId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var colorRepository: ColorRepository

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLogging()
        initializeDeviceIdentity()

        val imageClient = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor())
            .build()
        setContent {
            MeiApp(
                database = database,
                colorRepository = colorRepository,
                userData = UserData.VISITOR,
                startInLibrary = intent?.action == ACTION_LIBRARY,
                imageClient = imageClient,
            )
        }
    }

    private fun configureLogging() {
        CrashHandler.init(this)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        Timber.plant(FileLoggingTree(this))
    }

    private fun initializeDeviceIdentity() {
        lifecycleScope.launch {
            getAndroidId(this@MainActivity)
            if (dataStore.get(FirstLaunchKey, true)) {
                dataStore.edit { settings ->
                    settings[FirstLaunchKey] = false
                    settings[DeviceIdKey] = com.ljyh.mei.utils.getDeviceId()
                }
            }
        }
    }

    private fun userAgentInterceptor() = Interceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader("User-Agent", UserAgent)
                .build(),
        )
    }

    companion object {
        const val ACTION_LIBRARY = "com.ljyh.mei.action.LIBRARY"
    }
}
