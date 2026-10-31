package com.ljyh.mei.ui.component.glass

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** A hero is the first list item, but may have metadata/actions below its actual image. */
@Stable
internal class MeiArtworkTopBarState(private val list: LazyListState) {
    private var artworkBottom by mutableStateOf<Float?>(null)
    private var toolbarBottom by mutableFloatStateOf(0f)

    val artworkPassed by derivedStateOf {
        // An earlier item can still be visible inside top content padding, behind the bar.
        // Only use the index fallback when that item is actually out of the viewport.
        val headerVisible = list.layoutInfo.visibleItemsInfo.any { it.index == 0 }
        (!headerVisible && list.firstVisibleItemIndex > 0) ||
            (toolbarBottom > 0f && artworkBottom?.let { it <= toolbarBottom } == true)
    }

    val artworkModifier = Modifier.onGloballyPositioned {
        artworkBottom = it.positionInRoot().y + it.size.height
    }
    val toolbarModifier = Modifier.onGloballyPositioned {
        toolbarBottom = it.positionInRoot().y + it.size.height
    }
}

@Composable
internal fun rememberMeiArtworkTopBarState(list: LazyListState, contentKey: Any): MeiArtworkTopBarState =
    remember(list, contentKey) { MeiArtworkTopBarState(list) }
