package com.ljyh.mei.ui.component.glass

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.background
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import com.ljyh.mei.ui.component.layout.MeiPageLayout
import com.ljyh.mei.ui.component.layout.meiPageGutter

/**
 * Top bar backed by the same glass material as the navigation chrome, so page content shows
 * through a blurred layer instead of an opaque surface.
 *
 * [large] keeps the title expanded at first and folds it into the collapsed bar on scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeiTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    large: Boolean = false,
    centerAligned: Boolean = false,
    hazeState: HazeState? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
) {
    Box(modifier.fillMaxWidth()) {
        val source = hazeState ?: LocalMeiHazeState.current
        val barModifier = Modifier.padding(horizontal = (meiPageGutter() - 8.dp).coerceAtLeast(0.dp))
        val barActions: @Composable RowScope.() -> Unit = {
            CompositionLocalProvider(LocalMeiHazeState provides source) { actions() }
        }
        MeiTopBarBackdrop(state = source, modifier = Modifier.matchParentSize())
        // The shared backdrop owns the surface in both live and fallback modes.
        val barColors = colors.copy(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
        val icon: @Composable () -> Unit = {
            if (navigationIcon != null) {
                CompositionLocalProvider(LocalMeiHazeState provides source) { navigationIcon() }
            }
        }

        when {
            large -> LargeTopAppBar(
                modifier = barModifier,
                title = title,
                navigationIcon = icon,
                actions = barActions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
            centerAligned -> CenterAlignedTopAppBar(
                modifier = barModifier,
                title = title,
                navigationIcon = icon,
                actions = barActions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
            else -> TopAppBar(
                modifier = barModifier,
                title = title,
                navigationIcon = icon,
                actions = barActions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
        }
    }
}

/** Shared floating icon action; the readable glyph never enters the sampled layer. */
@Composable
fun MeiGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    hazeState: HazeState? = LocalMeiHazeState.current,
    content: @Composable () -> Unit,
) {
    MeiToolbarIconContainer(modifier = modifier.alpha(if (enabled) 1f else 0.38f), hazeState = hazeState) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                role = Role.Button,
            ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Box(Modifier.size(MeiPageLayout.ToolbarIconSize), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}

/** Floating glass back affordance for secondary screens. */
@Composable
fun MeiGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String = "返回",
    hazeState: HazeState? = null,
) {
    MeiGlassIconButton(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        hazeState = hazeState ?: LocalMeiHazeState.current,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = contentDescription)
    }
}

/** One quiet lens with a separate full-sized interaction slot. */
@Composable
private fun MeiToolbarIconContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    Box(modifier.size(MeiPageLayout.ToolbarTouchSize), contentAlignment = Alignment.Center) {
        if (LocalMeiGlassEnabled.current) {
            MeiGlassSurface(
                modifier = Modifier.size(MeiPageLayout.ToolbarLensSize),
                shape = RoundedCornerShape(50),
                role = MeiGlassRole.ToolbarControl,
                hazeState = hazeState,
            ) {}
        } else {
            Box(Modifier.size(MeiPageLayout.ToolbarLensSize).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape))
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}
