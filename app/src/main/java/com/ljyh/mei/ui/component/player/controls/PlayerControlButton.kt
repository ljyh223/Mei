package com.ljyh.mei.ui.component.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor

/** A quiet tonal circle separates secondary actions without competing with the artwork. */
@Composable
fun PlayerControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    foreground: Color = LocalPlayerForegroundColor.current,
    size: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(size)) {
        CompositionLocalProvider(LocalContentColor provides foreground.copy(alpha = if (enabled) 1f else 0.38f)) {
            Box(
                modifier = Modifier.size(size - 8.dp)
                    .background(foreground.copy(alpha = if (enabled) 0.14f else 0.06f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}
