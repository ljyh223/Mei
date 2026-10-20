package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ljyh.mei.constants.MusicQuality

@Composable
fun PlayerQualityLabel(
    quality: MusicQuality,
    color: Color,
    textStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = quality.explanation,
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = "更改播放音质", onClick = onClick)
            .semantics { contentDescription = "播放音质设置：${quality.explanation}" },
        style = textStyle,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
