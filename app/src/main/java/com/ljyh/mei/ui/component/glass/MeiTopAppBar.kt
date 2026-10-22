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
    glassNavigationIcon: Boolean = true,
    hazeState: HazeState? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
) {
    Box(modifier.fillMaxWidth()) {
        val source = hazeState ?: LocalMeiHazeState.current
        MeiTopBarBackdrop(state = source, modifier = Modifier.matchParentSize())
        // The shared backdrop owns the surface in both live and fallback modes.
        val barColors = colors.copy(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
        val icon: @Composable () -> Unit = {
            if (navigationIcon != null) {
                if (glassNavigationIcon) {
                    MeiToolbarIconContainer(
                        modifier = Modifier.padding(start = 8.dp),
                        hazeState = source,
                        content = navigationIcon,
                    )
                } else {
                    navigationIcon()
                }
            }
        }

        when {
            large -> LargeTopAppBar(
                title = title,
                navigationIcon = icon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
            centerAligned -> CenterAlignedTopAppBar(
                title = title,
                navigationIcon = icon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
            else -> TopAppBar(
                title = title,
                navigationIcon = icon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = barColors,
            )
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
    MeiToolbarIconContainer(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 3.dp),
        hazeState = hazeState ?: LocalMeiHazeState.current,
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
                onClickLabel = contentDescription,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** One quiet 40dp lens with a separate, full-sized 48dp interaction/content slot. */
@Composable
private fun MeiToolbarIconContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        MeiGlassSurface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(50),
            role = MeiGlassRole.ToolbarControl,
            hazeState = hazeState,
        ) {}
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}
