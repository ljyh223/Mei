package com.ljyh.mei.ui.screen.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.constants.PlaylistCardSizeTablet
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardHeightTablet
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.constants.RecommendCardWidthTablet
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.component.utils.DeviceInfo
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo

@Composable
fun HomeShimmer() {
    val device = rememberDeviceInfo()
    val recommendCardWidth = if (device.isTablet) RecommendCardWidthTablet else RecommendCardWidth
    val recommendCardHeight = if (device.isTablet) RecommendCardHeightTablet else RecommendCardHeight
    val playlistCardSize = if (device.isTablet) PlaylistCardSizeTablet else PlaylistCardSize

    ShimmerHost {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                RecommendRowShimmer(
                    cardWidth = recommendCardWidth,
                    cardHeight = recommendCardHeight,
                    count = 3
                )
            }
            repeat(3) {
                item {
                    PlaylistBlockShimmer(
                        cardSize = playlistCardSize,
                        count = 4
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendRowShimmer(
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    count: Int = 3
) {
    Column {
        TextPlaceholder(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            height = 20.dp
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(count) {
                RecommendCardShimmer(cardWidth, cardHeight)
            }
        }
    }
}

@Composable
private fun RecommendCardShimmer(
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
private fun PlaylistBlockShimmer(
    cardSize: androidx.compose.ui.unit.Dp,
    count: Int = 4
) {
    Column {
        TextPlaceholder(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            height = 20.dp
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(count) {
                PlaylistCardShimmer(cardSize)
            }
        }
    }
}

@Composable
private fun PlaylistCardShimmer(
    cardSize: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.width(cardSize)) {
        Box(
            modifier = Modifier
                .size(cardSize)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Spacer(Modifier.height(8.dp))
        TextPlaceholder(
            modifier = Modifier.fillMaxWidth(),
            height = 14.dp
        )
        Spacer(Modifier.height(4.dp))
        TextPlaceholder(
            modifier = Modifier.fillMaxWidth(0.6f),
            height = 14.dp
        )
    }
}
