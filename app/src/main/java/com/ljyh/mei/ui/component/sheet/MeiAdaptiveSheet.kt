package com.ljyh.mei.ui.component.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
class MeiAdaptiveSheetState internal constructor() {
    internal var closing by mutableStateOf(false)
        private set
    fun hide() { closing = true }
    internal fun cancelDismiss() { closing = false }
}

@Composable
fun rememberMeiAdaptiveSheetState(): MeiAdaptiveSheetState = remember { MeiAdaptiveSheetState() }

internal enum class MeiSheetPresentation { Bottom, Side }

internal fun meiSheetPresentation(
    widthDp: Float,
    fontScale: Float,
    allowSide: Boolean,
    minSideWidthDp: Float = 600f,
): MeiSheetPresentation =
    if (allowSide && widthDp.isFinite() && widthDp >= minSideWidthDp * fontScale.coerceIn(1f, 1.3f)) {
        MeiSheetPresentation.Side
    } else MeiSheetPresentation.Bottom

private data class MeiSheetMetrics(val height: Dp, val presentation: MeiSheetPresentation)
private val LocalMeiSheetMetrics = staticCompositionLocalOf<MeiSheetMetrics?> { null }

@Composable
internal fun isCompactSideSheet(): Boolean =
    LocalPhoneLandscape.current && LocalMeiSheetMetrics.current?.presentation == MeiSheetPresentation.Side

/** Content consumes its host's constraints; only bottom sheets retain their compact height. */
@Composable
internal fun meiSheetContentHeight(fraction: Float = 1f, maxHeight: Dp = Dp.Infinity): Dp {
    val metrics = LocalMeiSheetMetrics.current
    val available = metrics?.height ?: LocalConfiguration.current.screenHeightDp.dp
    return if (metrics?.presentation == MeiSheetPresentation.Side) available
    else minOf(available * fraction, maxHeight)
}

