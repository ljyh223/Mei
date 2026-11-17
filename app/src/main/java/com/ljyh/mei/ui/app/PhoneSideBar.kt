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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
) {
    BoxWithConstraints(modifier.fillMaxHeight().width(PhoneSideBarWidth).padding(horizontal = 12.dp)) {
        if (visibility <= 0f) return@BoxWithConstraints
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
            Modifier.fillMaxHeight().graphicsLayer { alpha = visibility }.padding(vertical = verticalMargin),
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
                IconButton(onClick = onOpenPlayer, enabled = enabled, modifier = slot) {
                    MeiGlassSurface(modifier = Modifier.size(slotHeight), shape = CircleShape) {
                        if (coverUrl.isNullOrBlank()) {
                            Icon(
                                Icons.Rounded.MusicNote, "打开播放器", Modifier.align(Alignment.Center),
                                tint = meiGlassContentColor(),
                            )
                        } else {
                            val coverSize = (slotHeight - 8.dp).coerceAtLeast(0.dp)
                            Box(Modifier.align(Alignment.Center).size(coverSize).clip(CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MusicNote, null, tint = meiGlassContentColor())
                                AsyncImage(coverUrl, "打开播放器", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
        }
    }
}
