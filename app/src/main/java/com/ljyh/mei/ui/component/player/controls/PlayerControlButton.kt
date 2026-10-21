package com.ljyh.mei.ui.component.player.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor
import dev.chrisbanes.haze.HazeState

// Full-screen controls sample their own background, never the underlying browsing page.
val LocalPlayerControlsHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun PlayerControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    foreground: Color = LocalPlayerForegroundColor.current,
    size: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    MeiGlassSurface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 2),
        role = MeiGlassRole.PlayerControl,
        hazeState = LocalPlayerControlsHazeState.current,
        tintColor = foreground,
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size)) {
            CompositionLocalProvider(LocalContentColor provides foreground.copy(alpha = if (enabled) 1f else 0.38f)) {
                Box(contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}
