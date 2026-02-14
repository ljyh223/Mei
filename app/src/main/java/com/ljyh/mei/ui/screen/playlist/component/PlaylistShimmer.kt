package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
@Composable
fun PlaylistShimmer() {
    ShimmerHost {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp

            if (isTablet) {
                TabletShimmerContent()
            } else {
                MobileShimmerContent()
            }
        }
    }
}

/**
 * 手机端骨架屏：垂直堆叠
 */
@Composable
private fun MobileShimmerContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // 封面
        TextPlaceholder(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        // 标题与信息
        TextPlaceholder(modifier = Modifier.height(32.dp).width(180.dp))
        Spacer(modifier = Modifier.height(12.dp))
        TextPlaceholder(modifier = Modifier.height(16.dp).width(120.dp))

        Spacer(modifier = Modifier.height(24.dp))
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonPlaceholder(modifier = Modifier.size(48.dp))
            TextPlaceholder(modifier = Modifier.height(48.dp).width(140.dp).clip(RoundedCornerShape(24.dp)))
            ButtonPlaceholder(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        // 歌曲列表
        repeat(8) {
            TrackItemShimmer(isTablet = false)
        }
    }
}

/**
 * 平板端骨架屏：左右布局
 */
@Composable
private fun TabletShimmerContent() {
    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧固定区域 (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(32.dp),
            horizontalAlignment = Alignment.Start // 平板端通常靠左对齐更专业
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            TextPlaceholder(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(32.dp))
            TextPlaceholder(modifier = Modifier.height(36.dp).fillMaxWidth(0.8f))
            Spacer(modifier = Modifier.height(16.dp))
            TextPlaceholder(modifier = Modifier.height(20.dp).fillMaxWidth(0.4f))

            Spacer(modifier = Modifier.height(32.dp))
            Row {
                TextPlaceholder(modifier = Modifier.height(48.dp).width(140.dp).clip(RoundedCornerShape(24.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                ButtonPlaceholder(modifier = Modifier.size(48.dp))
            }
        }

        // 右侧列表区域 (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            // 表头骨架
            Row(modifier = Modifier.padding(16.dp)) {
                TextPlaceholder(modifier = Modifier.height(12.dp).width(20.dp))
                Spacer(modifier = Modifier.width(56.dp))
                TextPlaceholder(modifier = Modifier.height(12.dp).width(60.dp))
            }
            // 歌曲列表
            repeat(10) {
                TrackItemShimmer(isTablet = true)
            }
        }
    }
}

/**
 * 单个歌曲行的骨架
 */
@Composable
private fun TrackItemShimmer(isTablet: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTablet) {
            // 序号占位
            TextPlaceholder(modifier = Modifier.width(36.dp).height(12.dp))
        }

        // 封面 + 标题权重
        Row(
            modifier = Modifier.weight(if (isTablet) 4f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextPlaceholder(
                modifier = Modifier
                    .size(if (isTablet) 40.dp else 50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                TextPlaceholder(modifier = Modifier.height(16.dp).fillMaxWidth(0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                TextPlaceholder(modifier = Modifier.height(12.dp).fillMaxWidth(0.5f))
            }
        }

        if (isTablet) {
            // 专辑占位
            TextPlaceholder(modifier = Modifier.weight(3f).height(14.dp).padding(horizontal = 16.dp))
            // 时长占位
            TextPlaceholder(modifier = Modifier.width(40.dp).height(14.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))
        ButtonPlaceholder(modifier = Modifier.size(24.dp))
    }
}