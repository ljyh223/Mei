package com.ljyh.music.ui.screen.index.library

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.ljyh.music.constants.AppBarHeight
import com.ljyh.music.constants.CookieKey
import com.ljyh.music.constants.UserAvatarUrlKey
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.constants.UserNicknameKey
import com.ljyh.music.constants.UserPhotoKey
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.PlaylistItem
import com.ljyh.music.ui.component.utils.fadingEdge
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.screen.Screen
import com.ljyh.music.utils.largeImage
import com.ljyh.music.utils.rememberPreference


@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val account by viewModel.account.collectAsState()

    val navController = LocalNavController.current
    val userPlaylist by viewModel.playlists.collectAsState()
    val photoAlbum by viewModel.photoAlbum.collectAsState()
    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")
    val (userPhoto, setUserPhoto) = rememberPreference(UserPhotoKey, "")
    val cookie by rememberPreference(CookieKey, defaultValue = "")
//    var userPhoto by remember { mutableStateOf("") }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(key1 = userId) {
        Log.d("libraryScreen", "userId: $userId")
        if (userId != "") {
            viewModel.getUserPlaylist(userId)
            if (userPhoto == "") {
                viewModel.getPhotoAlbum(userId)
            }
        } else if (cookie != "") {
            viewModel.getUserAccount()
        }
    }

    when (val result = photoAlbum) {
        is Resource.Error -> {
            Log.d("photoAlbum", result.toString())
        }

        Resource.Loading -> {
            Log.d("photoAlbum", result.toString())
        }

        is Resource.Success -> {
            setUserPhoto(result.data.data.records[0].imageUrl)
        }
    }
    when (val result = account) {
        is Resource.Error -> {
            Log.d("libraryScreen", result.toString())
        }

        Resource.Loading -> {
            Log.d("libraryScreen", result.toString())

        }

        is Resource.Success -> {
            //异步写入datastore
            setUserId(result.data.profile.userId.toString())
            setUserNickname(result.data.profile.nickname)
            setUserAvatarUrl(result.data.profile.avatarUrl)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Spacer(
            Modifier.height(
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
            )
        )
        if (userId != "") {
            User(
                userId = userId,
                userNickname = userNickname,
                userAvatarUrl = userAvatarUrl,
                userPhoto = userPhoto
            )
            Spacer(Modifier.height(10.dp))
        } else if (cookie == "") {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    Screen.ContentSettings.navigate(navController)
                },
            ) {
                Text("去填写cookie")

            }
        }


        when (val result = userPlaylist) {
            is Resource.Error -> {
                Log.d("libraryScreen", result.toString())
            }

            Resource.Loading -> {
                Log.d("libraryScreen", result.toString())
            }

            is Resource.Success -> {
                result.data.playlist.forEach {
                    PlaylistItem(it) {
                        Screen.PlayList.navigate(navController) {
                            addPath(it.id.toString())
                        }
                    }
                }

            }
        }
        Spacer(
            Modifier.height(
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        )
    }
}

@Composable
fun User(
    userId: String,
    userNickname: String,
    userAvatarUrl: String,
    userPhoto: String
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3)
    ) {

        if (userPhoto != "") {
            AsyncImage(
                model = userPhoto,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .fadingEdge(
                        top = WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateTopPadding(),
                        bottom = 64.dp
                    ),
                contentScale = ContentScale.Crop

            )
        }


        AsyncImage(
            model = userAvatarUrl.largeImage(),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
                .clickable { Screen.Setting.navigate(navController) }
                .clip(RoundedCornerShape(36.dp)),
        )
        Text(
            text = userNickname,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(4f, 4f),
                    blurRadius = 8f
                ),
            ),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 32.dp)

        )
    }
}

