package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.MeiTopBarBackdrop
import com.ljyh.mei.ui.component.glass.meiTopBarContentClearance
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** The floating controls sample the blurred header so sharp result text stays behind it. */
@Composable
internal fun SearchResultChrome(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable (topPadding: Dp) -> Unit,
) {
    val pageSource = rememberHazeState()
    val headerSource = rememberHazeState()
    val surface = MaterialTheme.colorScheme.background
    val clearance = meiTopBarContentClearance()
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val header = subcompose("header") {
            Box(Modifier.fillMaxWidth()) {
                MeiTopBarBackdrop(
                    state = pageSource,
                    modifier = Modifier.matchParentSize().hazeSource(headerSource),
                )
                CompositionLocalProvider(LocalMeiHazeState provides headerSource) {
                    header()
                }
            }
        }.single().measure(constraints.copy(minHeight = 0))
        val body = subcompose("content") {
            CompositionLocalProvider(LocalMeiHazeState provides pageSource) {
                Box(Modifier.fillMaxSize().hazeSource(pageSource).background(surface)) {
                    content(header.height.toDp() + clearance)
                }
            }
        }.single().measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            body.placeRelative(0, 0)
            header.placeRelative(0, 0)
        }
    }
}

/** Keep the query and categories in one row when there is room for both to remain usable. */
@Composable
internal fun SearchResultHeader(
    back: @Composable () -> Unit,
    field: @Composable (Modifier) -> Unit,
    filters: (@Composable (Modifier) -> Unit)?,
) {
    val compact = LocalPhoneLandscape.current
    val sideInset = LocalPageStartInset.current
    val gutters = meiPageHorizontalPadding()
    BoxWithConstraints(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        val inlineFilters = compact && filters != null && maxWidth - sideInset >= 440.dp
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(gutters).padding(vertical = if (compact) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                back()
                field(Modifier.weight(if (inlineFilters) 0.65f else 1f, fill = false))
                if (inlineFilters) filters(Modifier.weight(1f))
            }
            if (!inlineFilters) {
                val padding = if (compact) Modifier.padding(bottom = 4.dp)
                    else Modifier.padding(top = 4.dp, bottom = 8.dp)
                filters?.invoke(Modifier.padding(gutters).then(padding))
            }
        }
    }
}
