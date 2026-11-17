package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun CenteredArtworkPane(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable (availableHeight: Dp) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val viewportHeight = maxHeight
        val availableHeight = viewportHeight - padding.calculateTopPadding() - padding.calculateBottomPadding()
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = viewportHeight).padding(padding),
                contentAlignment = Alignment.Center,
            ) { content(availableHeight) }
        }
    }
}
