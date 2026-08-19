package com.ljyh.mei.ui.component.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.MusicService
import com.ljyh.mei.playback.PlayerConnection
import timber.log.Timber

@Composable
fun rememberPlayerConnection(
    context: Context,
    database: AppDatabase,
): PlayerConnection? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerConnection by remember { mutableStateOf<PlayerConnection?>(null) }

    DisposableEffect(context, database, lifecycleOwner) {
        val intent = Intent(context, MusicService::class.java)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Timber.tag("PlayerConnection").d("Service connected")
                if (service is MusicService.MusicBinder) {
                    playerConnection = PlayerConnection(
                        context = context,
                        binder = service,
                        database = database,
                        scope = lifecycleOwner.lifecycleScope,
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Timber.tag("PlayerConnection").d("Service disconnected")
                playerConnection?.dispose()
                playerConnection = null
            }
        }

        context.startService(intent)
        val isBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            playerConnection?.dispose()
            playerConnection = null
            if (isBound) context.unbindService(connection)
        }
    }

    return playerConnection
}
