package com.ljyh.mei.ui.component.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    overlayContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = state.progress
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state.anchoredDraggableState,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }
            .drop(1)
            .collect { settledValue ->
                if (settledValue == BottomSheetValue.Dismissed) {
                    currentOnDismiss?.invoke()
                }
            }
    }

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
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    flingBehavior = flingBehavior,
                )
                .shadow(
                    elevation = if (transparentCollapsedContainer) 0.dp else 8.dp,
                    shape = sheetShape,
                )
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

            overlayContent()
        }
    }
}

private const val MINI_PLAYER_FADE_END = 0.18f

@Stable
class BottomSheetState(
    internal val anchoredDraggableState: AnchoredDraggableState<BottomSheetValue>,
    private val coroutineScope: CoroutineScope,
    private val onAnchorChanged: (Int) -> Unit,
    density: Density,
    dismissedBound: Dp,
    collapsedBound: Dp,
    expandedBound: Dp,
) {
    private var density by mutableStateOf(density)

    var dismissedBound by mutableStateOf(dismissedBound)
        private set

    var collapsedBound by mutableStateOf(collapsedBound)
        private set

    var expandedBound by mutableStateOf(expandedBound)
        private set

    val settledValue: BottomSheetValue
        get() = anchoredDraggableState.settledValue

    val value: Dp
        get() = with(density) {
            anchoredDraggableState.offset
                .takeUnless(Float::isNaN)
                ?.toDp()
                ?: dismissedBound
        }

    val isDismissed by derivedStateOf {
        isAt(BottomSheetValue.Dismissed)
    }

    val isCollapsed by derivedStateOf {
        isAt(BottomSheetValue.Collapsed)
    }

    val isExpanded by derivedStateOf {
        isAt(BottomSheetValue.Expanded)
    }

    val isTargetExpanded: Boolean
        get() = anchoredDraggableState.targetValue == BottomSheetValue.Expanded

    val isTargetDismissed: Boolean
        get() = anchoredDraggableState.targetValue == BottomSheetValue.Dismissed

    val revealProgress by derivedStateOf {
        normalizedProgress(
            value = value,
            start = this.dismissedBound,
            end = this.collapsedBound,
        )
    }

    val progress by derivedStateOf {
        normalizedProgress(
            value = value,
            start = this.collapsedBound,
            end = this.expandedBound,
        )
    }

    private fun animateTo(
        target: BottomSheetValue,
        anchor: Int,
        animationSpec: AnimationSpec<Float>,
        onFinished: (() -> Unit)? = null,
    ) {
        onAnchorChanged(anchor)
        coroutineScope.launch {
            anchoredDraggableState.animateTo(target, animationSpec)
            onFinished?.invoke()
        }
    }

    private fun collapse(animationSpec: AnimationSpec<Float>) {
        animateTo(BottomSheetValue.Collapsed, collapsedAnchor, animationSpec)
    }

    private fun expand(animationSpec: AnimationSpec<Float>) {
        animateTo(BottomSheetValue.Expanded, expandedAnchor, animationSpec)
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
        animateTo(
            target = BottomSheetValue.Collapsed,
            anchor = collapsedAnchor,
            animationSpec = tween(durationMillis = 280),
            onFinished = onCollapsed,
        )
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        animateTo(
            target = BottomSheetValue.Dismissed,
            anchor = dismissedAnchor,
            animationSpec = spring(),
        )
    }

    internal fun updateAnchors(
        density: Density,
        dismissedBound: Dp,
        collapsedBound: Dp,
        expandedBound: Dp,
        anchors: DraggableAnchors<BottomSheetValue>,
    ) {
        this.density = density
        this.dismissedBound = dismissedBound
        this.collapsedBound = collapsedBound
        this.expandedBound = expandedBound
        anchoredDraggableState.updateAnchors(anchors)
    }

    internal fun recordSettledAnchor(value: BottomSheetValue) {
        onAnchorChanged(
            when (value) {
                BottomSheetValue.Dismissed -> dismissedAnchor
                BottomSheetValue.Collapsed -> collapsedAnchor
                BottomSheetValue.Expanded -> expandedAnchor
            }
        )
    }

    private fun isAt(value: BottomSheetValue): Boolean {
        val anchor = anchoredDraggableState.anchors.positionOf(value)
        val offset = anchoredDraggableState.offset
        return !anchor.isNaN() && !offset.isNaN() && abs(anchor - offset) < 0.5f
    }
}

enum class BottomSheetValue {
    Dismissed,
    Collapsed,
    Expanded,
}

private fun bottomSheetValue(anchor: Int): BottomSheetValue =
    when (anchor) {
        dismissedAnchor -> BottomSheetValue.Dismissed
        collapsedAnchor -> BottomSheetValue.Collapsed
        expandedAnchor -> BottomSheetValue.Expanded
        else -> error("Unknown BottomSheet anchor: $anchor")
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
    val anchors = DraggableAnchors {
        BottomSheetValue.Dismissed at with(density) { dismissedBound.toPx() }
        BottomSheetValue.Collapsed at with(density) { collapsedBound.toPx() }
        BottomSheetValue.Expanded at with(density) { expandedBound.toPx() }
    }
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = bottomSheetValue(previousAnchor),
            anchors = anchors,
        )
    }
    val state = remember(coroutineScope, anchoredDraggableState) {
        BottomSheetState(
            anchoredDraggableState = anchoredDraggableState,
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            density = density,
            dismissedBound = dismissedBound,
            collapsedBound = collapsedBound,
            expandedBound = expandedBound,
        )
    }

    SideEffect {
        state.updateAnchors(
            density = density,
            dismissedBound = dismissedBound,
            collapsedBound = collapsedBound,
            expandedBound = expandedBound,
            anchors = anchors,
        )
    }

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { settledValue ->
            state.recordSettledAnchor(settledValue)
        }
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
