package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.MeiPageLayout

/** Place the playback controls over the cover without changing the artwork layout. */
@Composable
internal fun MeiArtworkActions(
    modifier: Modifier = Modifier,
    actionAlignment: Alignment = Alignment.BottomStart,
    artwork: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize(), content = artwork)
        Row(
            Modifier.align(actionAlignment).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
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
    val colors = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick = onPlayAll,
        enabled = enabled,
        modifier = modifier.heightIn(min = MeiPageLayout.ToolbarTouchSize)
            .semantics { if (accessibilityLabel != null) contentDescription = accessibilityLabel },
        colors = ButtonDefaults.filledTonalButtonColors(
            disabledContainerColor = colors.surfaceContainer,
            disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (preparing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
