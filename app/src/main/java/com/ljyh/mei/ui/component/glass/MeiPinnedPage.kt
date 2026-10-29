package com.ljyh.mei.ui.component.glass

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.layout.meiPageGutter

/** Scroll state of the current pinned page, so the caller's column can share it. */
val LocalMeiPageScroll = staticCompositionLocalOf<ScrollState?> { null }

/** 0 = large title fully visible, 1 = collapsed into the compact bar. */
val LocalMeiPageProgress = staticCompositionLocalOf { 0f }

/** Allows a pane showing a top-level destination to omit its redundant back button. */
val LocalMeiShowBackButton = staticCompositionLocalOf { true }

private val CollapseDistance = 56.dp
private val BarHeight = 54.dp

/**
 * Blur-only bar over a Haze source layer. The caller records the page with [hazeSource] and the
 * toolbar stays outside that layer, so the blur samples the page without sampling itself.
 */
@Composable
fun MeiHazeTopBar(
    state: HazeState,
    title: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    backgroundProgress: Float = progress,
    onBackLongClick: (() -> Unit)? = null,
) {
    val alpha = progress.coerceIn(0f, 1f)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier.fillMaxWidth().height(statusTop + BarHeight)) {
        MeiTopBarBackdrop(
            state = state,
            progress = backgroundProgress,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = meiPageGutter() - 4.dp),
        ) {
            if (onNavigateBack != null && LocalMeiShowBackButton.current) {
                Box(Modifier.align(Alignment.CenterStart)) {
                    MeiGlassBackButton(
                        onClick = onNavigateBack,
                        onLongClick = onBackLongClick,
                        hazeState = state,
                        modifier = Modifier.padding(0.dp),
                    )
                }
            }
            // Title is centered on the screen, not in the space left over by the icons.
            Box(
                Modifier.align(Alignment.Center).padding(horizontal = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.graphicsLayer { this.alpha = alpha }) {
                    if (titleContent != null) {
                        titleContent()
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalMeiHazeState provides state) { actions() }
            }
        }
    }
}

/**
 * iOS-style pinned page on Haze: the page body is the source layer and the bar sits outside it.
 * The big title lives inside the scrolling content; the compact bar fades in on scroll.
 */
@Composable
fun MeiPinnedPage(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    onBackLongClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    contentTopPadding: Dp = BarHeight + 10.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val state = rememberHazeState()
    val scroll = rememberScrollState()
    val progress by rememberProgress(scroll)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(state)) {
            CompositionLocalProvider(
                LocalMeiPageScroll provides scroll,
                LocalMeiPageProgress provides progress,
            ) {
                content(PaddingValues(top = statusTop + contentTopPadding, bottom = bottomInset + 24.dp))
            }
        }
        MeiHazeTopBar(
            state = state,
            title = title,
            progress = progress,
            onNavigateBack = onNavigateBack,
            onBackLongClick = onBackLongClick,
            actions = actions,
        )
    }
}

/** Big in-content title that shrinks and blurs away as the page scrolls. */
@Composable
fun MeiLargeTitle(title: String, modifier: Modifier = Modifier, startPadding: androidx.compose.ui.unit.Dp = meiPageGutter()) {
    val progress = LocalMeiPageProgress.current
    Text(
        text = title,
        modifier = modifier
            .padding(start = startPadding, end = startPadding, top = 4.dp, bottom = 8.dp)
            .graphicsLayer {
                alpha = 1f - progress
                val scale = 1f - 0.04f * progress
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .blur(6.dp * progress),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun rememberProgress(scroll: ScrollState): State<Float> {
    val distancePx = with(LocalDensity.current) { CollapseDistance.toPx() }
    return remember(scroll, distancePx) {
        derivedStateOf { (scroll.value / distancePx).coerceIn(0f, 1f) }
    }
}

/** Lazy-list variant: the page body is the Haze source and the bar samples it from outside. */
@Composable
fun MeiPinnedLazyPage(
    title: String,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    horizontalContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    largeTitleHorizontalPadding: androidx.compose.ui.unit.Dp? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val state = rememberHazeState()
    val listState = rememberLazyListState()
    val progress by rememberLazyProgress(listState)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val insetBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val indent = largeTitleHorizontalPadding ?: (meiPageGutter() - horizontalContentPadding).coerceAtLeast(0.dp)
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(state)) {
            CompositionLocalProvider(LocalMeiPageProgress provides progress) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = horizontalContentPadding,
                        end = horizontalContentPadding,
                        top = statusTop + BarHeight + 10.dp,
                        bottom = insetBottom + 24.dp,
                    ),
                    verticalArrangement = verticalArrangement,
                ) {
                    item(key = "mei-large-title") { MeiLargeTitle(title, startPadding = indent) }
                    content()
                }
            }
        }
        MeiHazeTopBar(
            state = state,
            title = title,
            progress = progress,
            onNavigateBack = onNavigateBack,
            actions = actions,
        )
    }
}

@Composable
private fun rememberLazyProgress(state: LazyListState): State<Float> {
    val distancePx = with(LocalDensity.current) { CollapseDistance.toPx() }
    return remember(state, distancePx) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)
        }
    }
}
