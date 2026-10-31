package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.MeiTopBarBackdrop
import com.ljyh.mei.ui.component.glass.MeiTopBarBackground
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Floating controls sample the content directly, without a second full-header blur layer. */
@Composable
internal fun SearchResultChrome(
    modifier: Modifier = Modifier,
    toolbar: @Composable () -> Unit,
    tabs: (@Composable () -> Unit)? = null,
    content: @Composable (topPadding: Dp) -> Unit,
) {
    val source = rememberHazeState()
    CompositionLocalProvider(LocalMeiHazeState provides source) {
        SubcomposeLayout(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { constraints ->
            val header = subcompose("header") {
                Box(Modifier.fillMaxWidth()) {
                    MeiTopBarBackdrop(
                        state = source,
                        modifier = Modifier.matchParentSize(),
                        backgroundMode = MeiTopBarBackground.Transparent,
                    )
                    Column(Modifier.fillMaxWidth()) {
                        toolbar()
                        tabs?.invoke()
                    }
                }
            }.single().measure(constraints.copy(minHeight = 0))
            val body = subcompose("content") {
                Box(Modifier.fillMaxSize().hazeSource(source)) {
                    content(header.height.toDp())
                }
            }.single().measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                body.placeRelative(0, 0)
                header.placeRelative(0, 0)
            }
        }
    }
}
