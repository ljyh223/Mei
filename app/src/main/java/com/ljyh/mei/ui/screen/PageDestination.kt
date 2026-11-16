package com.ljyh.mei.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ljyh.mei.ui.component.layout.LocalPageHorizontalSafePadding
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape

/** An opaque moving page; all navigation motion is inherited from the main NavHost. */
fun NavGraphBuilder.pageDestination(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    drawsBehindSideBar: Boolean = false,
    drawsToHorizontalEdgesInLandscape: Boolean = false,
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments) { entry ->
        MeiDestinationSurface(drawsBehindSideBar, drawsToHorizontalEdgesInLandscape) { content(entry) }
    }
}

@Composable
internal fun MeiDestinationSurface(
    drawsBehindSideBar: Boolean,
    drawsToHorizontalEdgesInLandscape: Boolean = false,
    horizontalSafeInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    content: @Composable () -> Unit,
) {
    val drawsToEdges = drawsToHorizontalEdgesInLandscape && LocalPhoneLandscape.current
    // Edge-to-edge pages apply these insets to their controls and list padding.
    val safePadding = if (drawsToEdges) horizontalSafeInsets.asPaddingValues() else PaddingValues(0.dp)
    val safety = if (drawsToEdges) {
        Modifier.consumeWindowInsets(horizontalSafeInsets)
    } else {
        Modifier.windowInsetsPadding(horizontalSafeInsets)
    }
    val startInset = LocalPageStartInset.current
    val pagePadding = if (drawsBehindSideBar) 0.dp else startInset
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(safety)
            .padding(start = pagePadding),
    ) {
        // Only pages with a full-width viewport apply the inset to their foreground.
        CompositionLocalProvider(
            LocalPageStartInset provides (startInset - pagePadding),
            LocalPageHorizontalSafePadding provides safePadding,
            content = content,
        )
    }
}
