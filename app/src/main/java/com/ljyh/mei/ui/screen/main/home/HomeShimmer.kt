package com.ljyh.mei.ui.screen.main.home

import com.ljyh.mei.ui.component.card.PlaylistCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.constants.PlaylistCardSizeTablet
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardHeightTablet
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.constants.RecommendCardWidthTablet
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun HomeShimmer(viewModel: HomeViewModel) {
    val device = rememberDeviceInfo()
    val recommendCardWidth = if (device.isTablet) RecommendCardWidthTablet else RecommendCardWidth
    val recommendCardHeight = if (device.isTablet) RecommendCardHeightTablet else RecommendCardHeight
    val playlistCardSize = if (device.isTablet) PlaylistCardSizeTablet else PlaylistCardSize

    // The page owns the title, scroll state and safe areas in every loading state.
    SkeletonShimmerHost(enabled = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            RecommendRowShimmer(
                cardWidth = recommendCardWidth,
                cardHeight = recommendCardHeight,
                count = 3,
                viewModel = viewModel,
            )
            repeat(3) {
                PlaylistBlockShimmer(cardSize = playlistCardSize, count = 4)
            }
        }
    }
}

@Composable
private fun RecommendRowShimmer(
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    count: Int = 3,
    viewModel: HomeViewModel,
) {
    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = meiPageGutter())
        ) {
            items(count) {
                RecommendCard(
                    cover = "",
                    title = "正在加载",
                    extInfo = CardExtInfo(text = "推荐内容"),
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    viewModel = viewModel,
                    isLoading = true,
                )
            }
        }
    }
}

@Composable
private fun PlaylistBlockShimmer(
    cardSize: androidx.compose.ui.unit.Dp,
    count: Int = 4
) {
    Column {
        Title(text = "", isLoading = true)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = meiPageGutter())
        ) {
            items(count) {
                PlaylistCard(
                    id = "playlist_placeholder_$it",
                    title = "",
                    coverImg = "",
                    cardSize = cardSize,
                    isLoading = true,
                    onClick = {},
                )
            }
        }
    }
}
