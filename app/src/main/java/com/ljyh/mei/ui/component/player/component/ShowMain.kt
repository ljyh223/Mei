package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.IrregularityCoverKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.size1600
import com.ljyh.mei.utils.smallImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShowMain(
    viewModel: PlayerViewModel,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    playlistViewModel: PlaylistViewModel= hiltViewModel()
) {
    val context = LocalContext.current
    val coverStyle by rememberEnumPreference(CoverStyleKey, defaultValue = CoverStyle.Square)
    val enabledIrregularityCoverKey by rememberPreference(
        IrregularityCoverKey,
        defaultValue = false
    )

    val originalCover by rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )
    val isLiked by viewModel.like.collectAsState(initial = null)
    var showTrackBottomSheet by remember { mutableStateOf(false) }
    LaunchedEffect(
        key1 = mediaMetadata.id,
    ) {
        viewModel.getLike(mediaMetadata.id.toString())
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .placeholderMemoryCacheKey(mediaMetadata.coverUrl.smallImage()) // 先用小图占位
                .data(
                    if (originalCover) mediaMetadata.coverUrl else mediaMetadata.coverUrl.size1600()
                )
                .crossfade(true) // 平滑过渡
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Loaded Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    when (coverStyle) {
                        CoverStyle.Square -> RoundedCornerShape(ThumbnailCornerRadius * 2)
                        CoverStyle.Circle -> CircleShape
                    }
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

        viewModel.mediaMetadata?.let {
            TrackBottomSheet(
                showBottomSheet = showTrackBottomSheet,
                viewModel = playlistViewModel,
                mediaMetadata=it,
            ) {
                showTrackBottomSheet = false
            }
        }

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
                    AnimatedLikeButton(isLiked = isLiked != null, onToggleLike = {
                        viewModel.like(id = mediaMetadata.id.toString())
                    })
                }
                Box(modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = {
                            showTrackBottomSheet=true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
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
                tween(durationMillis = 200)
            } else {
                tween(durationMillis = 200)
            }
        },
        label = "scale"
    ) { state ->
        if (state) 1.1f else 1f
    }

    val icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder

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
                .scale(scale)
        )
    }
}