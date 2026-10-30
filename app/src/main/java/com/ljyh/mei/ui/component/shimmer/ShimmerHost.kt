package com.ljyh.mei.ui.component.shimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.valentinilk.shimmer.shimmer

private val LocalSkeletonShimmerActive = compositionLocalOf { false }

@Composable
fun SkeletonShimmerHost(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val parentActive = LocalSkeletonShimmerActive.current
    Box(modifier = if (enabled && !parentActive) modifier.shimmer() else modifier) {
        CompositionLocalProvider(LocalSkeletonShimmerActive provides (enabled || parentActive)) {
            content()
        }
    }
}
