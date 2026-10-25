package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PlaylistBackground(coverUrl: String?, emphasized: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = if (emphasized) 0.65f else 0.24f
            )
            // Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = if (emphasized) 0.45f else 0.72f),
                                MaterialTheme.colorScheme.surface.copy(alpha = if (emphasized) 0.82f else 0.97f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}
