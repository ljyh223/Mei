package com.ljyh.mei.ui.component.card

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.PlaylistCardSize

@Composable
fun PlaylistCard(
    id: String,
    title: String,
    coverImg: String,
    showPlay: Boolean = false,
    subTitle: List<String>? = null,
    extInfo: String? = null,
    imageSize: Boolean = true,
    cardSize: Dp = PlaylistCardSize,
    modifier: Modifier = Modifier.width(cardSize),
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val textShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(2f, 2f), blurRadius = 4f)
    ArtworkCard(
        title = title, coverUrl = coverImg, modifier = modifier,
        isLoading = isLoading, resizeImage = imageSize, onClick = onClick,
        coverContent = {
            // 顶部播放量
            if (!isLoading && extInfo != null) {
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
            if (!isLoading && subTitle != null) {
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
            if (!isLoading && showPlay) {
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
        },
    )
}
