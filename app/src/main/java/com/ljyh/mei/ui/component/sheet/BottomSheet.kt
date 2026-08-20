package com.ljyh.mei.ui.component.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.NavigationBarAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class BottomSheetMorphSpec(
    val collapsedHorizontalMargin: Dp = 12.dp,
    val collapsedMaxWidth: Dp? = null,
    val collapsedCornerRadius: Dp = 24.dp,
    val expandedHorizontalMargin: Dp = 0.dp,
    val expandedCornerRadius: Dp = 0.dp,
    val collapsedHeight: Dp = 52.dp,
    val collapsedBottomMargin: Dp = 8.dp,
    val expandedBottomMargin: Dp = 0.dp,
)


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onDismiss: (() -> Unit)? = null,
    onHorizontalSwipe: ((direction: HorizontalSwipeDirection) -> Unit)? = null,
    morphSpec: BottomSheetMorphSpec? = null,
    sharedTransitionKey: String? = null,
    keepExpandedContentComposed: Boolean = false,
    transparentCollapsedContainer: Boolean = false,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = state.progress

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val morphLayout = morphSpec?.let {
            resolveMorphLayout(
                maxWidth = maxWidth,
                expandedHeight = state.expandedBound,
                spec = it,
                progress = progress,
                revealProgress = state.revealProgress,
            )
        }
        val cornerRadius = morphLayout?.cornerRadius
            ?: if (!state.isExpanded) 16.dp else 0.dp
        val sheetShape = if (morphLayout != null) {
            RoundedCornerShape(cornerRadius)
        } else {
            RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        }
        val containerModifier = if (morphLayout != null) {
            Modifier
                .align(Alignment.TopCenter)
                .width(morphLayout.width)
                .height(morphLayout.height)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = containerModifier
                .offset {
                    val y = (state.expandedBound - state.value)
                        .roundToPx() - (morphLayout?.effectiveBottomMargin ?: 0.dp).roundToPx()
                    IntOffset(x = 0, y = y.coerceAtLeast(0))
                }
                .pointerInput(onHorizontalSwipe) {
                    if (onHorizontalSwipe == null) return@pointerInput

                    val velocityTracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onHorizontalDrag = { change, _ ->
                            velocityTracker.addPointerInputChange(change)
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            val swipeThreshold = 500f

                            if (velocity > swipeThreshold) {
                                onHorizontalSwipe(HorizontalSwipeDirection.Right)
                            } else if (velocity < -swipeThreshold) {
                                onHorizontalSwipe(HorizontalSwipeDirection.Left)
                            }
                        }
                    )
                }
                .pointerInput(state) {
                    val velocityTracker = VelocityTracker()

                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            velocityTracker.addPointerInputChange(change)
                            state.dispatchRawDelta(dragAmount)
                        },
                        onDragCancel = {
                            velocityTracker.resetTracking()
                            state.snapTo(state.collapsedBound)
                        },
                        onDragEnd = {
                            val velocity = -velocityTracker.calculateVelocity().y
                            velocityTracker.resetTracking()
                            state.performFling(velocity, onDismiss)
                        }
                    )
                }
                .shadow(elevation = 8.dp, shape = sheetShape)
                .clip(sheetShape)
                .background(
                    if (morphLayout != null) {
                        backgroundColor.copy(
                            alpha = backgroundColor.alpha * if (transparentCollapsedContainer) {
                                ((progress - 0.12f) / 0.28f).coerceIn(0f, 1f)
                            } else if (keepExpandedContentComposed) {
                                1f
                            } else {
                                morphLayout.backgroundAlpha
                            }
                        )
                    } else {
                        backgroundColor.copy(
                            alpha = backgroundColor.alpha *
                                    ((state.progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        )
                    }
                )
        ) {
            if (!state.isCollapsed && !state.isDismissed) {
                BackHandler(onBack = state::collapseSoft)
            }

            if (sharedTransitionKey != null) {
                SharedTransitionLayout {
                    val sharedScope = this
                    AnimatedContent(
                        targetState = state.isTargetExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                    fadeOut(animationSpec = tween(120))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "playerContainerContent"
                    ) { targetExpanded ->
                        val animatedVisibilityScope = this
                        val sharedModifier = with(sharedScope) {
                            Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(sharedTransitionKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                )
                                .clip(RoundedCornerShape(cornerRadius))
                        }

                        if (targetExpanded) {
                            BoxWithConstraints(
                                modifier = sharedModifier.fillMaxSize(),
                                content = content
                            )
                        } else if (onDismiss == null || !state.isDismissed) {
                            Box(
                                modifier = sharedModifier
                                    .fillMaxWidth()
                                    .height(morphSpec?.collapsedHeight ?: state.collapsedBound)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = state::expandSoft
                                    ),
                                content = collapsedContent
                            )
                        }
                    }
                }
            } else {
                if (keepExpandedContentComposed || !state.isCollapsed) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (keepExpandedContentComposed) {
                                    1f
                                } else {
                                    ((state.progress - 0.25f) * 4).coerceIn(0f, 1f)
                                }
                            },
                        content = content
                    )
                }

                if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = if (keepExpandedContentComposed) {
                                    ((MINI_PLAYER_FADE_END - state.progress) /
                                        MINI_PLAYER_FADE_END).coerceIn(0f, 1f)
                                } else {
                                    1f - (state.progress * 4).coerceAtMost(1f)
                                }
                            }
                            .fillMaxWidth()
                            .height(morphSpec?.collapsedHeight ?: state.collapsedBound)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = state::expandSoft
                            ),
                        content = collapsedContent
                    )
                }
            }
        }
    }
}

