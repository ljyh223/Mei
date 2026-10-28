package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.DefaultTtmlLyricsBaseUrl
import com.ljyh.mei.constants.QqLyricApi
import com.ljyh.mei.constants.QqLyricApiKey
import com.ljyh.mei.constants.QqTimeout
import com.ljyh.mei.constants.QqTimeoutKey
import com.ljyh.mei.constants.TtmlLyricsBaseUrlKey
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.component.AudioSourceModePreference
import com.ljyh.mei.ui.screen.setting.component.EditTextPreference
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.utils.preferences.AudioSourceMode
import com.ljyh.mei.utils.preferences.AudioSourceModeKey
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun SourceAndLyricsSettingsScreen() {
    val navController = LocalNavController.current
    val (ttmlLyricsBaseUrl, onTtmlLyricsBaseUrlChange) = rememberPreference(
        TtmlLyricsBaseUrlKey, defaultValue = DefaultTtmlLyricsBaseUrl,
    )
    MeiPinnedPage(title = "音源与平台歌词", onNavigateBack = navController::navigateUp) { paddingValues ->
        Column(Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!).padding(paddingValues)) {
            MeiLargeTitle("音源与平台歌词")
            PreferenceSection("其他音源与歌词") {
                val (audioSourceMode, onAudioSourceModeChange) = rememberEnumPreference(AudioSourceModeKey, AudioSourceMode.Lx)
                AudioSourceModePreference(audioSourceMode, onAudioSourceModeChange)
                PreferenceEntry(
                    title = { Text("LX 自定义音源") },
                    description = "管理脚本与优先级",
                    icon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                    onClick = { Screen.LxSources.navigate(navController) },
                )
                val (qqLyricApi, onQqLyricApi) = rememberEnumPreference(QqLyricApiKey, QqLyricApi.Auto)
                ListPreference(
                    title = { Text("QQ 搜索与歌词接口") },
                    description = "选择 QQ 搜索和歌词使用的接口，切换到下一次请求生效",
                    icon = { Icon(Icons.Rounded.Lyrics, "QQ 搜索与歌词接口") },
                    selectedValue = qqLyricApi,
                    values = QqLyricApi.entries.toList(),
                    valueText = { it.label },
                    onValueSelected = onQqLyricApi,
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
}
