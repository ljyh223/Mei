package com.ljyh.mei.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun SingleImagePickerSheet(
    images: List<String>,
    minItemSize: Dp = 96.dp,
    maxHeight: Dp = 420.dp,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val minItemPx = with(LocalDensity.current) { minItemSize.toPx() }
    val columns = remember(screenWidthPx, minItemPx) {
        (screenWidthPx / minItemPx).toInt().coerceIn(2, 5)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(top = 12.dp)
    ) {
        // 顶部标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择图片",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }

        // 宫格布局
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images.size) { index ->
                val item = images[index]
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication  = ripple()
                        ) {
                            onSelect(item)
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}