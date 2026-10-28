package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.UsbExclusivePlaybackKey
import com.ljyh.mei.playback.UsbOutputState
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.utils.preferences.rememberPreference

@Composable
fun AudioOutputSettingsScreen() {
    val navController = LocalNavController.current
    val (usbExclusive) = rememberPreference(UsbExclusivePlaybackKey, false)
    val usbStatus by UsbOutputState.status.collectAsState()
    MeiPinnedPage(title = "音频输出与音效", onNavigateBack = navController::navigateUp) { paddingValues ->
        Column(Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!).padding(paddingValues)) {
            MeiLargeTitle("音频输出与音效")
            PreferenceSection("配置") {
                PreferenceEntry(
                    title = { Text("USB 独占播放") },
                    description = if (usbExclusive) usbStatus.message.ifEmpty { "已开启" } else "已关闭 · 输出、缓冲与后台运行设置",
                    icon = { Icon(Icons.Rounded.Usb, null) },
                    onClick = { Screen.UsbExclusiveSettings.navigate(navController) },
                )
                PreferenceEntry(
                    title = { Text("均衡器") },
                    description = if (usbExclusive) "USB 独占播放时不可用" else "参数均衡器、频响曲线与预设",
                    icon = { Icon(Icons.Rounded.Equalizer, null) },
                    onClick = { Screen.Equalizer.navigate(navController) },
                    isEnabled = !usbExclusive,
                )
                PreferenceEntry(
                    title = { Text("混响") },
                    description = if (usbExclusive) "USB 独占播放时不可用" else "房间、大厅、板式与自定义参数",
                    icon = { Icon(Icons.Rounded.Waves, null) },
                    onClick = { Screen.ReverbSettings.navigate(navController) },
                    isEnabled = !usbExclusive,
                )
            }
        }
    }
}