/**
 * One modal presentation boundary for transient tasks. Content and business navigation do not
 * choose a device layout. The same movable content is reused when the window changes shape.
 * [onDismissRequest] runs after the exit animation; the content callback requests that exit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeiAdaptiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: MeiAdaptiveSheetState = rememberMeiAdaptiveSheetState(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    dismissEnabled: Boolean = true,
    onDismissBlocked: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    compactTitle: String? = null,
    compactContent: Boolean = false,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val config = LocalConfiguration.current
    val insets = WindowInsets.safeDrawing
    val width = with(density) {
        (window.width - insets.getLeft(this, LocalLayoutDirection.current) -
            insets.getRight(this, LocalLayoutDirection.current)).toDp().value
    }
    val windowLayout = LocalAppWindowLayout.current
    val compactSide = windowLayout == AppWindowLayout.Compact && LocalPhoneLandscape.current
    val allowSide = when (windowLayout) {
        AppWindowLayout.Wide -> true
        AppWindowLayout.Compact -> compactSide
        null -> config.smallestScreenWidthDp >= 600
    }
    val presentation = meiSheetPresentation(width, density.fontScale, allowSide,
        minSideWidthDp = if (compactSide) 480f else 600f)
    val closing = state.closing && dismissEnabled
    val isClosing = rememberUpdatedState(closing)
    var dismissed by remember(state) { mutableStateOf(false) }
    val allowed = rememberUpdatedState(dismissEnabled)
    val onClosed = rememberUpdatedState(onDismissRequest)
    val onBlocked = rememberUpdatedState(onDismissBlocked)
    val navigateBack = rememberUpdatedState(onNavigateBack)
    val requestClose: () -> Unit = remember(state) {
        { if (allowed.value) state.hide() else onBlocked.value() }
    }
    val finishClose: () -> Unit = remember(state) {
        { if (!dismissed) { dismissed = true; onClosed.value() } }
    }
    val handleBack: () -> Unit = remember(requestClose) {
        { if (!allowed.value) onBlocked.value() else navigateBack.value?.invoke() ?: requestClose() }
    }
    LaunchedEffect(state.closing, dismissEnabled) {
        if (state.closing && !dismissEnabled) { state.cancelDismiss(); onBlocked.value() }
    }
    val latestContent = rememberUpdatedState(content)
    val sharedContent = remember {
        movableContentOf<() -> Unit> { close -> latestContent.value(close) }
    }

    // Keep a single dialog/recomposer alive across presentation changes. Replacing a
    // ModalBottomSheet dialog with another Dialog disposes even movable content state.
    val reveal = remember { Animatable(0f) }
    val bottomState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { next -> next != SheetValue.Hidden || allowed.value },
    )
    var bottomWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(closing, presentation) {
        coroutineScope {
            launch { reveal.animateTo(if (closing) 0f else 1f, PlayerMotionSpec.tween()) }
            if (presentation == MeiSheetPresentation.Bottom) {
                if (closing) bottomState.hide() else bottomState.expand()
            }
        }
        if (closing) finishClose()
    }
    LaunchedEffect(presentation) {
        if (presentation == MeiSheetPresentation.Bottom) {
            bottomWasVisible = false
            snapshotFlow { bottomState.currentValue }.collect { value ->
                if (value == SheetValue.Expanded) bottomWasVisible = true
                if (value == SheetValue.Hidden && bottomWasVisible && !isClosing.value) {
                    requestClose()
                }
            }
        }
    }
    Dialog(
        onDismissRequest = handleBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let {
                it.setDimAmount(0f)
                it.setWindowAnimations(0)
            }
        }
        BackHandler(onBack = handleBack)
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = .32f * reveal.value))
                .clickable(onClickLabel = "关闭弹层", onClick = requestClose))
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .imePadding(),
            ) {
                if (presentation == MeiSheetPresentation.Bottom) {
                    BottomSheetScaffold(
                        modifier = modifier,
                        scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomState),
                        sheetPeekHeight = 0.dp,
                        sheetContainerColor = containerColor,
                        sheetSwipeEnabled = dismissEnabled && !closing,
                        containerColor = Color.Transparent,
                        sheetDragHandle = {
                            MeiSheetHeader(false, if (dismissEnabled) navigateBack.value else null, requestClose,
                                title = compactTitle.takeIf { compactContent })
                        },
                        sheetContent = {
                            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()) {
                                CompositionLocalProvider(LocalMeiSheetMetrics provides MeiSheetMetrics(maxHeight, presentation)) {
                                    sharedContent(requestClose)
                                }
                            }
                        },
                    ) {
                        Box(Modifier.fillMaxSize().clickable(onClickLabel = "关闭弹层", onClick = requestClose))
                    }
                } else {
                    val scope = rememberCoroutineScope()
                    var dragged by remember { mutableFloatStateOf(0f) }
                    val preferredWidth = if (compactSide) 352.dp else 420.dp
                    val horizontalMargin = if (compactSide) 12.dp else 16.dp
                    val verticalMargin = if (compactSide) 8.dp else 16.dp
                    val panelWidth = minOf(
                        preferredWidth * density.fontScale.coerceIn(1f, 1.25f),
                        (maxWidth - 80.dp).coerceAtLeast(0.dp),
                    )
                    val panelPx = with(density) { panelWidth.toPx() }
                    val compactPanelMaxHeight = maxHeight * 0.7f
                    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
                    Box(
                        Modifier.fillMaxSize().navigationBarsPadding()
                            .padding(horizontal = horizontalMargin, vertical = verticalMargin),
                    ) {
                        Surface(
                            modifier = modifier.align(if (compactContent) Alignment.BottomEnd else Alignment.CenterEnd)
                                .width(panelWidth)
                                .then(if (compactContent) Modifier.heightIn(max = compactPanelMaxHeight) else Modifier.fillMaxHeight())
                                .graphicsLayer {
                                    translationX = direction * (dragged + (panelPx - dragged + with(density) { horizontalMargin.toPx() }) * (1f - reveal.value))
                                }
                                .draggable(
                                    state = rememberDraggableState { delta -> dragged = (dragged + delta * direction).coerceIn(0f, panelPx) },
                                    orientation = Orientation.Horizontal,
                                    enabled = dismissEnabled && !closing,
                                    onDragStopped = { velocity ->
                                        if (dragged > panelPx * .3f || velocity * direction > 1000f) requestClose()
                                        else scope.launch { animate(dragged, 0f, animationSpec = PlayerMotionSpec.tween()) { value, _ -> dragged = value } }
                                    },
                                )
                                .semantics { paneTitle = "操作面板"; dismiss { requestClose(); true } },
                            shape = RoundedCornerShape(28.dp),
                            color = containerColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp,
                        ) {
                            Column {
                                MeiSheetHeader(
                                    side = true,
                                    onBack = if (dismissEnabled) navigateBack.value else null,
                                    onClose = requestClose,
                                    title = compactTitle.takeIf { compactSide || compactContent },
                                )
                                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = !compactContent)) {
                                    CompositionLocalProvider(LocalMeiSheetMetrics provides MeiSheetMetrics(maxHeight, presentation)) {
                                        sharedContent(requestClose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeiSheetHeader(side: Boolean, onBack: (() -> Unit)?, onClose: () -> Unit, title: String? = null) {
    Box(Modifier.fillMaxWidth().height(48.dp)) {
        if (!side) BottomSheetDefaults.DragHandle(Modifier.align(Alignment.Center))
        if (!title.isNullOrBlank()) Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart)
                .padding(start = if (onBack != null) 48.dp else 16.dp, end = 48.dp)
                .semantics { heading() },
        )
        if (onBack != null) IconButton(onBack, Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回上一弹层")
        }
        IconButton(onClose, Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.Close, "关闭弹层") }
    }
}
