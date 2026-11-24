package com.ljyh.mei.ui.screen.profile

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.playback.queue.ListQueue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.screen.main.library.*
import com.ljyh.mei.ui.screen.setting.ShareViewModel

@Composable
internal fun PersonalCenterEntry(onClick: () -> Unit, avatarUrl: String? = null) {
    val (cachedAvatar) = rememberPreference(UserAvatarUrlKey, "")
    val (cookie) = rememberPreference(CookieKey, "")
    val avatar = avatarUrl?.takeIf { it.isNotBlank() } ?: cachedAvatar.takeIf { cookie.isNotBlank() }
    MeiGlassIconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = "个人中心" }) {
        Box(Modifier.requiredSize(34.dp).clip(CircleShape), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Icon(Icons.Rounded.Person, contentDescription = null)
            if (!avatar.isNullOrBlank()) AsyncImage(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun PersonalCenterScreen(
    viewModel: LibraryViewModel,
    accountManager: ShareViewModel = hiltViewModel(),
    content: @Composable (PersonalCenterState, PersonalCenterActions) -> Unit = { state, actions -> PersonalCenterContent(state, actions) },
) {
    val nav = LocalNavController.current
    val signedIn = bindLibraryAccount(viewModel)
    val footprint by viewModel.listeningFootprint.collectAsStateWithLifecycle()
    val reportStatus by viewModel.listeningReportStatus.collectAsStateWithLifecycle()
    val player = LocalPlayerConnection.current
    val library by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val detail by viewModel.userDetail.collectAsStateWithLifecycle()
    val vip by viewModel.userVipInfo.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.refreshErrors.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    }
    val profile = if (signedIn) (library as? LibraryUiState.Content)?.data?.profile else null
    val archiveModel = rememberListeningArchive(profile?.userId.takeIf { signedIn })
    val archive by archiveModel.state.collectAsStateWithLifecycle()
    val state = PersonalCenterState(
        signedIn = signedIn, profile = profile,
        membership = profileMembership(vip, System.currentTimeMillis()),
        loading = signedIn && profile == null && account is Resource.Loading,
        refreshing = refreshing || archive.refreshing,
        footprint = if (signedIn) footprint else ListeningFootprintUi(),
        reportStatus = reportStatus,
        archive = if (signedIn) archive else ListeningArchiveState(),
        accountError = signedIn && (account is Resource.Error ||
            (account is Resource.Success && ((account as Resource.Success).data.code != 200 || (account as Resource.Success).data.profile == null))),
        statisticsError = detail is Resource.Error || (detail is Resource.Success && (detail as Resource.Success).data.code != 200),
    )
    content(state, PersonalCenterActions(
        onBack = { nav.navigateUp() },
        onRefresh = { viewModel.refresh(); archiveModel.refresh() },
        onManageAccount = { Screen.ContentSettings.navigate(nav) },
        onLogout = { confirmLogout = true },
        onRankPeriod = archiveModel::selectPeriod,
        onOpenRecentListening = { Screen.ListeningHistory.navigate(nav) },
        onPlay = { tracks, index, title ->
            if (index in tracks.indices) player?.playQueue(ListQueue(
                id = "listening-archive", title = title,
                items = tracks.map { it.song.id.toString() to null }, startIndex = index,
            ))
        },
    ))
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("退出登录？") },
        text = { Text("退出后可重新登录其他账号。") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; accountManager.saveCookie("") }) { Text("退出登录") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } },
    )
}
