package com.ljyh.mei.ui.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.ui.screen.main.home.HomeViewModel
import com.ljyh.mei.utils.largeImage

@Composable
fun RecommendCard(
    cover: String,
    title: String? = null,
    extInfo: CardExtInfo,
    showPlay: Boolean = false,
    cardWidth: Dp = RecommendCardWidth,
    cardHeight: Dp = RecommendCardHeight,
    viewModel: HomeViewModel,
    onPlayClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    var extractedColor by remember(cover) {
        mutableStateOf(viewModel.getCachedColor(cover) ?: Color.DarkGray)
    }

    LaunchedEffect(cover) {
        extractedColor = viewModel.getOrExtractColor(cover)
    }

    val baseColor by animateColorAsState(extractedColor, label = "recommendCardColor")

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = cover.largeImage(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(cardHeight),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        // 信息叠在封面上；底部渐变覆盖同一张图，因此没有图片与信息区的物理边界。
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            baseColor.copy(alpha = 0.86f),
                            baseColor
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.orEmpty(),
                        maxLines = 1,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        extInfo.icon?.let { icon ->
                            AsyncImage(
                                model = icon,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = extInfo.text,
                            maxLines = 1,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showPlay || onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick ?: onClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "播放",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
data class CardExtInfo(val icon: String?=null, val text: String)
