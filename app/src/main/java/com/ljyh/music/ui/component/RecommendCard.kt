package com.ljyh.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.ljyh.music.constants.RecommendCardHeight
import com.ljyh.music.constants.RecommendCardWidth
import com.ljyh.music.ui.screen.index.home.HomeViewModel
import com.ljyh.music.utils.ImageUtils.getImageDominantColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun RecommendCard(
    picUrl: String,
    title: String,
    extInfo: CardExtInfo,
    showPlay: Boolean = false,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var colorFlow = remember(picUrl) { MutableStateFlow(Color(0xFF000000)) }
    LaunchedEffect(picUrl) {
        coroutineScope.launch {
            try {
                val newColor = getImageDominantColor(picUrl, context, viewModel.colorRepository)
                colorFlow.value = newColor
            } catch (e: Exception) {
                // 处理异常
            }
        }
    }
    val color by colorFlow.collectAsState()
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp)),
            onClick = onClick,
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(picUrl)
                        .size(Size.ORIGINAL) // 根据需要调整大小
                        .build(),
                    modifier = Modifier
                        .size(RecommendCardWidth, RecommendCardHeight),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )


                Row(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)

                ) {
                    extInfo.icon?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(it).size(Size.ORIGINAL)
                                .build(),
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = extInfo.text,
                        fontSize = 16.sp,
                        maxLines = 1,
                        lineHeight = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )

                }
                if (showPlay) {
                    IconButton(modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .padding(4.dp),
                        onClick = {
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.PlaylistPlay,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                    }
                }


            }

        }

        Surface(
            modifier = Modifier
                .width(RecommendCardWidth)
                .clip(RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
        ) {
            Box(
                Modifier
                    .width(RecommendCardWidth)
                    .clip(RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color((color.toArgb() and 0xFFFFFF) or 0xD1000000.toInt()), // 与一个 82% 不透明的白色，做 centerColor。注意此处位运算的改变
                                Color(color.toArgb() or 0xFF000000.toInt()) // 确保是不透明的颜色
                            )
                        )
                    )
            ) {
                Text(
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    text = title,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }


    }
}


data class CardExtInfo(val icon: String?, val text: String)