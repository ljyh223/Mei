package com.ljyh.mei.ui.component.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.utils.largeImage

@Composable
fun PlaylistCard(
    id: String,
    title: String,
    coverImg: String,
    showPlay: Boolean = false,
    subTitle: List<String>? = null,
    extInfo: String? = null,
    imageSize: Boolean = true,
    onClick: () -> Unit
) {
    // 常用阴影样式，提取出来复用
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.7f),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )

    Column(
        modifier = Modifier
            .width(PlaylistCardSize)
            .clip(RoundedCornerShape(8.dp)) // 整个组件裁切，防止水波纹溢出
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(PlaylistCardSize)
        ) {
            AsyncImage(
                model = if (imageSize) coverImg.largeImage() else coverImg,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

            // 顶部播放量
            if (extInfo != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headset,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = extInfo,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }

            // 底部左侧副标题 (如果有)
            if (subTitle != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    subTitle.forEach { t ->
                        Text(
                            text = t,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(shadow = textShadow)
                        )
                    }
                }
            }

            // 右下角播放按钮
            if (showPlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 标题
        Text(
            text = title,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            fontSize = 13.sp, // 调整字体大小，更精致
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}