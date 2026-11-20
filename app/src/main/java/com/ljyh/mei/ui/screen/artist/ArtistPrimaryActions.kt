package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance

/** Shared for Apple artwork and the original-photo fallback; every action has a 48dp target. */
@Composable
internal fun ArtistPrimaryActions(
    canPlay: Boolean,
    subscription: ArtistSubscriptionState,
    onInfo: () -> Unit,
    onPlay: () -> Unit,
    onToggleSubscription: () -> Unit,
    compact: Boolean = false,
) {
    val foreground = MaterialTheme.colorScheme.onBackground
    val background = MaterialTheme.colorScheme.background
    val reduceTransparency = LocalMeiGlassAppearance.current.reduceTransparency
    val tint = foreground.copy(alpha = 0.12f)
    val sideColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = if (reduceTransparency) tint.compositeOver(background) else tint,
        contentColor = foreground,
        disabledContainerColor = if (reduceTransparency) tint.compositeOver(background) else tint,
        disabledContentColor = foreground.copy(alpha = 0.38f),
    )
    val sideModifier = Modifier.size(48.dp)
        .border(0.75.dp, foreground.copy(alpha = 0.10f), CircleShape)
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = onInfo, modifier = sideModifier, shape = CircleShape, colors = sideColors) {
            Icon(painterResource(R.drawable.ic_artist_info), "歌手简介", Modifier.size(28.dp), tint = foreground)
        }
        FilledIconButton(onClick = onPlay, enabled = canPlay,
            modifier = Modifier.size(if (compact) 56.dp else 72.dp), shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = foreground, contentColor = background,
                disabledContainerColor = foreground.copy(alpha = 0.25f), disabledContentColor = background)) {
            Icon(Icons.Rounded.PlayArrow, "播放热门歌曲", Modifier.size(if (compact) 32.dp else 40.dp))
        }
        FilledIconButton(onClick = onToggleSubscription, enabled = !subscription.loading && !subscription.pending,
            shape = CircleShape, colors = sideColors,
            modifier = sideModifier.semantics {
                stateDescription = when {
                    subscription.loading -> "正在加载收藏状态"
                    subscription.pending -> "正在更新收藏状态"
                    subscription.error && subscription.subscribed == null -> "收藏状态加载失败"
                    subscription.subscribed == true -> "已收藏"
                    else -> "未收藏"
                }
            }) {
            if (subscription.loading || subscription.pending) CircularProgressIndicator(
                Modifier.size(24.dp), color = foreground, strokeWidth = 2.dp)
            else Icon(painterResource(if (subscription.subscribed == true) R.drawable.ic_artist_star_filled else R.drawable.ic_artist_star),
                if (subscription.error && subscription.subscribed == null) "重试获取收藏状态"
                else if (subscription.subscribed == true) "取消收藏歌手" else "收藏歌手",
                Modifier.size(28.dp), tint = foreground)
        }
    }
}