private const val MINI_PLAYER_FADE_END = 0.18f

@Stable
class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
    initialAnchor: Int,
) : DraggableState by draggableState {
    private var targetAnchor by mutableIntStateOf(initialAnchor)

    val dismissedBound: Dp
        get() = animatable.lowerBound!!

    val expandedBound: Dp
        get() = animatable.upperBound!!

    val value by animatable.asState()

    val isDismissed by derivedStateOf {
        value == animatable.lowerBound!!
    }

    val isCollapsed by derivedStateOf {
        value == collapsedBound
    }

    val isExpanded by derivedStateOf {
        value == animatable.upperBound
    }

    val isTargetExpanded: Boolean
        get() = targetAnchor == expandedAnchor

    val revealProgress by derivedStateOf {
        normalizedProgress(animatable.value, dismissedBound, collapsedBound)
    }

    val progress by derivedStateOf {
        normalizedProgress(animatable.value, collapsedBound, expandedBound)
    }

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        updateTargetAnchor(collapsedAnchor)
        coroutineScope.launch {
            animatable.animateTo(collapsedBound, animationSpec)
        }
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        updateTargetAnchor(expandedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.upperBound!!, animationSpec)
        }
    }

    private fun collapse() {
        collapse(SpringSpec())
    }

    private fun expand() {
        expand(SpringSpec())
    }

    fun collapseSoft() {
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    /**
     * Closes the expanded sheet before leaving the current screen.
     *
     * Navigation changes the content beneath the player immediately. Waiting for the collapse
     * animation prevents the expanded player from remaining above the destination while keeping
     * the player transition visible.
     */
    fun collapseThen(onCollapsed: () -> Unit) {
        updateTargetAnchor(collapsedAnchor)
        coroutineScope.launch {
            animatable.animateTo(
                collapsedBound,
                tween(durationMillis = 280)
            )
            onCollapsed()
        }
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        updateTargetAnchor(dismissedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.lowerBound!!)
        }
    }

    private fun updateTargetAnchor(anchor: Int) {
        targetAnchor = anchor
        onAnchorChanged(anchor)
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch {
            animatable.snapTo(value)
        }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        when (
            resolveFlingTarget(
                value = value,
                velocity = velocity,
                dismissedBound = dismissedBound,
                collapsedBound = collapsedBound,
                expandedBound = expandedBound,
                canDismiss = onDismiss != null,
            )
        ) {
            BottomSheetTarget.Dismissed -> {
                dismiss()
                onDismiss?.invoke()
            }
            BottomSheetTarget.Collapsed -> collapse()
            BottomSheetTarget.Expanded -> expand()
        }
    }

    val preUpPostDownNestedScrollConnection: NestedScrollConnection =
        BottomSheetNestedScrollConnection(this)
}

const val expandedAnchor = 2
const val collapsedAnchor = 1
const val dismissedAnchor = 0

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable {
        mutableIntStateOf(initialAnchor)
    }
    val animatable = remember {
        Animatable(0.dp, Dp.VectorConverter)
    }


    val state = remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    animatable.snapTo(animatable.value - with(density) { delta.toDp() })
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound,
            initialAnchor = previousAnchor
        )
    }

    LaunchedEffect(state) {
        val target = when (previousAnchor) {
            expandedAnchor -> expandedBound
            collapsedAnchor -> collapsedBound
            dismissedAnchor -> dismissedBound
            else -> error("Unknown BottomSheet anchor")
        }
        animatable.animateTo(target, NavigationBarAnimationSpec)
    }

    return state
}
// 在你的文件顶部或一个合适的位置定义这个枚举
enum class HorizontalSwipeDirection {
    Left, Right
}

internal fun normalizedProgress(value: Dp, start: Dp, end: Dp): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}
