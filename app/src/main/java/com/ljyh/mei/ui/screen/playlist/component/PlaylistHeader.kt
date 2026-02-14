package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.ui.component.playlist.FinalPerfectCollage
import com.ljyh.mei.ui.screen.playlist.ActionButton
import com.ljyh.mei.utils.rememberEnumPreference


@Composable
fun PlaylistHeader(
    title: String,
    count: Int,
    playCount: Long,
    subscribeCount: Long,
    cover:String,
    coverList: List<String>,
    creator: String,
    isSubscribed: Boolean,
    onPlayAll: () -> Unit,
    onSubscribed: (Boolean) -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
) {

    val playlistCoverStyle by rememberEnumPreference(PlaylistCoverStyleKey, defaultValue = PlaylistCoverStyle.Combination)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.size(220.dp)
        ) {

            when (playlistCoverStyle) {
                PlaylistCoverStyle.Cover -> {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                PlaylistCoverStyle.FirstSongImage -> {
                    AsyncImage(
                        model = coverList.firstOrNull(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }

                PlaylistCoverStyle.Combination -> {

                    if (coverList.size < 5) {
                        AsyncImage(
                            model = cover.firstOrNull(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        FinalPerfectCollage(
                            imageUrls = coverList,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }


        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Title & Metadata
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "By $creator",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape
            ) {
                Text(
                    text = "$count 首",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "播放 $playCount · 收藏 $subscribeCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {


            ActionButton(
                icon = actionIcon,
                text = actionLabel,
                color = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    onSubscribed(isSubscribed)
                }
            )


            // Play All Button (Prominent)
            Button(
                onClick = onPlayAll,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("播放全部")
            }

            // Download Button
            ActionButton(
                icon = Icons.Filled.Download,
                text = "下载",
                onClick = {
//                    handleDownloadClick(
//                        context = context,
//                        isCreator = isCreator,
//                        playlistDetail = playlistDetail,
//                        onShowDialog = { count, ids ->
//                            downloadCount.intValue = count
//                            downloadIds.value = ids
//                            showDownloadDialog.value = true
//                        }
//                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
