package com.ljyh.mei.ui.component.player.component.applemusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Title(
    title: String,
    subTitle: String,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    subTitleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    needShadow: Boolean = true,
    titleColor: Color = Color.White,
    subTitleColor: Color = Color.White.copy(alpha = 0.7f),
    iconColor: Color = Color.White,
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
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subTitle,
                style = subTitleStyle.copy(
                    shadow = shadowStyle,
                ),
                color = subTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 2. 按钮区域
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val actionButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = iconColor.copy(alpha = 0.14f),
                contentColor = iconColor,
            )

            FilledTonalIconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(40.dp),
                colors = actionButtonColors,
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Like",
                    tint = iconColor.copy(alpha = if (isLiked) 1f else 0.8f),
                    modifier = Modifier.size(22.dp),
                )
            }

            FilledTonalIconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp),
                colors = actionButtonColors,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = iconColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
