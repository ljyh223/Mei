package com.ljyh.mei.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade


@Composable
fun ImageCollageContent(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    if (imageUrls.size < 5) return

    Column(
        modifier = modifier,
    ) {
        // 上面两张 (1:1 宽高比)
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 每张占一半宽度，宽高比为1:1
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrls[0]).crossfade(true).build(),
                contentDescription = "Image 1 (1:1)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrls[1]).crossfade(true).build(),
                contentDescription = "Image 2 (1:1)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
        }

        // 下面三张 (3:1 宽高比), 留一个空白
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 在一个4等分的空间里放3张图和1个空白
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrls[2]).crossfade(true).build(),
                contentDescription = "Image 3 (3:1)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f) // 宽高比3:1
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrls[3]).crossfade(true).build(),
                contentDescription = "Image 4 (3:1)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrls[4]).crossfade(true).build(),
                contentDescription = "Image 5 (3:1)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
            // 留下一个权重的空白区域
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}