package com.ljyh.mei.ui.screen.main.library

import android.widget.Toast
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.constants.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.main.library.component.LibraryBrowseLayout
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val nav = LocalNavController.current
    val context = LocalContext.current
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val signedIn = bindLibraryAccount(viewModel)
    val likedArtwork by viewModel.likedArtwork.collectAsStateWithLifecycle()
    val likedPlaylist = (uiState as? LibraryUiState.Content)?.data?.likedPlaylist()
    val likedId = likedPlaylist?.id
    LaunchedEffect(signedIn, likedId, likedPlaylist?.count) { viewModel.loadLikedArtwork(likedId.takeIf { signedIn }) }
    val search = LocalLibrarySearchState.current
    var preparingPlayback by remember { mutableStateOf(false) }
    SideEffect { search?.state = if (signedIn) uiState else null }
    DisposableEffect(search) { onDispose { search?.state = null } }

    LibraryBrowseLayout(
        state = uiState,
        likedArtwork = likedArtwork,
        signedIn = signedIn,
        scrollBehavior = scrollBehavior,
        preparingPlayback = preparingPlayback,
        canPlay = player != null,
        onPlayLiked = { id ->
            if (!preparingPlayback && player != null) scope.launch {
                preparingPlayback = true
                try {
                    when (val result = viewModel.preparePlaylistPlayback(id)) {
                        is Resource.Success -> {
                            player.playQueue(result.data)
                            viewModel.recordPlaylistPlayback(id)
                        }
                        is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        Resource.Loading -> Unit
                    }
                } finally { preparingPlayback = false }
            }
        },
        onOpenPlaylist = { id -> Screen.PlayList.navigate(nav) { addPath(id) } },
        onOpenCollection = { collection -> Screen.LibraryCollection.navigate(nav) { addPath(collection.name) } },
        onLogin = { Screen.ContentSettings.navigate(nav) },
        onRetry = { viewModel.getUserAccount(force = true) },
    )
}

/** Overview and secondary collections share the parent entry's account and cached resources. */
@Composable
internal fun bindLibraryAccount(viewModel: LibraryViewModel): Boolean {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (nickname, setNickname) = rememberPreference(UserNicknameKey, "")
    val (avatar, setAvatar) = rememberPreference(UserAvatarUrlKey, "")
    val cookie by rememberPreference(CookieKey, "")
    val profile = (account as? Resource.Success)?.data?.profile
    val seed = LibraryProfileUi(profile?.userId?.toString() ?: userId,
        profile?.nickname ?: nickname, profile?.avatarUrl ?: avatar, profile?.signature.orEmpty())
    LaunchedEffect(seed) { if (seed.userId.isNotBlank()) viewModel.loadLibrary(seed) }
    LaunchedEffect(cookie) { viewModel.onCookieAvailable(cookie) }
    LaunchedEffect(account) {
        profile?.let { setUserId(it.userId.toString()); setNickname(it.nickname); setAvatar(it.avatarUrl) }
    }
    return cookie.isNotBlank() || seed.userId.isNotBlank()
}
