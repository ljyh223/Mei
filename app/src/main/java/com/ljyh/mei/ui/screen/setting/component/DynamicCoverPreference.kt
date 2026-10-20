package com.ljyh.mei.ui.screen.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.constants.DynamicCoverSettings

@Composable
internal fun DynamicCoverPreference(
    settings: DynamicCoverSettings,
    englishTitlesOnly: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onAppleMusicChange: (Boolean) -> Unit,
    onNeteaseChange: (Boolean) -> Unit,
    onEnglishTitlesOnlyChange: (Boolean) -> Unit,
) {
    // Keep the source switches on the same surface as the master switch. Their text
    // starts at the master's title, while a further inset marks the Apple-only option.
    Column(Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        SwitchPreference(
            title = { Text("动态封面") },
            description = "省电模式或低电量时暂停加载",
            icon = { Icon(Icons.Rounded.MusicVideo, contentDescription = null) },
            checked = settings.enabled,
            onCheckedChange = onEnabledChange,
        )

        if (settings.enabled) {
            SwitchPreference(
                title = { Text("Apple Music") },
                icon = {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.am),
                        contentDescription = null
                    )
                },
                checked = settings.selectedSources.appleMusic,
                onCheckedChange = onAppleMusicChange,
            )
            if (settings.selectedSources.appleMusic) {
                SwitchPreference(
                    title = {
                        Text(
                            "仅匹配英文专辑",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    icon = { Icon(Icons.Rounded.Title, contentDescription = null) },
                    checked = englishTitlesOnly,
                    onCheckedChange = onEnglishTitlesOnlyChange,
                )
            }
            SwitchPreference(

                title = { Text("网易云") },
                icon = {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.netease),
                        contentDescription = null
                    )
                },
                checked = settings.selectedSources.netease,
                onCheckedChange = onNeteaseChange,
            )
        }
    }
}
