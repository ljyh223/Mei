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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.ui.screen.index.home.HomeViewModel
import com.ljyh.mei.utils.largeImage
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecommendCard(
    cover: String,
    title: String? = null,
    extInfo: CardExtInfo,
    showPlay: Boolean = false,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    val loader = rememberNetworkLoader()
    val dominantColorState = rememberDominantColorState(loader)
    val isColorLoaded = remember(cover) { mutableStateOf(false) }

    // 颜色提取逻辑 (保持你原有的数据库缓存逻辑，很好)
    LaunchedEffect(cover) {
        if (isColorLoaded.value) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val cachedColor = viewModel.getColors(cover)
            if (cachedColor != null) {
                withContext(Dispatchers.Main) {
                    dominantColorState.updateFrom(Url(cover)) // 这里可能需要优化，直接设置 Color 而不是 Url
                    // 实际上 kmpalette 主要是从 Url 提色，如果有 cachedColor int 值，
                    // 最好直接有一个 state 存储 color，而不是再次调用 loader。
                    // 但为了保持兼容你现有逻辑，先不动 kmpalette 的核心用法。
                    isColorLoaded.value = true
                }
            } else {
                withContext(Dispatchers.Main) {
                    val demoImageUrl = Url(cover)
                    loader.load(demoImageUrl)
                    dominantColorState.updateFrom(demoImageUrl)
                }
                if (dominantColorState.color != Color.Unspecified) {
                    viewModel.addColor(
                        com.ljyh.mei.data.model.room.CacheColor(
                            url = cover,
                            color = dominantColorState.color.toArgb()
                        )
                    )
                    isColorLoaded.value = true
                }
            }
        }
    }

    // 基础颜色，如果没有提取到，使用深灰色兜底
    val baseColor = if (dominantColorState.color != Color.Unspecified) dominantColorState.color else Color.DarkGray

    Column(
        modifier = Modifier
            .width(RecommendCardWidth)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        // 图片区域
        Box(
            modifier = Modifier
                .size(RecommendCardWidth, RecommendCardHeight)
        ) {
            AsyncImage(
                model = cover.largeImage(),
                modifier = Modifier.matchParentSize(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

            // 顶部渐变遮罩 (增强文字可读性)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.6f),
                                baseColor.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 200f // 仅覆盖顶部
                        )
                    )
            )

            // 顶部左上角图标+文字
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (extInfo.icon != null) {
                    AsyncImage(
                        model = extInfo.icon,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = extInfo.text,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (showPlay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                )
            }
        }

        // 底部标题区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            baseColor.copy(alpha = 0.9f), // 稍微透明一点，更有质感
                            baseColor
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = title ?: "",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
data class CardExtInfo(val icon: String?, val text: String)