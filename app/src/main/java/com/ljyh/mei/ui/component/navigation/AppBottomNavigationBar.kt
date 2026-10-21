package com.ljyh.mei.ui.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.screen.Index

/** Retain the measured bottom inset while the player and navigation animate independently. */
@Composable
fun AppBottomNavigationBar(
    visibilityProgress: Float,
    alphaProgress: Float = visibilityProgress,
    interactive: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    compactProgress: Float = 0f,
    onExpand: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var measuredHeight by remember { mutableIntStateOf(0) }
    val progress = visibilityProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val selectedDestination = Index.entries.firstOrNull { it.route == selectedRoute } ?: Index.Home
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { measuredHeight = it.height }
            .graphicsLayer {
                alpha = alphaProgress.coerceIn(0f, 1f)
                translationY = measuredHeight * (1f - progress)
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            val surfaceWidth = lerp(maxWidth, 48.dp, compact)
            val surfaceHeight = lerp(64.dp, 48.dp, compact)
            MeiGlassSurface(
                modifier = Modifier.width(surfaceWidth).height(surfaceHeight),
                shape = CircleShape,
                role = MeiGlassRole.Navigation,
            ) {
                if (compact < 0.5f) {
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup().padding(6.dp)
                            .alpha(((0.5f - compact) / 0.5f).coerceIn(0f, 1f)),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Index.entries.forEach { destination ->
                            val selected = selectedRoute == destination.route
                            val fill by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                label = "navigationSelection",
                            )
                            val ink by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "navigationInk",
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                                    .clip(CircleShape)
                                    .background(fill)
                                    .selectable(
                                        selected = selected,
                                        enabled = interactive,
                                        role = Role.Tab,
                                        onClick = { onTabSelect(destination) },
                                    )
                                    .heightIn(min = 52.dp)
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            ) {
                                Icon(
                                    destination.icon,
                                    contentDescription = null,
                                    tint = ink,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(destination.label, style = MaterialTheme.typography.labelMedium, color = ink)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .clickable(
                                enabled = interactive,
                                role = Role.Button,
                                onClick = onExpand,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = selectedDestination.icon,
                            contentDescription = selectedDestination.label,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
