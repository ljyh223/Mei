package com.ljyh.music.ui.component.player

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.music.constants.ThumbnailCornerRadius
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.playback.PlayerConnection
import com.ljyh.music.utils.size1600
import com.ljyh.music.utils.smallImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShowMain(
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    modifier: Modifier
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(false) }
    LaunchedEffect(mediaMetadata.id) {
        isLiked = withContext(Dispatchers.IO) {
            playerConnection.isLike(mediaMetadata.id.toString())
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        AsyncImage(
            model = ImageRequest.Builder(context)
                .placeholderMemoryCacheKey(mediaMetadata.coverUrl.smallImage()) // 先用小图占位
                .data(mediaMetadata.coverUrl.size1600())
                .crossfade(true) // 平滑过渡
                .build(),
            contentDescription = "Loaded Image",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(ThumbnailCornerRadius * 2)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                playerConnection.player.seekBack()
                            } else {
                                playerConnection.player.seekForward()
                            }
                        }
                    )
                }
        )


        Spacer(Modifier.height(96.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(5f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .basicMarquee()
                )

                if (mediaMetadata.artists.isNotEmpty()) {
                    Text(
                        text = mediaMetadata.artists.joinToString(separator = ", ") { it.name },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier
                            .basicMarquee()
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier.weight(2f)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedLikeButton(isLiked = isLiked, onToggleLike = {
                        playerConnection.toggleLike(mediaMetadata.id.toString())
                        isLiked = !isLiked
                    })
                }
                Box(modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = {
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }


}


@Composable
fun AnimatedLikeButton(
    isLiked: Boolean, // Pass the like state as a parameter
    onToggleLike: () -> Unit // Callback for when the button is clicked
) {
    val transition = updateTransition(targetState = isLiked, label = "like_transition")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                // Animation when transitioning to liked state
                tween(durationMillis = 200)
            } else {
                // Animation when transitioning to unliked state
                tween(durationMillis = 200)
            }
        },
        label = "scale"
    ) { state ->
        if (state) 1.2f else 1f // Scale up slightly when liked
    }

    val icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder

    IconButton(
        onClick = onToggleLike,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .scale(scale) // Apply the scale animation
        )
    }
}