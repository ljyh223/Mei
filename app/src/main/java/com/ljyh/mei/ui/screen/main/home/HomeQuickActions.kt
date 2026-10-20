package com.ljyh.mei.ui.screen.main.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.theme.MeiCategory
import com.ljyh.mei.ui.theme.MeiCategoryIcon

@Composable
internal fun HomeQuickActions(onDaily: () -> Unit, onAudioMatch: () -> Unit, onLocal: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val actions = listOf(
            Triple("每日推荐", Icons.Rounded.CalendarToday, MeiCategory.Playback) to onDaily,
            Triple("听歌识曲", Icons.Rounded.GraphicEq, MeiCategory.Appearance) to onAudioMatch,
            Triple("本地音乐", Icons.Rounded.LibraryMusic, MeiCategory.Offline) to onLocal,
        )
        actions.forEach { (entry, action) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.clickable(onClick = action).heightIn(min = 96.dp)
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MeiCategoryIcon(entry.second, entry.third, size = 32.dp)
                    Text(entry.first, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun HomeStatus(title: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onRetry) { Text("重新加载") }
    }
}
