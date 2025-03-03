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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.ljyh.music.constants.RecommendCardHeight
import com.ljyh.music.constants.RecommendCardWidth
import com.ljyh.music.ui.screen.index.home.HomeViewModel
import com.ljyh.music.utils.largeImage
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecommendCard(
    cover: String,
    title: String?=null,
    extInfo: CardExtInfo,
    showPlay: Boolean = false,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val loader = rememberNetworkLoader()
    val dominantColorState = rememberDominantColorState(loader)
    val isColorLoaded = remember { mutableStateOf(false) } // 记录颜色是否已从数据库加载
    LaunchedEffect(cover) {

        withContext(Dispatchers.IO){
            val cachedColor = viewModel.getColors(cover)
            if (cachedColor != null) {
                withContext(Dispatchers.Main) {
                    dominantColorState.updateFrom(Url(cover))
                    isColorLoaded.value = true // 颜色已加载，避免重复存储
                }
            } else {
                withContext(Dispatchers.Main) {
                    val demoImageUrl = Url(cover)
                    loader.load(demoImageUrl)
                    dominantColorState.updateFrom(demoImageUrl)
                    isColorLoaded.value = false // 颜色未加载，将在下一个 LaunchedEffect 存入数据库
                }
            }
        }

    }
    // 颜色计算完成后存入数据库
    LaunchedEffect(dominantColorState.color) {
        if (!isColorLoaded.value) {
            withContext(Dispatchers.IO){
                viewModel.addColor(
                    com.ljyh.music.data.model.room.Color(
                        url = cover,
                        color = dominantColorState.color.toArgb()
                    )
                )
            }
            isColorLoaded.value = true
        }
    }
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
                    model = cover.largeImage(),
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
                            model = it,
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
                                offset = Offset(4f, 4f),
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
                                Color((dominantColorState.color.toArgb() and 0xFFFFFF) or 0xD1000000.toInt()), // 与一个 82% 不透明的白色，做 centerColor。注意此处位运算的改变
                                Color(dominantColorState.color.toArgb() or 0xFF000000.toInt()) // 确保是不透明的颜色
                            )
                        )
                    )
            ) {
                Text(
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    text = title?:"",
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