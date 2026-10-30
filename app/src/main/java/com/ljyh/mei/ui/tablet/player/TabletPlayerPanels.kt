package com.ljyh.mei.ui.tablet.player

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel

/** Presentation transitions are separate from panel intent and engine-backed content ownership. */
@Composable
internal fun TabletPlayerPanels(
    panel: PlayerPanel,
    style: TabletAnimationStyle,
    modifier: Modifier,
    content: @Composable (PlayerPanel, Modifier) -> Unit,
) {
    if (style == TabletAnimationStyle.FLIP_3D) {
        var displayed by remember { mutableStateOf(panel) }
        val rotation = remember { Animatable(0f) }
        val density = LocalDensity.current.density
        LaunchedEffect(panel) {
            if (displayed != panel) {
                rotation.animateTo(90f, tween(260, easing = FastOutSlowInEasing))
                displayed = panel
                rotation.snapTo(-90f)
            }
            rotation.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
        }
        Box(modifier.graphicsLayer { rotationY = rotation.value; cameraDistance = 12f * density }) {
            content(displayed, Modifier.fillMaxSize())
        }
    } else {
        AnimatedContent(panel, modifier = modifier, transitionSpec = {
            when (style) {
                TabletAnimationStyle.SLIDE -> (fadeIn(tween(200)) + slideInHorizontally { it / 5 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { -it / 5 })
                TabletAnimationStyle.ZOOM -> (fadeIn(tween(200)) + scaleIn(initialScale = .96f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(targetScale = .96f))
                else -> fadeIn(tween(200)) togetherWith fadeOut(tween(160))
            }
        }, label = "TabletPlayerPanel") { visible -> content(visible, Modifier.fillMaxSize()) }
    }
}
