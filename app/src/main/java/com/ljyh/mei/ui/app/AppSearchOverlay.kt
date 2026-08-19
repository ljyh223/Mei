package com.ljyh.mei.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.IconButton as CombinedClickableIconButton
import com.ljyh.mei.ui.component.SearchBar
import com.ljyh.mei.ui.component.TabletNavigationRailWidth
import com.ljyh.mei.ui.screen.search.SearchScreen

@Composable
fun BoxScope.AppSearchOverlay(
    shellState: MainShellState,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSubmit: (query: String, type: Int) -> Unit,
    onNavigateUp: () -> Unit,
    onBackToMain: () -> Unit,
    onOpenSettings: () -> Unit,
    isTopLevelRoute: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    focusRequester: FocusRequester,
) {
    AnimatedVisibility(
        visible = shellState.showSearchBar,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { if (it.isNotEmpty()) onSubmit(it, 1) },
            active = shellState.isSearchActive,
            onActiveChange = onActiveChange,
            scrollBehavior = scrollBehavior,
            placeholder = { Text("搜索") },
            leadingIcon = {
                CombinedClickableIconButton(
                    onClick = {
                        when {
                            shellState.isSearchActive -> onActiveChange(false)
                            !shellState.isMainDestination -> onNavigateUp()
                            else -> onActiveChange(true)
                        }
                    },
                    onLongClick = {
                        if (!shellState.isSearchActive && !shellState.isMainDestination) {
                            onBackToMain()
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (
                            shellState.isSearchActive || !shellState.isMainDestination
                        ) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Search,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null,
                    )
                }
            },
            trailingIcon = {
                when {
                    shellState.isSearchActive -> {
                        if (query.text.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange(TextFieldValue()) }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                        IconButton(onClick = { onSubmit(query.text, 1) }) {
                            Icon(
                                painter = painterResource(R.drawable.cloud),
                                contentDescription = "neteasecloud",
                            )
                        }
                    }
                    isTopLevelRoute -> IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
            },
            focusRequester = focusRequester,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = if (shellState.showTabletSidebar) {
                        TabletNavigationRailWidth
                    } else {
                        0.dp
                    },
                ),
        ) {
            SearchScreen(
                query = query.text,
                onQueryChange = onQueryChange,
                onSearch = onSubmit,
                onDismiss = { onActiveChange(false) },
            )
        }
    }
}
