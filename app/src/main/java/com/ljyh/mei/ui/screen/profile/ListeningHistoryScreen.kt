package com.ljyh.mei.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.track.BasicTrackActionMenu
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.LibraryUiState
import com.ljyh.mei.ui.screen.main.library.LibraryViewModel
import com.ljyh.mei.ui.screen.main.library.bindLibraryAccount

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun ListeningHistoryScreen(viewModel: LibraryViewModel) {
    val nav = LocalNavController.current
    val player = LocalPlayerConnection.current
    val signedIn = bindLibraryAccount(viewModel)
    val library by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val profile = (library as? LibraryUiState.Content)?.data?.profile.takeIf { signedIn }
    val model = rememberListeningArchive(profile?.userId)
    val archive by model.state.collectAsStateWithLifecycle()
    val tracks = if (signedIn) (archive.recent as? Resource.Success)?.data.orEmpty() else emptyList()
    val source = remember { QueueSource(QueueSource.Type.HISTORY, "netease-history", "网易云播放历史") }
    val refresh: () -> Unit = { if (profile == null) viewModel.refresh() else model.refreshRecent() }
    var menuTrack by remember(profile?.userId) { mutableStateOf<TrackMenuTarget?>(null) }
    val gutter = meiPageGutter()

    BoxWithConstraints {
        val fontScale = LocalDensity.current.fontScale
        val direction = LocalLayoutDirection.current
        val padding = meiPageHorizontalPadding()
        val contentWidth = maxWidth - padding.calculateStartPadding(direction) - padding.calculateEndPadding(direction)
        val showAlbumColumn = contentWidth >= 600.dp * fontScale
        MeiCollectionPage(
            title = "播放历史",
            itemCount = tracks.size,
            itemKey = { tracks[it].song.id },
            onNavigateBack = { nav.navigateUp() },
            onRetry = refresh,
            loading = signedIn && !(profile == null && account is Resource.Error) &&
                (archive.recent is Resource.Loading || (archive.refreshing && tracks.isEmpty())),
            failed = signedIn && (archive.recent is Resource.Error || (profile == null && account is Resource.Error)),
            emptyText = if (signedIn) "网易云还没有最近收听记录" else "登录后查看网易云播放历史",
            emptyActionLabel = null,
            horizontalContentPadding = 0.dp,
            verticalItemSpacing = 0.dp,
            actions = {
                if (signedIn) MeiGlassIconButton(
                    onClick = refresh, enabled = !archive.refreshing,
                    modifier = Modifier.semantics { contentDescription = "刷新播放历史" },
                ) {
                    if (archive.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            },
            controls = {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (signedIn) Text(
                        "网易云最近收听" + if (tracks.isNotEmpty()) " · ${tracks.size} 首" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) else TextButton(onClick = { Screen.ContentSettings.navigate(nav) }) { Text("登录账号") }
                    if (signedIn) archive.refreshError?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        ) { index ->
            val track = tracks[index]
            Track(
                track = track.song,
                index = index,
                isTablet = showAlbumColumn,
                supportingText = track.playedAt?.let(::formatPlayedAt),
                onClick = {
                    player?.playQueue(ListQueue(
                        id = "netease-history", title = "网易云播放历史",
                        items = tracks.map { it.song.id.toString() to it.song.toMediaItem() },
                        startIndex = index, source = source,
                    ))
                },
                onMoreClick = { anchor -> menuTrack = TrackMenuTarget(track.song, anchor, source) },
            )
        }
    }
    BasicTrackActionMenu(menuTrack) { menuTrack = null }
}
