package com.ljyh.mei.ui.screen.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.image.largeImage

@Composable
fun RecommendCard(
    cover: String,
    title: String? = null,
    extInfo: CardExtInfo,
    showPlay: Boolean = false,
    cardWidth: Dp = RecommendCardWidth,
    cardHeight: Dp = RecommendCardHeight,
    viewModel: HomeViewModel,
    isLoading: Boolean = false,
    onPlayClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    var extractedColor by remember(cover) {
        mutableStateOf(viewModel.getCachedColor(cover) ?: Color.DarkGray)
    }

    LaunchedEffect(cover, isLoading) {
        if (!isLoading) {
            extractedColor = viewModel.getOrExtractColor(cover)
        }
    }

    // Gently soften saturated artwork colors and give the text a little contrast headroom.
    val contrastTone = if (extractedColor.luminance() > 0.179f) Color.White else Color.Black
    val baseColor by animateColorAsState(
        targetValue = lerp(extractedColor, contrastTone, 0.08f),
        label = "recommendCardColor",
    )
    val foregroundColor = if (baseColor.luminance() > 0.179f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .skeleton(isLoading, RoundedCornerShape(8.dp))
    ) {
        if (!isLoading) AsyncImage(
            model = cover.largeImage(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(cardHeight),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        // Blend into the artwork gradually; reserve the opaque color for the bottom edge.
        if (!isLoading) Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(cardHeight * 0.64f)
                .background(
                    Brush.verticalGradient(
                        0f to baseColor.copy(alpha = 0f),
                        0.3f to baseColor.copy(alpha = 0.32f),
                        0.48f to baseColor.copy(alpha = 0.9f),
                        0.62f to baseColor.copy(alpha = 0.98f),
                        0.78f to baseColor,
                        1f to baseColor,
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.orEmpty(),
                        maxLines = 1,
                        fontSize = 14.sp,
                        color = foregroundColor,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        extInfo.icon?.takeIf { it.isNotBlank() }?.let { icon ->
                            AsyncImage(
                                model = icon,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = extInfo.text,
                            maxLines = 1,
                            fontSize = 12.sp,
                            color = foregroundColor,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showPlay || onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick ?: onClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(foregroundColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "播放",
                            tint = foregroundColor
                        )
                    }
                }
            }
        }
    }
}
data class CardExtInfo(val icon: String?=null, val text: String)
