package com.ljyh.mei.ui.tablet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Shared wide-page chrome only. Each page owns both pane contents and their scroll states. */
@Composable
internal fun TabletDetailPage(
    title: String,
    sideWidth: Dp,
    onBack: (() -> Unit)? = null,
    backgroundMode: MeiTopBarBackground = MeiTopBarBackground.Auto,
    toolbarModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    sidePane: @Composable (PaddingValues) -> Unit,
    mainPane: @Composable (PaddingValues) -> Unit,
) {
    val source = rememberHazeState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MeiHazeTopBar(
                state = source, title = title, progress = 1f, centerTitle = false,
                onNavigateBack = onBack, actions = actions, backgroundMode = backgroundMode,
                modifier = toolbarModifier,
            )
        },
    ) { padding ->
        val insets = PaddingValues(
            // Keep reading insets stable when the hero changes the toolbar from glass to solid.
            top = padding.calculateTopPadding() + meiTopBarContentClearance() + 12.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
        )
        Row(Modifier.fillMaxSize().hazeSource(source).background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.width(sideWidth).fillMaxHeight()) { sidePane(insets) }
            Box(Modifier.weight(1f).fillMaxHeight()) { mainPane(insets) }
        }
    }
}

@Composable
internal fun TabletArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    placeholder: ImageVector = Icons.Rounded.MusicNote,
) {
    Box(modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) Icon(placeholder, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else AsyncImage(url, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun TabletSectionHeading(title: String, onShowAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = com.ljyh.mei.ui.component.layout.meiPageGutter()).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically) {
        com.ljyh.mei.ui.component.MeiSectionTitle(title, Modifier.weight(1f))
        if (onShowAll != null) com.ljyh.mei.ui.component.MeiShowAllButton(onShowAll)
    }
}

@Composable
internal fun <T> TabletArtworkRow(row: List<T>, columns: Int, content: @Composable (T, Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = com.ljyh.mei.ui.component.layout.meiPageGutter()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        row.forEach { content(it, Modifier.weight(1f)) }
        repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}
