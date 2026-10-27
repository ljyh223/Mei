package com.ljyh.mei.ui.screen.comment.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.CommentEntry
import com.ljyh.mei.data.model.domain.FloorCommentEntry
import com.ljyh.mei.data.network.Resource
import timber.log.Timber
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun CommentItem(
    comment: CommentEntry,
    isExpanded: Boolean,
    floorComments: Resource<List<FloorCommentEntry>>,
    onToggleFloor: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
    lightText: Boolean = false,
    horizontalPadding: Dp = meiPageGutter(),
    floorHasMore: Boolean = false,
    floorLoadMoreError: Boolean = false,
    floorWaitingForReveal: Boolean = false,
    onShowMoreFloor: () -> Unit = {},
) {
    val primaryText = if (lightText) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (lightText) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    val mutedText = if (lightText) Color.White.copy(alpha = 0.68f) else MaterialTheme.colorScheme.outline
    val accentText = if (lightText) Color.White else MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
        ) {
            AsyncImage(
                model = comment.user.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = secondaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedText
                    )

                    if (comment.location?.isNotBlank() == true) {
                        Text(
                            text = "  IP: ${comment.location}",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedText
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (comment.liked) accentText else mutedText
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (comment.likedCount > 0) comment.likedCount.toString() else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.liked) accentText else mutedText
                    )
                }

                val floorCount = comment.replyCount
                if (floorCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isExpanded) "收起回复" else "展开 $floorCount 条回复",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accentText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onToggleFloor(comment.id, floorCount) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPadding + 46.dp, end = horizontalPadding, bottom = 4.dp)
                    .background(
                        if (lightText) Color.White.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                when (floorComments) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(20.dp)
                                .align(Alignment.CenterHorizontally),
                            strokeWidth = 2.dp
                        )
                    }
                    is Resource.Success -> {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            floorComments.data.forEach { floor ->
                                FloorCommentItem(comment = floor, lightText = lightText)
                            }
                            if (floorHasMore) {
                                Text(
                                    text = when {
                                        floorLoadMoreError -> "加载失败，点击重试"
                                        floorWaitingForReveal -> "加载中…"
                                        else -> "查看更多回复"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = accentText,
                                    modifier = Modifier
                                        .clickable { onShowMoreFloor() }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        Timber.d("子评论加载失败" + floorComments.message)
                        Text(
                            text = "加载失败，点击重试",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lightText) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onToggleFloor(comment.id, comment.replyCount); onToggleFloor(comment.id, comment.replyCount) }.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
