package com.ljyh.music.ui.screen.index.home

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFlatMap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.network.Resource
import com.ljyh.music.playback.queue.ListQueue
import com.ljyh.music.ui.component.CardExtInfo
import com.ljyh.music.ui.component.PlaylistCard
import com.ljyh.music.ui.component.RecommendCard
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.ui.screen.Screen
import com.ljyh.music.utils.DateUtils.getGreeting
import com.ljyh.music.utils.positionComparator
import com.ljyh.music.utils.rearrangeArray
import com.ljyh.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)
        ?.collectAsState()
    val scrollState = rememberScrollState()

    val homePageResourceShowPage1 by viewModel.homePageResourceShow.collectAsState()

    val userId by rememberPreference(UserIdKey, "")
    val isRefreshing by remember { mutableStateOf(false) }

    // 滚动到顶部逻辑
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }


    LaunchedEffect(userId) {
        viewModel.homePageResourceShow()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.homePageResourceShow(true)
            },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)

            ) {
                Spacer(
                    modifier = Modifier.height(
                        LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
                    )
                )

                // 渲染首页资源
                when (val result = homePageResourceShowPage1) {
                    is Resource.Error -> {}
                    Resource.Loading -> {}
                    is Resource.Success -> {
                        RenderHomePageContent(
                            homePageResource = result.data,
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }


                // 测试按钮
                Button(onClick = { Screen.Test.navigate(navController) }) {
                    Text("Go Test")
                }

                Spacer(
                    modifier = Modifier.height(
                        LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            .calculateBottomPadding()
                    )
                )
            }
        }

    }
}

/**
 * 渲染首页内容的子组件
 */
@Composable
private fun RenderHomePageContent(
    homePageResource: List<HomePageResourceShow.Data.Block>,
    navController: NavController,
    viewModel: HomeViewModel
) {

    val sortedBlocks = homePageResource.sortedWith(positionComparator)
    val playerConnection = LocalPlayerConnection.current ?: return
    sortedBlocks.forEach { block ->
        Log.d("Block", block.positionCode)
        when (block.positionCode) {
            // 每日推荐
            "PAGE_RECOMMEND_DAILY_RECOMMEND" -> BlockWithTitle(
                title = getGreeting(),
                resources = block.dslData.blockResource.resources
            ) { resource ->
                RecommendCard(
                    cover = resource.coverImg,
                    title = resource.singleLineTitle,
                    extInfo = CardExtInfo(
                        icon = resource.iconDesc.image,
                        text = resource.subTitle
                    ),
                    viewModel = viewModel
                ) {}
            }

            "PAGE_RECOMMEND_RADAR" -> BlockWithTitle(
                title = "雷达歌单",
                resources = block.dslData.blockResource.resources
            ) { resource ->
                PlaylistCard(
                    id = resource.resourceId,
                    title = resource.title,
                    coverImg = resource.coverImg,
                    subTitle = resource.resourceExtInfo.coverText,
                    showPlay = true,
                    extInfo = resource.resourceInteractInfo.playCount
                ) {
                    Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                }
            }

            // 推荐歌单
            "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" -> BlockWithTitle(
                title = "推荐歌单",
                resources = block.dslData.blockResource.resources
            ) { resource ->
                PlaylistCard(
                    id = resource.resourceId,
                    title = resource.title,
                    coverImg = resource.coverImg,
                    showPlay = true,
                    extInfo = resource.resourceInteractInfo.playCount
                ) {
                    Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                }
            }

            "PAGE_RECOMMEND_RANK" -> {
                Log.d("Rank", "${block.dslData}")
                BlockWithTitle(
                    title = "排行榜",
                    resources = block.dslData.rank.resources
                ) { resource ->

                    PlaylistCard(
                        id = resource.resourceId,
                        title = resource.title,
                        coverImg = resource.coverImg,
                        showPlay = true,
                        imageSize = false
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }

                }
            }


            "PAGE_RECOMMEND_PRIVATE_RCMD_SONG" -> {
                Log.d("Private song", "${block.dslData}")
                Title("为你推荐")
                TripleLaneSlider(
                    songsArray = block.dslData.homeCommon.content.items
                ) { songs, index ->

                    val flatSongs = songs.flatMap { it.items }.map { it.resourceId }
                    playerConnection.playQueue(
                        ListQueue(
                            title = "PRIVATE_RCMD_SONG",
                            items = rearrangeArray(index, flatSongs)
                        )
                    )
                }

            }
        }
    }
}

/**
 * 带标题的块组件
 */
@Composable
private fun <T> BlockWithTitle(
    title: String,
    resources: List<T>,
    content: @Composable (T) -> Unit
) {
    Title(title)
    HorizontalScrollRow {
        resources.forEach { resource ->
            content(resource)
        }
    }
}

/**
 * 水平滚动的 Row 组件
 */
@Composable
private fun HorizontalScrollRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        content()
    }
}

/**
 * 标题组件
 */
@Composable
fun Title(text: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(10.dp))
}


@Composable
fun TripleLaneSlider(
    songsArray: List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>,
    onClick: (List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>, Int) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { 4 }
    )
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 32.dp)
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            songsArray[page].items.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = song.artistName,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            onClick(
                                songsArray,
                                page * 3 + index
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }


    }
}

