package com.ljyh.mei.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import com.ljyh.mei.ui.screen.Index

const val BottomNavigationAnimationDurationMillis = 240

/**
 * Material 3 bottom navigation owned by the app scaffold.
 *
 * Visibility only affects drawing, not measurement. Keeping the scaffold inset stable prevents
 * the collapsed player anchor from moving while the full player is animating back into the bar.
 */
@Composable
fun AppBottomNavigationBar(
    visibilityProgress: Float,
    interactive: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    var navigationBarHeight by remember { mutableIntStateOf(0) }
    val progress = visibilityProgress.coerceIn(0f, 1f)

    NavigationBar(
        modifier = modifier
            .onSizeChanged { navigationBarHeight = it.height }
            .graphicsLayer {
                alpha = progress
                translationY = navigationBarHeight * (1f - progress)
            },
    ) {
        Index.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onTabSelect(destination) },
                enabled = interactive,
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
