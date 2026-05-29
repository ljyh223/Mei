package com.ljyh.mei.ui.screen.local.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType

@Composable
internal fun LibraryStats(songs: List<Song>) {
    val songCount = songs.size
    val downloadCount = songs.count { it.sourceType == SourceType.DOWNLOAD }
    val localCount = songs.count { it.sourceType == SourceType.LOCAL }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("全部", "$songCount 首", Icons.Rounded.MusicNote, Color(0xFF4CAF50))
        StatCard("已下载", "$downloadCount 首", Icons.Rounded.LibraryMusic, Color(0xFF2196F3))
        if (localCount > 0) {
            StatCard("已导入", "$localCount 首", Icons.Rounded.Folder, Color(0xFFFF9800))
        }
    }
}

@Composable
internal fun RowScope.StatCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
