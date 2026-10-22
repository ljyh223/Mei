package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.theme.MeiCategory
import com.ljyh.mei.ui.theme.MeiCategoryIcon
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun BentoDashboard(onHistoryClick: () -> Unit, onLocalClick: () -> Unit, onDownloadClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryShortcut("最近播放", Icons.Rounded.History, MeiCategory.Content,
            onHistoryClick, Modifier.weight(1f))
        LibraryShortcut("本地音乐", Icons.Rounded.Folder, MeiCategory.Offline,
            onLocalClick, Modifier.weight(1f))
        LibraryShortcut("下载管理", Icons.Rounded.Download, MeiCategory.Storage,
            onDownloadClick, Modifier.weight(1f))
    }
}

@Composable
private fun LibraryShortcut(
    label: String, icon: ImageVector, category: MeiCategory, onClick: () -> Unit, modifier: Modifier,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 100.dp).padding(horizontal = 4.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MeiCategoryIcon(icon, category, size = 32.dp)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
