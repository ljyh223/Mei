package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.utils.image.saveImageToGallery
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.size1600
import com.ljyh.mei.utils.smallImage
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun Cover(
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
) {
    val context = LocalContext.current
    // 获取当前的封面样式设置
    val coverStyle by rememberEnumPreference(CoverStyleKey, defaultValue = CoverStyle.Square)
    val originalCover by rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )

    val coverShape = if (coverStyle == CoverStyle.Circle) {
        CircleShape
    } else {
        RoundedCornerShape(12.dp)
    }

    var showFullImage by remember { mutableStateOf(false) }

    AnimatedContent(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = coverShape
        ),
        targetState = mediaMetadata.coverUrl,
        transitionSpec = {
            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
        },
        label = "CoverTransition"
    ) { url ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .shadow(elevation = 12.dp, shape = coverShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    playerConnection.player.seekBack()
                                } else {
                                    playerConnection.player.seekForward()
                                }
                            },
                            onLongPress = {
                                showFullImage = true
                            }
                        )
                    }

                ,
                shape = coverShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .placeholderMemoryCacheKey(url.smallImage())
                        .data(if (originalCover) url else url.size1600())
                        .crossfade(false)
                        .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = "Loaded Image",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showFullImage) {
            val highResUrl = if (originalCover) url else url.size1600()
            FullScreenImageViewer(
                imageUrl = highResUrl,
                onDismiss = { showFullImage = false }
            )
        }
    }
}

/**
 * 全屏图片查看器组件
 * 支持：双指缩放、拖拽、保存图片
 */
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()



    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 全屏关键
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)

                    .crossfade(true)
                    .build(),
                contentDescription = "Full Screen Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(rememberZoomState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }


            Box(
                modifier =  Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ){
                // 保存按钮
                IconButton(
                    onClick = {
                        scope.launch {
                            saveImageToGallery(context, imageUrl)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
