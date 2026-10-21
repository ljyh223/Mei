package com.ljyh.mei.ui.component.navigation

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.screen.Index

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
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchActive) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSubmit = onSearchSubmit,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SearchActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭搜索",
                    size = 64.dp,
                    onClick = onSearchClose,
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
                    contentDescription = "搜索",
                    size = lerp(64.dp, 48.dp, compact),
                    onClick = onSearchActivate,
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
    BoxWithConstraints(
        modifier = modifier.height(64.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val surfaceWidth = lerp(maxWidth, 48.dp, compact)
        val surfaceHeight = lerp(64.dp, 48.dp, compact)
        MeiGlassSurface(
            modifier = Modifier.width(surfaceWidth).height(surfaceHeight),
            shape = CircleShape,
            role = MeiGlassRole.Navigation,
        ) {
            if (compact < 0.5f) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup().padding(6.dp)
                        .alpha(((0.5f - compact) / 0.5f).coerceIn(0f, 1f)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Index.entries.forEach { destination ->
                        val selected = selectedRoute == destination.route
                        val fill by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            label = "navigationSelection",
                        )
                        val ink by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "navigationInk",
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(CircleShape)
                                .background(fill)
                                .selectable(
                                    selected = selected,
                                    enabled = interactive,
                                    role = Role.Tab,
                                    onClick = { onTabSelect(destination) },
                                )
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        ) {
                            Icon(
                                destination.icon,
                                contentDescription = null,
                                tint = ink,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(destination.label, style = MaterialTheme.typography.labelMedium, color = ink)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .clickable(enabled = interactive, role = Role.Button, onClick = onExpand),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = selectedDestination.icon,
                        contentDescription = selectedDestination.label,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
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
) {
    MeiGlassSurface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        role = MeiGlassRole.Navigation,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(if (size > 56.dp) 24.dp else 22.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    MeiGlassSurface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(32.dp),
        role = MeiGlassRole.Navigation,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(query.text) }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.text.isEmpty()) {
                            Text(
                                text = SearchPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}
