package com.ljyh.mei.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Category hues stay independent from the changing album accent. */
enum class MeiCategory(val color: Color) {
    Appearance(Color(0xFF7954C7)),
    Playback(Color(0xFFD04B65)),
    Content(Color(0xFF3971C0)),
    Lyrics(Color(0xFFB05D22)),
    Offline(Color(0xFF197D72)),
    Storage(Color(0xFF596C87)),
    Experimental(Color(0xFF8B5AB2)),
    About(Color(0xFF657181)),
}

@Composable
fun MeiCategoryIcon(icon: ImageVector, category: MeiCategory, size: Dp = 36.dp) {
    Box(
        modifier = Modifier.size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(category.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.60f))
    }
}
