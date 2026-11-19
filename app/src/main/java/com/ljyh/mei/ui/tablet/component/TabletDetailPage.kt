package com.ljyh.mei.ui.tablet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.ljyh.mei.ui.component.layout.LocalPageHorizontalSafePadding
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
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
    showToolbar: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    sidePane: @Composable (PaddingValues) -> Unit,
    mainPane: @Composable (PaddingValues) -> Unit,
) {
    val source = rememberHazeState()
    val compact = LocalPhoneLandscape.current
    val surface = MaterialTheme.colorScheme.surface
    Scaffold(
        containerColor = surface,
        topBar = {
            if (showToolbar) {
                MeiHazeTopBar(
                    state = source, title = title, progress = 1f, centerTitle = false,
                    onNavigateBack = onBack, actions = actions, backgroundMode = backgroundMode,
                    modifier = toolbarModifier,
                )
            } else {
                MeiStatusBarBackdrop(source, backgroundMode)
            }
        },
    ) { padding ->
        val insets = PaddingValues(
            // Keep reading insets stable when the hero changes the toolbar from glass to solid.
            top = padding.calculateTopPadding() + meiTopBarContentClearance() + if (compact) 4.dp else 12.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + if (compact) 8.dp else 24.dp,
        )
        Row(
            Modifier.fillMaxSize().hazeSource(source).background(surface)
                .padding(meiPageHorizontalPadding(gutter = 0.dp)),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CompositionLocalProvider(
                LocalPageStartInset provides 0.dp,
                LocalPageHorizontalSafePadding provides PaddingValues(0.dp),
            ) {
                Box(Modifier.width(sideWidth).fillMaxHeight()) { sidePane(insets) }
                Box(Modifier.weight(1f).fillMaxHeight()) { mainPane(insets) }
            }
        }
    }
}

@Composable
internal fun LazyItemScope.DetailSideArtwork(
    url: String?,
    aspectRatio: Float = 1f,
    placeholder: ImageVector = Icons.Rounded.MusicNote,
    loading: Boolean = false,
) {
    val landscape = LocalPhoneLandscape.current
    val frame = Modifier.padding(horizontal = meiPageGutter())
    SkeletonShimmerHost(
        enabled = loading,
        modifier = if (landscape) frame.fillParentMaxHeight(0.64f).fillMaxWidth() else frame,
    ) {
        val artwork = if (landscape) {
            Modifier.align(Alignment.Center).aspectRatio(1f, matchHeightConstraintsFirst = true)
        } else Modifier
        TabletArtwork(url, artwork, aspectRatio = if (landscape) 1f else aspectRatio, placeholder = placeholder)
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
internal fun TabletSectionHeading(
    title: String,
    onShowAll: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = com.ljyh.mei.ui.component.layout.meiPageGutter()).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically) {
        com.ljyh.mei.ui.component.MeiSectionTitle(title, Modifier.weight(1f))
        actions()
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
