package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Record the artwork alone so the glass actions can sample it. */
@Composable
internal fun MeiArtworkGlassActions(
    modifier: Modifier = Modifier,
    actionAlignment: Alignment = Alignment.BottomStart,
    artwork: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val source = rememberHazeState()
    Box(modifier) {
        Box(Modifier.fillMaxSize().hazeSource(source), content = artwork)
        CompositionLocalProvider(LocalMeiHazeState provides source) {
            Row(
                Modifier.align(actionAlignment).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
internal fun MeiArtworkPlayButton(
    enabled: Boolean,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    preparing: Boolean = false,
    accessibilityLabel: String? = null,
    label: String = "播放全部",
) {
    MeiGlassSurface(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.heightIn(min = MeiPageLayout.ToolbarTouchSize)
                .clickable(enabled = enabled, role = Role.Button, onClick = onPlayAll)
                .semantics { if (accessibilityLabel != null) contentDescription = accessibilityLabel }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (preparing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
