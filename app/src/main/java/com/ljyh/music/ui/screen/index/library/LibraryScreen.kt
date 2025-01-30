package com.ljyh.music.ui.screen.index.library

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.ljyh.music.constants.UserAvatarUrlKey
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.constants.UserNicknameKey
import com.ljyh.music.data.model.UserAccount
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.PlaylistItem
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.screen.Screen
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.get
import com.ljyh.music.utils.rememberPreference
import com.ljyh.music.utils.sharedPreferencesOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@OptIn(DelicateCoroutinesApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val account by viewModel.account.collectAsState()

    val navController = LocalNavController.current
    val userPlaylist by viewModel.playlists.collectAsState()
    val scope = rememberCoroutineScope()

    val (userId, setUserId) = rememberPreference(UserIdKey, "")
    val (userNickname, setUserNickname) = rememberPreference(UserNicknameKey, "")
    val (userAvatarUrl, setUserAvatarUrl) = rememberPreference(UserAvatarUrlKey, "")

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    val tabs = listOf("创建", "收藏")

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(key1 = Unit) {
        Log.d("libraryScreen", "userId: $userId")
        if (userId != "") viewModel.getUserPlaylist(userId) else viewModel.getUserAccount()
    }


    Spacer(
        Modifier.height(
            LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
        )
    )
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (userId != "") {
            User(
                userId = userId,
                userNickname = userNickname,
                userAvatarUrl = userAvatarUrl
            )
            Spacer(Modifier.height(10.dp))
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
        when (val result = userPlaylist) {
            is Resource.Error -> {
                Log.d("libraryScreen", result.toString())
            }

            Resource.Loading -> {
                Log.d("libraryScreen", result.toString())
            }

            is Resource.Success -> {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = {
                                Text(
                                    text = title,
                                    color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary // 当前页颜色
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // 非选中页颜色
                                    fontSize = if (pagerState.currentPage == index) 18.sp else 16.sp, // 可选：改变字体大小
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal // 可选：加粗当前页字体
                                )
                            },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // 根据不同的页面显示不同的内容
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {

                        when (page) {
                            0 -> {
                                result.data.playlist.filter {
                                    it.userId.toString() == userId
                                }
                            }

                            1 -> {
                                result.data.playlist.filter {
                                    it.userId.toString() != userId
                                }
                            }

                            else -> {
                                listOf()
                            }
                        }.forEach {
                            PlaylistItem(it) {
                                Screen.PlayList.navigate(navController) {
                                    addPath(it.id.toString())
                                }
                            }
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
    userAvatarUrl: String
) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Spacer(Modifier.width(20.dp))
        AsyncImage(
            model = userAvatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(100.dp))
        )

        Spacer(Modifier.width(20.dp))

        Text(
            text = userNickname,
            modifier = Modifier
                .align(Alignment.CenterVertically),
            fontSize = 20.sp
        )
    }
}

