package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Two large squares above four smaller squares; the last repeats the first cover. */
@Composable
fun ImageCollageContent(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.size < 5) return

    BoxWithConstraints(modifier = modifier) {
        val collageWidth = maxWidth
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(collageWidth / 2)) {
                repeat(2) { index ->
                    AsyncImage(
                        model = imageUrls[index],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().height(collageWidth / 4)) {
                listOf(imageUrls[2], imageUrls[3], imageUrls[4], imageUrls[0]).forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}
