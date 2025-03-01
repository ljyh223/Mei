package com.ljyh.music.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.music.constants.CookieKey
import com.ljyh.music.constants.UseQQMusicLyricKey
import com.ljyh.music.ui.component.EditTextPreference
import com.ljyh.music.ui.component.IconButton
import com.ljyh.music.ui.component.PreferenceGroupTitle
import com.ljyh.music.ui.component.SwitchPreference
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.screen.backToMain
import com.ljyh.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSetting(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current
    val (useQQMusicLyric, onUseQQMusicLyricChange) = rememberPreference(
        UseQQMusicLyricKey,
        defaultValue = false
    )
    val (cookie, onCookie) = rememberPreference(
        CookieKey,
        defaultValue = ""
    )

    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text("内容设置") },
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
                title = "MUSIC"
            )
            SwitchPreference(
                title = { Text("启用QQ音乐歌词") },
                icon = { Icon(Icons.Rounded.Lyrics, null) },
                checked = useQQMusicLyric,
                onCheckedChange = onUseQQMusicLyricChange
            )
            EditTextPreference(
                title = { Text("网易云Cookie: MUSIC_U") },
                icon = { Icon(Icons.Rounded.Cookie, null) },
                value = cookie,
                onValueChange = onCookie
            )
        }
    }


}