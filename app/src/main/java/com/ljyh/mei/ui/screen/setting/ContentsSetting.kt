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
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.DefaultTtmlLyricsBaseUrl
import com.ljyh.mei.constants.QqTimeout
import com.ljyh.mei.constants.QqCookieKey
import com.ljyh.mei.constants.QqTimeoutKey
import com.ljyh.mei.constants.TtmlLyricsBaseUrlKey
import com.ljyh.mei.data.model.auth.QrLoginUiState
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.ShareViewModel
import com.ljyh.mei.ui.component.EditTextPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.ListPreference
import com.ljyh.mei.ui.component.PreferenceEntry
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.preferences.rememberPreference
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSetting(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val (cookie, onCookie) = rememberPreference(
        CookieKey,
        defaultValue = ""
    )
    var showQrLogin by rememberSaveable { mutableStateOf(false) }
    val userAccount by viewModel.userAccount.collectAsState()
    val qrLoginState by viewModel.qrLoginState.collectAsState()

    val userName = when (val result = userAccount) {
        is Resource.Success -> if (result.data.code == 200 && result.data.profile != null) {
            result.data.profile.nickname
        } else {
            "error"
        }

        is Resource.Error -> "error"
        Resource.Loading -> "~~~"
    }

    LaunchedEffect(userAccount, qrLoginState) {
        val result = userAccount
        if (result is Resource.Success && qrLoginState !is QrLoginUiState.Success) {
            val valid = result.data.code == 200 && result.data.profile != null
            Toast.makeText(
                context,
                if (valid) "看上去还不错哦" else "cookie 可能存在错误",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(qrLoginState) {
        if (qrLoginState is QrLoginUiState.Success) {
            val nickname = (qrLoginState as QrLoginUiState.Success).nickname
            Toast.makeText(
                context,
                nickname?.takeIf(String::isNotBlank)?.let { "登录成功，欢迎回来，$it" } ?: "登录成功",
                Toast.LENGTH_SHORT,
            ).show()
            showQrLogin = false
        }
    }

    if (showQrLogin) {
        QrLoginDialog(
            state = qrLoginState,
            onRefresh = viewModel::refreshQrLogin,
            onDismiss = {
                viewModel.stopQrLogin()
                showQrLogin = false
            },
        )
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

            EditTextPreference(
                title = { Text("网易云Cookie: MUSIC_U") },
                icon = { Icon(Icons.Rounded.Cookie, "网易云Cookie: MUSIC_U") },
                value = cookie,
                onValueChange = onCookie
            )

            val (ttmlLyricsBaseUrl, onTtmlLyricsBaseUrlChange) = rememberPreference(
                TtmlLyricsBaseUrlKey,
                defaultValue = DefaultTtmlLyricsBaseUrl,
            )


            PreferenceEntry(
                title = { Text("扫码登录") },
                description = "使用网易云音乐 App 扫码",
                icon = { Icon(Icons.Rounded.QrCode2, "扫码登录") },
                onClick = {
                    showQrLogin = true
                    viewModel.startQrLogin()
                },
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

            PreferenceGroupTitle(title = "备用音源")
            val (qqCookie, onQqCookieChange) = rememberPreference(QqCookieKey, defaultValue = "")
            EditTextPreference(
                title = { Text("QQ 音乐 Cookie") },
                icon = { Icon(Icons.Rounded.Cookie, contentDescription = null) },
                value = qqCookie,
                onValueChange = { onQqCookieChange(it.trim()) },
                sensitive = true,
                placeholder = { Text("粘贴 QQ 音乐网页的完整 Cookie") },
                isInputValid = { true },
            )

            val (qqTimeout, onQqTimeoutChange) = rememberPreference(
                QqTimeoutKey,
                defaultValue = QqTimeout.Sec8.name
            )
            val currentTimeout = try {
                QqTimeout.valueOf(qqTimeout)
            } catch (_: Exception) {
                QqTimeout.Sec8
            }

            ListPreference(
                title = { Text("QQ音乐获取歌词超时") },
                description = null,
                icon = { Icon(Icons.Rounded.TipsAndUpdates, "QQ音乐获取歌词超时") },
                selectedValue = currentTimeout,
                values = QqTimeout.entries.toList(),
                valueText = { it.label },
                onValueSelected = { onQqTimeoutChange(it.name) }
            )
            EditTextPreference(
                title = { Text("TTML 歌词服务根地址") },
                icon = { Icon(Icons.Rounded.Lyrics, "TTML 歌词服务根地址") },
                value = ttmlLyricsBaseUrl,
                onValueChange = onTtmlLyricsBaseUrlChange,
                isInputValid = { input ->
                    input.trim().trimEnd('/').toHttpUrlOrNull()?.let {
                        it.query == null && it.fragment == null
                    } == true
                },
            )
        }
    }
}
