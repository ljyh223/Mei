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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.weapi.CommentX
import com.ljyh.mei.data.model.weapi.FComment
import com.ljyh.mei.data.network.Resource

@Composable
fun CommentItem(
    comment: CommentX,
    isExpanded: Boolean,
    floorComments: Resource<List<FComment>>,
    onToggleFloor: (Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                // 1. 用户名：使用 labelMedium 并加粗，使其更具辨识度
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. 评论主内容：改用 bodyMedium (14sp 左右)，默认的 bodyLarge 放在列表里太笨重了
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // 【修复点1】：安全调用判断，如果 ipLocation 为空或 location 为空则不显示
                    if (!comment.ipLocation?.location.isNullOrEmpty()) {
                        Text(
                            text = "  IP: ${comment.ipLocation?.location}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp), // 点赞图标稍微缩小一点点，配合 labelSmall
                        tint = if (comment.liked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (comment.likedCount > 0) comment.likedCount.toString() else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.liked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }

                // 【修复点2】：安全获取楼中楼回复数量，如果为空则默认为 0
                val floorCount = comment.showFloorComment?.replyCount ?: 0
                if (floorCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // 3. 展开楼中楼按钮：降级到 labelSmall，并且稍微加粗使其显眼但字号不突兀
                    Text(
                        text = if (isExpanded) "收起回复" else "展开 ${floorCount} 条回复",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onToggleFloor(comment.commentId, floorCount) }
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
                    .padding(start = 62.dp, end = 16.dp, bottom = 4.dp) // 增加底部边距，防止视觉贴边
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
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
                        // 4. 楼中楼列表包装
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            floorComments.data.forEach { floor ->
                                FloorCommentItem(comment = floor)
                            }
                        }
                    }
                    is Resource.Error -> {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
