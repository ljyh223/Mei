package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.LoopPlaybackKey
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.constants.PreviousPlaybackKey
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

    val (musicQuality, onMusicQualityChange) = rememberEnumPreference(
        key = MusicQualityKey,
        defaultValue = MusicQuality.EXHIGH,
    )
    val (loopPlayback, onLoopPlaybackChange) = rememberPreference(
        key = LoopPlaybackKey,
        defaultValue = true
    )

    val (previousPlayback, onPreviousPlaybackChange) = rememberPreference(
        key = PreviousPlaybackKey,
        defaultValue = true
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
                title = { Text("循环播放") },
                icon = { Icon(Icons.Rounded.Loop, null) },
                checked = loopPlayback,
                onCheckedChange = onLoopPlaybackChange
            )
            SwitchPreference(
                title = { Text("上一首切换逻辑")},
                description = "关闭时，切换上一首如果大于3秒，会重头开始播放",
                icon = { Icon(Icons.Rounded.SkipPrevious, null) },
                checked = previousPlayback,
                onCheckedChange = onPreviousPlaybackChange
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