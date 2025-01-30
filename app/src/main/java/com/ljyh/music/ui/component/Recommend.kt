package com.ljyh.music.ui.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.ljyh.music.data.model.Recommend
import com.ljyh.music.utils.largeImage
import com.ljyh.music.utils.middleImage


@Composable
fun Recommend(
    recommend: Recommend
) {
    val context = LocalContext.current

    Column {
        Row {
            Surface(
                modifier = Modifier
                    .weight(2f)
                    .aspectRatio(1f)
                    .padding(start = 0.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                    .clickable {
                        
                    },
                tonalElevation = 12.dp,
                shape = RoundedCornerShape(16.dp)

            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text(
                        text = "推荐",
                        fontSize = 48.sp,
                        color = Color(0xFF0091EA)
                    )
                    Text(text = "歌曲", fontSize = 48.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(recommend.data.dailySongs[9].al.picUrl.middleImage())
                        .size(Size.ORIGINAL) // 根据需要调整大小
                        .build(),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                        },
                    contentDescription = null
                )
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(recommend.data.dailySongs[1].al.picUrl.middleImage())
                        .size(Size.ORIGINAL) // 根据需要调整大小
                        .build(),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                        },
                    contentDescription = null
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(recommend.data.dailySongs[2].al.picUrl.largeImage())
                    .size(Size.ORIGINAL) // 根据需要调整大小
                    .build(),
                modifier = Modifier
                    .weight(2f)
                    .aspectRatio(1f)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                    },
                contentDescription = null
            )

        }

        Row {

            for (i in 3..7) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(recommend.data.dailySongs[i].al.picUrl.middleImage())
                        .size(Size.ORIGINAL) // 根据需要调整大小
                        .build(),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                        },
                    contentDescription = null
                )
            }
        }
    }
}

