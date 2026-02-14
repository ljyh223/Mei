package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImmersiveBackground(imageUrl: String, isTablet: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isTablet) 100.dp else 80.dp) // 平板端更模糊，减少干扰
                .alpha(if (isTablet) 0.4f else 0.5f),
            contentScale = ContentScale.Crop
        )

        // 平板端增加一个水平渐变，让左侧（侧边栏文字区）稍微暗一点
        val brush = if (isTablet) {
            Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
                )
            )
        } else {
            SolidColor(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )
    }
}