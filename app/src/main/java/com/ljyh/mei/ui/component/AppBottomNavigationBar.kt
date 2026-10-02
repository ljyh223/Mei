package com.ljyh.mei.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
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

/**
 * Material 3 bottom navigation owned by the app scaffold.
 *
 * Visibility only affects drawing, not measurement. Keeping the scaffold inset stable prevents
 * the collapsed player anchor from moving while the full player is animating back into the bar.
 */
@Composable
fun AppBottomNavigationBar(
    visibilityProgress: Float,
    alphaProgress: Float = visibilityProgress,
    interactive: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    var navigationBarHeight by remember { mutableIntStateOf(0) }
    val progress = visibilityProgress.coerceIn(0f, 1f)
    val alpha = alphaProgress.coerceIn(0f, 1f)

    ShortNavigationBar(
        modifier = modifier
            .onSizeChanged { navigationBarHeight = it.height }
            .graphicsLayer {
                this.alpha = alpha
                translationY = navigationBarHeight * (1f - progress)
            },
        arrangement = ShortNavigationBarArrangement.EqualWeight,
    ) {
        Index.entries.forEach { destination ->
            ShortNavigationBarItem(
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
