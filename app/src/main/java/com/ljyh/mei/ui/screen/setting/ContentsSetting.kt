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
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
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
import com.ljyh.mei.constants.AiTriggerMode
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.AiTriggerModeKey
import com.ljyh.mei.constants.AiBaseUrlKey
import com.ljyh.mei.constants.AiApiKeyKey
import com.ljyh.mei.constants.AiModelKey
import com.ljyh.mei.constants.QqTimeout
import com.ljyh.mei.constants.QqTimeoutKey
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
import com.ljyh.mei.utils.rememberPreference

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

            PreferenceGroupTitle(
                title = "AI 歌词"
            )

            val (aiMode, onAiModeChange) = rememberPreference(
                AiTriggerModeKey,
                defaultValue = AiTriggerMode.Off.name
            )
            val currentMode = try { AiTriggerMode.valueOf(aiMode) } catch (_: Exception) { AiTriggerMode.Off }

            ListPreference(
                title = { Text("触发模式") },
                description = null,
                icon = { Icon(Icons.Rounded.AutoAwesome, "AI 触发模式") },
                selectedValue = currentMode,
                values = AiTriggerMode.entries.toList(),
                valueText = { mode ->
                    when (mode) {
                        AiTriggerMode.Off -> "关闭"
                        AiTriggerMode.OnMissing -> "缺译时"
                        AiTriggerMode.Always -> "始终"
                    }
                },
                onValueSelected = { onAiModeChange(it.name) }
            )

            val (qqTimeout, onQqTimeoutChange) = rememberPreference(
                QqTimeoutKey,
                defaultValue = QqTimeout.Sec8.name
            )
            val currentTimeout = try { QqTimeout.valueOf(qqTimeout) } catch (_: Exception) { QqTimeout.Sec8 }

            ListPreference(
                title = { Text("QQ 超时") },
                description = null,
                icon = { Icon(Icons.Rounded.TipsAndUpdates, "QQ 超时") },
                selectedValue = currentTimeout,
                values = QqTimeout.entries.toList(),
                valueText = { it.label },
                onValueSelected = { onQqTimeoutChange(it.name) }
            )

            val (aiBaseUrl, onAiBaseUrlChange) = rememberPreference(
                AiBaseUrlKey,
                defaultValue = ""
            )
            EditTextPreference(
                title = { Text("API 地址") },
                icon = { Icon(Icons.Rounded.Link, "API 地址") },
                value = aiBaseUrl,
                onValueChange = onAiBaseUrlChange
            )

            val (aiApiKey, onAiApiKeyChange) = rememberPreference(
                AiApiKeyKey,
                defaultValue = ""
            )
            EditTextPreference(
                title = { Text("API Key") },
                icon = { Icon(Icons.Rounded.Key, "API Key") },
                value = aiApiKey,
                onValueChange = onAiApiKeyChange
            )

            val (aiModel, onAiModelChange) = rememberPreference(
                AiModelKey,
                defaultValue = ""
            )
            EditTextPreference(
                title = { Text("模型") },
                icon = { Icon(Icons.Rounded.Memory, "模型") },
                value = aiModel,
                onValueChange = onAiModelChange
            )
        }
    }
}