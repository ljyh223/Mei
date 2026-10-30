package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Both phone and tablet pages share one refresh gesture and the same session owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryRefreshBox(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    val indicatorTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    Box(
        Modifier.fillMaxSize().pullToRefresh(
            state = pullState,
            isRefreshing = refreshing,
            enabled = enabled && !refreshing,
            onRefresh = onRefresh,
        ).semantics {
            if (enabled && !refreshing) customActions = listOf(
                CustomAccessibilityAction("刷新音乐库") { onRefresh(); true },
            )
        },
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            state = pullState,
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorTop),
        )
    }
}
