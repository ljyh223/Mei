package com.ljyh.mei.ui.screen.setting

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.LoopPlaybackKey
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.constants.NoAudioSourceKey
import com.ljyh.mei.ui.component.EnumListPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySetting(
    scrollBehavior: TopAppBarScrollBehavior
){

    val navController = LocalNavController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val (desktopLyricsEnabled, onDesktopLyricsChange) = rememberPreference(
        key = DesktopLyricsEnabledKey,
        defaultValue = false
    )
    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var waitingForOverlayPermission by remember { mutableStateOf(false) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayPermissionGranted = Settings.canDrawOverlays(context)
        if (waitingForOverlayPermission && overlayPermissionGranted) onDesktopLyricsChange(true)
        waitingForOverlayPermission = false
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayPermissionGranted = Settings.canDrawOverlays(context)
                if (waitingForOverlayPermission && overlayPermissionGranted) {
                    onDesktopLyricsChange(true)
                    waitingForOverlayPermission = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val (musicQuality, onMusicQualityChange) = rememberEnumPreference(
        key = MusicQualityKey,
        defaultValue = MusicQuality.EXHIGH,
    )
    val (loopPlayback, onLoopPlaybackChange) = rememberPreference(
        key = LoopPlaybackKey,
        defaultValue = true
    )

    val (noAudioSource, onNoAudioSourceChange) = rememberPreference(
        NoAudioSourceKey,
        defaultValue = false
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放设置") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        Column(
            Modifier
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceGroupTitle(
                title = "PLAY"
            )
            SwitchPreference(
                title = { Text("桌面歌词") },
                description = if (overlayPermissionGranted) "播放时显示，可拖动位置" else "开启需授予悬浮窗权限",
                icon = { Icon(Icons.Rounded.Lyrics, null) },
                checked = desktopLyricsEnabled && overlayPermissionGranted,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        onDesktopLyricsChange(false)
                    } else if (Settings.canDrawOverlays(context)) {
                        overlayPermissionGranted = true
                        onDesktopLyricsChange(true)
                    } else {
                        waitingForOverlayPermission = true
                        overlayPermissionLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )
            SwitchPreference(
                title = { Text("循环播放") },
                icon = { Icon(Icons.Rounded.Loop, null) },
                checked = loopPlayback,
                onCheckedChange = onLoopPlaybackChange
            )
            SwitchPreference(
                title = { Text("无音频源时下一首歌曲") },
                description = "无音频源时下一首歌曲，否则暂停",
                icon = { Icon(Icons.Rounded.HideSource, null) },
                checked = noAudioSource,
                onCheckedChange = onNoAudioSourceChange
            )
            EnumListPreference(
                title = { Text("音乐质量") },
                icon = { Icon(Icons.Rounded.HighQuality, null) },
                selectedValue = musicQuality,
                onValueSelected = onMusicQualityChange,
                valueText = { "${it.text} ${it.explanation}" }
            )
        }
    }
}
