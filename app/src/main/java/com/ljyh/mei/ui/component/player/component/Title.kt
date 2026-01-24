package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

@Composable
fun Title(
    title: String,
    subTitle: String,
    modifier: Modifier = Modifier, // 支持外部传入 Modifier
    // 默认使用大字体 (主界面模式)
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    subTitleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    needShadow: Boolean = true // 可选：控制是否需要阴影
) {
    val shadowStyle = if (needShadow) Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    ) else null

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start // 确保左对齐
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
}