package com.ljyh.mei.ui.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.meiChromeSelectionColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ljyh.mei.ui.component.glass.meiGlassAccentColor
import com.ljyh.mei.ui.component.glass.meiGlassContentColor
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.component.layout.meiPageGutter

private val SearchPlaceholder = "搜索歌曲、歌手、专辑"

/** Retain the measured bottom inset while the player and navigation animate independently. */
@Composable
fun AppBottomNavigationBar(
    visibilityProgress: Float,
    alphaProgress: Float = visibilityProgress,
    interactive: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    compactProgress: Float = 0f,
    onExpand: () -> Unit = {},
    searchActive: Boolean = false,
    searchPlaceholder: String = SearchPlaceholder,
    searchQuery: TextFieldValue = TextFieldValue(),
    onSearchQueryChange: (TextFieldValue) -> Unit = {},
    onSearchActivate: () -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    var measuredHeight by remember { mutableIntStateOf(0) }
    val progress = visibilityProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { measuredHeight = it.height }
            .graphicsLayer {
                alpha = alphaProgress.coerceIn(0f, 1f)
                translationY = measuredHeight * (1f - progress)
            }
            // IME is handled and consumed by the app's search viewport.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = meiPageGutter(), vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(bottomNavigationContentHeight(LocalDensity.current.fontScale)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchActive) {
                SearchField(
                    query = searchQuery,
                    placeholder = searchPlaceholder,
                    onQueryChange = onSearchQueryChange,
                    onSubmit = onSearchSubmit,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.weight(1f),
                    enabled = interactive,
                )
                Spacer(Modifier.width(8.dp))
                SearchActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭搜索",
                    size = 64.dp,
                    onClick = onSearchClose,
                    enabled = interactive,
                )
            } else {
                NavigationTabs(
                    selectedRoute = selectedRoute,
                    onTabSelect = onTabSelect,
                    compactProgress = compact,
                    onExpand = onExpand,
                    interactive = interactive,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SearchActionButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = searchPlaceholder,
                    size = lerp(64.dp, 48.dp, compact),
                    onClick = onSearchActivate,
                    enabled = interactive,
                )
            }
        }
    }
}

@Composable
private fun NavigationTabs(
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    compactProgress: Float,
    onExpand: () -> Unit,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val compact = compactProgress.coerceIn(0f, 1f)
    val selectedDestination = Index.entries.firstOrNull { it.route == selectedRoute } ?: Index.Home
    val appearance = LocalMeiGlassAppearance.current
    val strongContent = appearance.strongContent && appearance.usesOptics(LocalMeiGlassEnabled.current)
    val selectionColor = meiChromeSelectionColor()
    val accent = meiGlassAccentColor()
    val fontScale = LocalDensity.current.fontScale
    val expandedHeight = bottomNavigationContentHeight(fontScale)
    BoxWithConstraints(modifier = modifier.height(expandedHeight), contentAlignment = Alignment.CenterStart) {
        val surfaceWidth = lerp(maxWidth, 48.dp, compact)
        val surfaceHeight = lerp(expandedHeight, 48.dp, compact)
        val expandedItemWidth = (maxWidth - 24.dp) / Index.entries.size
        MeiGlassSurface(
            modifier = Modifier.width(surfaceWidth).height(surfaceHeight),
            shape = CircleShape,
        ) {
            // A single selected icon moves into the compact button without fading out or swapping.
            Index.entries.forEachIndexed { index, destination ->
                val selected = destination == selectedDestination
                if (selected || compact < 1f) {
                    val itemAlpha = if (selected) 1f else (1f - compact * 2f).coerceIn(0f, 1f)
                    val start = 6.dp + (expandedItemWidth + 4.dp) * index
                    val itemX = lerp(start, 0.dp, compact)
                    val itemWidth = lerp(expandedItemWidth, 48.dp, compact)
                    val fill by animateColorAsState(
                        if (selected) selectionColor else Color.Transparent,
                        label = "navigationSelection",
                    )
                    val ink = meiGlassContentColor(secondary = !selected && !strongContent)
                    val tabIconColor by animateColorAsState(
                        targetValue = if (selected) accent else ink,
                        animationSpec = tween(160),
                        label = "navigationIconColor",
                    )
                    // Follow the shell's existing progress directly: no delayed colour change
                    // when the selected tab becomes the compact navigation button.
                    val iconInk = androidx.compose.ui.graphics.lerp(tabIconColor, ink, compact)
                    val action = if (selected && compact >= 0.5f) {
                        Modifier.clickable(enabled = interactive, role = Role.Button, onClick = onExpand)
                    } else {
                        Modifier.selectable(
                            selected = selected,
                            enabled = interactive && compact < 0.5f,
                            role = Role.Tab,
                            onClick = { onTabSelect(destination) },
                        )
                    }
                    Column(
                        modifier = Modifier.offset(x = itemX).width(itemWidth).height(surfaceHeight)
                            .padding(vertical = 6.dp).alpha(itemAlpha)
                            .clip(CircleShape).background(fill.copy(alpha = fill.alpha * (1f - compact)))
                            .then(action)
                            .semantics {
                                contentDescription = if (selected && compact >= 0.5f)
                                    "展开导航，当前为${destination.label}" else destination.label
                            }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(destination.icon, contentDescription = null, tint = iconInk, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(lerp(2.dp, 0.dp, compact)))
                        Box(Modifier.height(lerp(18.dp * fontScale, 0.dp, compact)).clipToBounds()) {
                            Text(
                                destination.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                fontWeight = when {
                                    selected -> FontWeight.Bold
                                    strongContent -> FontWeight.SemiBold
                                    else -> FontWeight.Medium
                                },
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.alpha(1f - compact).clearAndSetSemantics {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    MeiGlassSurface(
        modifier = Modifier.size(size),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = meiGlassContentColor(),
                modifier = Modifier.size(if (size > 56.dp) 24.dp else 22.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: TextFieldValue,
    placeholder: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    val ink = meiGlassContentColor()
    val placeholderInk = meiGlassContentColor(secondary = true)
    MeiGlassSurface(
        modifier = modifier.height(bottomNavigationContentHeight(LocalDensity.current.fontScale)),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f)
                    .semantics { contentDescription = placeholder }
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                singleLine = true,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = meiGlassContentColor(),
                ),
                cursorBrush = SolidColor(meiGlassAccentColor()),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(query.text) }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = placeholderInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}
