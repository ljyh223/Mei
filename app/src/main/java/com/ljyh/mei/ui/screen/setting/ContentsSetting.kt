package com.ljyh.mei.ui.screen.setting

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.constants.AutoMatchQQMusicLyricKey
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.MatchSuccessToastKey
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.ShareViewModel
import com.ljyh.mei.ui.component.EditTextPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.PreferenceEntry
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSetting(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val (useQQMusicLyric, onUseQQMusicLyricChange) = rememberPreference(
        UseQQMusicLyricKey,
        defaultValue = true
    )

    val (autoMatchQQMusicLyric, onAutoMatchQQMusicLyricChange) = rememberPreference(
        AutoMatchQQMusicLyricKey,
        defaultValue = false
    )

    val (matchSuccessToast, onMatchSuccessToastChange) = rememberPreference(
        MatchSuccessToastKey,
        defaultValue = true
    )
    val (cookie, onCookie) = rememberPreference(
        CookieKey,
        defaultValue = ""
    )
    var userName by remember { mutableStateOf("") }
    val userAccount by viewModel.userAccount.collectAsState()

    userName = when (val result = userAccount) {
        is Resource.Success -> {
            if(result.data.code == 200 && result.data.profile != null){
                Toast.makeText(context, "看上去还不错哦", Toast.LENGTH_SHORT).show()
                result.data.profile.nickname
            }else{
                Toast.makeText(context, "cookie 可能存在错误", Toast.LENGTH_SHORT).show()
                "error"
            }

        }

        is Resource.Error -> "error"
        Resource.Loading -> "~~~"
    }
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
                icon = { Icon(Icons.Rounded.Lyrics, "启用QQ音乐歌词") },
                checked = useQQMusicLyric,
                onCheckedChange = onUseQQMusicLyricChange
            )


            SwitchPreference(
                title = { Text("自动匹配歌词") },
                description = "源自LDDC项目, 试验性测试功能(未广泛测试)",
                icon = { Icon(Icons.Rounded.AutoAwesome, "自动匹配歌词") },
                checked = autoMatchQQMusicLyric,
                onCheckedChange = onAutoMatchQQMusicLyricChange
            )

            SwitchPreference(
                title = { Text("匹配成功提示") },
                icon = { Icon(Icons.Rounded.TipsAndUpdates, "匹配成功提示") },
                isEnabled = autoMatchQQMusicLyric,
                checked = matchSuccessToast,
                onCheckedChange = onMatchSuccessToastChange
            )



            EditTextPreference(
                title = { Text("网易云Cookie: MUSIC_U") },
                icon = { Icon(Icons.Rounded.Cookie, "网易云Cookie: MUSIC_U") },
                value = cookie,
                onValueChange = onCookie
            )

            PreferenceEntry(
                title = { Text("测试Cookie") },
                description = userName,
                icon = { Icon(Icons.Rounded.TipsAndUpdates, "测试 Cookie") },
                onClick = {
                    if (cookie == "") {
                        Toast.makeText(context, "还没有填写cookie", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.getUserAccount()
                    }

                }
            )


        }
    }


}