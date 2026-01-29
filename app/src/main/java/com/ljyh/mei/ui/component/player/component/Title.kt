package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.room.Like

@Composable
fun Title(
    title: String,
    subTitle: String,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    subTitleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    needShadow: Boolean = true,
    iconColor: Color = Color.White // 新增：控制图标颜色，方便在不同背景下调整
) {
    val shadowStyle = if (needShadow) Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    ) else null

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically, // 垂直居中
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 文字区域：使用 weight(1f) 占据剩余空间
        Column(
            modifier = Modifier.weight(1f)
                .clickable(onClick = onTitleClick),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = titleStyle.copy(
                    shadow = shadowStyle,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subTitle,
                style = subTitleStyle.copy(
                    shadow = shadowStyle,
                    color = Color.White.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 2. 按钮区域
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Like Button
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else iconColor.copy(alpha = 0.8f), // 喜欢时变红
                    modifier = Modifier.size(24.dp) // 稍微调整大小适配不同高度
                )
            }

            // More Button
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert, // 或者 MoreHoriz
                    contentDescription = "More",
                    tint = iconColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}