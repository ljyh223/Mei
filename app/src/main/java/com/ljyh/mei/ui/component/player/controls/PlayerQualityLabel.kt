package com.ljyh.mei.ui.component.player.controls

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.ljyh.mei.ui.local.LocalPlayerConnection
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
    val connection = LocalPlayerConnection.current
    val sources = connection?.service?.mediaUriProvider?.sourceInfo?.collectAsState()?.value
    val mediaId = connection?.mediaMetadata?.collectAsState()?.value?.id
    val source = mediaId?.toString()?.let { sources?.get(it) }
    val actualQuality = when (source?.quality) {
        "128k", "standard" -> "标准"
        "320k", "higher", "exhigh" -> "高品质"
        "flac", "lossless" -> "无损"
        "flac24bit", "hires" -> "Hi-Res"
        null -> null
        else -> MusicQuality.entries.firstOrNull { it.text == source.quality }?.explanation
    }
    val label = if (source == null) quality.explanation else listOfNotNull(actualQuality, source.name).joinToString(" · ")
    Text(
        text = label,
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = "更改播放音质", onClick = onClick)
            .semantics { contentDescription = "播放音质设置：${label}" },
        style = textStyle,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
