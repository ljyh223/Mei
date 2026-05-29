package com.ljyh.mei.ui.screen.local.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun CoverCollage(
    covers: List<String>,
    size: Dp = 44.dp
) {
    val radius = 6.dp
    if (covers.isEmpty()) {
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(radius),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.MusicNote, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    } else if (covers.size == 1) {
        AsyncImage(
            model = covers[0],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(radius))
        )
    } else {
        val halfSize = size / 2
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(radius),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Row {
                    CoverTile(covers.getOrNull(0), halfSize, topLeft = true)
                    CoverTile(covers.getOrNull(1), halfSize, topRight = true)
                }
                androidx.compose.foundation.layout.Row {
                    CoverTile(covers.getOrNull(2), halfSize, bottomLeft = true)
                    CoverTile(covers.getOrNull(3), halfSize, bottomRight = true)
                }
            }
        }
    }
}

@Composable
private fun CoverTile(cover: String?, size: Dp, topLeft: Boolean = false, topRight: Boolean = false, bottomLeft: Boolean = false, bottomRight: Boolean = false) {
    if (cover != null) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size - 1.dp)
        )
    } else {
        Box(
            modifier = Modifier.size(size - 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(size * 0.3f)
            )
        }
    }
}
