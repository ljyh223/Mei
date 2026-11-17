package com.ljyh.mei.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.meiGlassAccentColor
import com.ljyh.mei.ui.component.glass.meiGlassContentColor
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import com.ljyh.mei.ui.screen.Index

internal fun usePhoneSideBar(widthDp: Float, heightDp: Float): Boolean = widthDp > heightDp

internal val PhoneSideBarWidth = 72.dp

@Composable
internal fun PhoneSideBar(
    selectedRoute: String,
    showPlayer: Boolean,
    coverUrl: String?,
    onSelect: (Index) -> Unit,
    onSearch: () -> Unit,
    onOpenPlayer: () -> Unit,
    visibility: Float,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    playerOwnedBySheet: Boolean = false,
    onPlayerBounds: (surface: Rect, artwork: Rect) -> Unit = { _, _ -> },
    onArtworkReady: (url: String, painter: Painter) -> Unit = { _, _ -> },
) {
    BoxWithConstraints(modifier.fillMaxHeight().width(PhoneSideBarWidth).padding(horizontal = 12.dp)) {
        val touchSize = MeiPageLayout.ToolbarTouchSize
        val slotCount = Index.entries.size + 1 + if (showPlayer) 1 else 0
        val playerGap = if (showPlayer) 8.dp else 0.dp
        val verticalMargin = ((maxHeight - touchSize * slotCount - playerGap) / 2).coerceIn(4.dp, 8.dp)
        val slotHeight = with(LocalDensity.current) {
            val space = constraints.maxHeight - 2 * verticalMargin.roundToPx() - playerGap.roundToPx()
            // Divide actual pixels so individually rounded slots cannot overflow the viewport.
            (space.coerceAtLeast(0) / slotCount).toDp().coerceAtMost(touchSize)
        }
        if (slotHeight == 0.dp) return@BoxWithConstraints
        val slot = Modifier.size(width = touchSize, height = slotHeight)
        val enabled = visibility >= 1f
        Column(
            Modifier.fillMaxHeight()
                .graphicsLayer { alpha = visibility }
                .then(if (enabled) Modifier else Modifier.clearAndSetSemantics {})
                .padding(vertical = verticalMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MeiGlassSurface(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.selectableGroup()) {
                    Index.entries.forEach { tab ->
                        val selected = tab.route == selectedRoute
                        val back = onBack.takeIf { selected }
                        Box(
                            slot.clip(CircleShape).then(
                                if (back != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = back)
                                else Modifier.selectable(selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(tab) }),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (back != null) Icons.AutoMirrored.Rounded.ArrowBack else tab.icon,
                                if (back != null) "返回上一页" else tab.label,
                                tint = if (selected) meiGlassAccentColor() else meiGlassContentColor(true),
                            )
                        }
                    }
                    IconButton(onClick = onSearch, enabled = enabled, modifier = slot) {
                        Icon(Icons.Rounded.Search, "搜索", tint = meiGlassContentColor())
                    }
                }
            }
            if (showPlayer) {
                var surfaceBounds by remember { mutableStateOf<Rect?>(null) }
                var artworkBounds by remember { mutableStateOf<Rect?>(null) }
                val playerAvailable = enabled && !playerOwnedBySheet
                IconButton(
                    onClick = onOpenPlayer,
                    enabled = playerAvailable,
                    modifier = slot.graphicsLayer { alpha = if (playerOwnedBySheet) 0f else 1f }
                        .then(if (playerAvailable) Modifier.semantics { contentDescription = "打开播放器" }
                            else Modifier.clearAndSetSemantics {}),
                ) {
                    MeiGlassSurface(
                        modifier = Modifier.size(slotHeight).onGloballyPositioned {
                            val bounds = Rect(it.positionInRoot(), it.size.toSize())
                            surfaceBounds = bounds
                            artworkBounds?.let { artwork -> onPlayerBounds(bounds, artwork) }
                        },
                        shape = CircleShape,
                    ) {
                        val coverSize = (slotHeight - 8.dp).coerceAtLeast(0.dp)
                        Box(
                            Modifier.align(Alignment.Center).size(coverSize)
                                .onGloballyPositioned {
                                    val bounds = Rect(it.positionInRoot(), it.size.toSize())
                                    artworkBounds = bounds
                                    surfaceBounds?.let { surface -> onPlayerBounds(surface, bounds) }
                                }
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = meiGlassContentColor())
                            if (!coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onSuccess = { onArtworkReady(coverUrl, it.painter) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
