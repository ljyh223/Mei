package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage


@Composable
fun BentoDashboard(
    lastPlayedCover: String?,
    onHistoryClick: () -> Unit,
    onLocalClick: () -> Unit,
    onCloudClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val cardHeight = 190.dp // 仪表盘总高度

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 左侧：巨大的最近播放入口 (占 60% 宽度) ---
        Card(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .clickable { onHistoryClick() },
            shape = RoundedCornerShape(28.dp),
            // 使用 SurfaceVariant 色调，避免和背景混淆
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 封面图背景
                if (!lastPlayedCover.isNullOrEmpty()) {
                    AsyncImage(
                        model = lastPlayedCover,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.8f),
                        contentScale = ContentScale.Crop
                    )
                    // 黑色渐变，保证文字清晰
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )
                }

                // 内容
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "最近播放",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "继续聆听", // 以后可以换成具体的歌名
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 装饰图标
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                )
            }
        }

        // --- 右侧：功能堆叠 (占 40% 宽度) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 右上：本地音乐
            DashboardSmallCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Folder,
                label = "本地音乐",
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = onLocalClick
            )

            // 右下：下载/云盘 (两个小按钮放在一行)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardSmallCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Cloud,
                    label = "云盘",
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    onClick = onCloudClick,
                    hideLabel = true
                )
                DashboardSmallCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download,
                    label = "下载",
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    onClick = onDownloadClick,
                    hideLabel = true
                )
            }
        }
    }
}


@Composable
fun DashboardSmallCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    hideLabel: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (hideLabel) 32.dp else 28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (!hideLabel) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}



@Composable
fun BentoLargeCard(
    lastPlayedCover: String?,
    onClick: () -> Unit,
){
    Card(
        modifier = Modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        // 使用 SurfaceVariant 色调，避免和背景混淆
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 封面图背景
            if (!lastPlayedCover.isNullOrEmpty()) {
                AsyncImage(
                    model = lastPlayedCover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.8f),
                    contentScale = ContentScale.Crop
                )
                // 黑色渐变，保证文字清晰
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                startY = 100f
                            )
                        )
                )
            }

            // 内容
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最近播放",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "继续聆听", // 以后可以换成具体的歌名
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // 装饰图标
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
            )
        }
    }
}