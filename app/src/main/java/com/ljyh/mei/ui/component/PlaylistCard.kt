package com.ljyh.mei.ui.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.ljyh.mei.ui.local.LocalNavController
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
//    val playerConnection = LocalPlayerConnection.current ?: return
//    val isPlaying by playerConnection.isPlaying.collectAsState()
//    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalNavController.current.context
    Row {
        Column(modifier = Modifier
            .width(PlaylistCardSize)
            .clickable { onClick() }) {
            Box {
                AsyncImage(
                    model = if (imageSize) {
                        coverImg.largeImage()
                    } else {
                        coverImg
                    },
                    modifier = Modifier
                        .size(PlaylistCardSize)
                        .clip(RoundedCornerShape(8.dp)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
                subTitle?.let {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                    ) {
                        it.forEach { t ->
                            Text(
                                text = t,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 16.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(4f, 4f),
                                        blurRadius = 8f
                                    )
                                )
                            )
                        }
                    }
                }

                extInfo?.let {

                    Row(
                        Modifier
                            .height(IntrinsicSize.Min)
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)

                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headset,
                            contentDescription = "Headset",
                            modifier = Modifier
                                .size(12.dp)
                                .shadow(
                                    4.dp,
                                    RoundedCornerShape(4.dp)
                                ),
                            tint = Color.White
                        )
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            maxLines = 1,
                            lineHeight = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(4f, 4f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }

                }



                if (showPlay) {
                    IconButton(modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .padding(4.dp),
                        onClick = {
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.shadow(
                                4.dp,
                                RoundedCornerShape(4.dp)
                            ),
                        )
                    }
                }


            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))
    }


}