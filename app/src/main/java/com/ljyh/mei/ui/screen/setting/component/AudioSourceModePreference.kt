package com.ljyh.mei.ui.screen.setting.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ljyh.mei.utils.preferences.AudioSourceMode

@Composable
fun AudioSourceModePreference(mode: AudioSourceMode, onModeSelected: (AudioSourceMode) -> Unit) {
    ListPreference(
        title = { Text("备用音源") },
        description = "${mode.label} · 下次获取播放地址时生效",
        icon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
        selectedValue = mode,
        values = AudioSourceMode.entries.toList(),
        valueText = { it.label },
        onValueSelected = onModeSelected,
    )
}
