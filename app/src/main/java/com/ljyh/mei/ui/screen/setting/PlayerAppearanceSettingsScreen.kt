package com.ljyh.mei.ui.screen.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ljyh.mei.constants.AlwaysExpandMiniPlayerKey
import com.ljyh.mei.constants.HidePlayerTitleBracketsKey
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

@Composable
internal fun PlayerAppearanceSettingsScreen() {
    val navController = LocalNavController.current
    val (alwaysExpandMiniPlayer, onAlwaysExpandMiniPlayerChange) = rememberPreference(
        AlwaysExpandMiniPlayerKey, defaultValue = false
    )
    val (hideTitleBrackets, onHideTitleBracketsChange) = rememberPreference(
        HidePlayerTitleBracketsKey, defaultValue = false
    )
    val (progressBarStyle, onProgressBarStyleChange) = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.LINEAR
    )
    val (meshFlowSpeed, onMeshFlowSpeedChange) = rememberPreference(
        MeshFlowSpeedKey, defaultValue = 0.25f
    )
    val (meshStaticMode, onMeshStaticModeChange) = rememberPreference(
        MeshStaticModeKey, defaultValue = false
    )
    val (meshPlaying, onMeshPlayingChange) = rememberPreference(
        MeshPlayingKey, defaultValue = true
    )
    AppearanceSettingsPage("播放界面") {
        PreferenceSection("播放控件") {
            SwitchPreference(
                title = { Text("始终展开底部播放条") },
                description = "保持播放条与底部导航展开，不随页面滑动收起",
                icon = { Icon(Icons.Rounded.MusicVideo, null) },
                checked = alwaysExpandMiniPlayer,
                onCheckedChange = onAlwaysExpandMiniPlayerChange,
            )
            SwitchPreference(
                title = { Text("隐藏歌名括号内容") },
                description = "在迷你播放条和完整播放器标题中隐藏中英文括号及其中内容",
                icon = { Icon(Icons.Rounded.FormatBold, null) },
                checked = hideTitleBrackets,
                onCheckedChange = onHideTitleBracketsChange,
            )
            EnumListPreference(
                title = { Text("进度条样式") },
                icon = { Icon(Icons.Rounded.LinearScale, null) },
                selectedValue = progressBarStyle,
                onValueSelected = onProgressBarStyleChange,
                valueText = { it.label }
            )
        }
        PreferenceSection("流动背景") {
            ListPreference(
                title = { Text("流动速度") },
                description = "渐变动画速度，值越大流动越快",
                icon = { Icon(Icons.Rounded.Speed, null) },
                selectedValue = meshFlowSpeed,
                values = listOf(0.05f, 0.10f, 0.15f, 0.25f, 0.5f),
                onValueSelected = onMeshFlowSpeedChange,
                valueText = {
                    when (it) {
                        0.05f -> "极慢"
                        0.10f -> "慢"
                        0.15f -> "标准"
                        0.25f -> "快"
                        0.5f -> "极快"
                        else -> "${(it * 100).toInt()}%"
                    }
                }
            )

            SwitchPreference(
                title = { Text("静态模式") },
                description = "过渡完成后停止动画，省电",
                icon = { Icon(Icons.Rounded.Timer, null) },
                checked = meshStaticMode,
                onCheckedChange = onMeshStaticModeChange
            )
            SwitchPreference(
                title = { Text("播放动画") },
                description = "暂停/恢复背景渲染",
                icon = { Icon(Icons.Rounded.MusicVideo, null) },
                checked = meshPlaying,
                onCheckedChange = onMeshPlayingChange
            )
            PreferenceEntry(
                title = { Text("高级设置") },
                description = "渲染精度与网格细分",
                icon = { Icon(Icons.Rounded.Tune, null) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                onClick = { Screen.BackgroundAdvancedSettings.navigate(navController) },
            )
        }
    }
}
