package com.ljyh.mei.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

/**
 * Material 3 bottom navigation owned by the app scaffold.
 *
 * Visibility only affects drawing, not measurement. Keeping the scaffold inset stable prevents
 * the collapsed player anchor from moving while the full player is animating back into the bar.
 */
@Composable
fun AppBottomNavigationBar(
    visible: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibilityProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = BottomNavigationAnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "bottomNavigationVisibility",
    )
    var navigationBarHeight by remember { mutableIntStateOf(0) }

    NavigationBar(
        modifier = modifier
            .onSizeChanged { navigationBarHeight = it.height }
            .graphicsLayer {
                alpha = visibilityProgress
                translationY = navigationBarHeight * (1f - visibilityProgress)
            },
    ) {
        Index.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onTabSelect(destination) },
                enabled = visible,
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

private const val BottomNavigationAnimationDurationMillis = 220
