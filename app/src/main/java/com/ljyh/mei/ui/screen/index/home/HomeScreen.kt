package com.ljyh.mei.ui.screen.index.home

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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.home.CardExtInfo
import com.ljyh.mei.ui.component.home.PlaylistCard
import com.ljyh.mei.ui.component.home.RecommendCard
import com.ljyh.mei.ui.component.playlist.PlayingImageView
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.DateUtils.getGreeting
import com.ljyh.mei.utils.positionComparator
import com.ljyh.mei.utils.rememberPreference
import java.util.UUID

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
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun RenderHomePageContent(
    homePageResource: List<HomePageResourceShow.Data.Block>,
    navController: NavController,
    viewModel: HomeViewModel
) {
    val gson = Gson()
    val sortedBlocks = homePageResource.sortedWith(positionComparator)
    val playerConnection = LocalPlayerConnection.current ?: return
    sortedBlocks.forEach { block ->
        Log.d("Block", block.positionCode)
        when (block.positionCode) {
            // 每日推荐
            "PAGE_RECOMMEND_DAILY_RECOMMEND" -> {
                val value = selectSpecialField(block.dslData) ?: return

                BlockWithTitle(
                    title = getGreeting(),
                    resources = value.get("resources").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }
                ) { resource ->
                    RecommendCard(
                        cover = resource.coverImg,
                        title = resource.singleLineTitle,
                        extInfo = CardExtInfo(
                            icon = resource.iconDesc.image,
                            text = resource.subTitle
                        ),
                        viewModel = viewModel
                    ) {

                        when (resource.moduleType) {
                            "daily_song_rec" -> {
                                Screen.EveryDay.navigate(navController)
                            }

                            "radar", "user_like", "tag_daily_rec", "mood", "new_song_album", "once_hear" -> {
                                Screen.PlayList.navigate(navController) {
                                    addPath(resource.resourceId)
                                }
                            }
                        }
                    }
                }
            }
            // 雷达歌单
            "PAGE_RECOMMEND_RADAR" -> {

                val value = selectSpecialField(block.dslData) ?: return
                Log.d("PAGE_RECOMMEND_RADAR", value.toString())
                BlockWithTitle(
                    title = value.get("title")?.asString ?: "雷达歌单",
                    resources = (value.get("resources")
                        ?: value.get("blockResource")).asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }
                ) { resource ->
                    PlaylistCard(
                        id = resource.resourceId,
                        title = resource.title,
                        coverImg = resource.coverImg,
                        subTitle = resource.resourceExtInfo.coverText,
                        showPlay = true,
                        extInfo = resource.resourceInteractInfo?.playCount,
                        imageSize = !resource.coverImg.contains("thumbnail"),
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }
                }

            }

            // 推荐歌单
            "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" -> {
                Log.d("PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST", block.dslData.toString())
                val value = selectSpecialField(block.dslData) ?: return
                BlockWithTitle(
                    title = value.get("title").asString,
                    resources = value.get("resources").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }
                ) { resource ->

                    PlaylistCard(
                        id = resource.resourceId,
                        title = resource.title,
                        coverImg = resource.coverImg,
                        showPlay = true,
                        extInfo = resource.resourceInteractInfo?.playCount,
                        imageSize = !resource.coverImg.contains("thumbnail")
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }

                }

            }

            "PAGE_RECOMMEND_PRIVATE_RCMD_SONG" -> {
                val value = selectSpecialField(block.dslData) ?: return
                Title(value.get("header").asJsonObject.get("title").asString)
                TripleLaneSlider(
                    songsArray = value.get("content").asJsonObject.get("items").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item::class.java
                        )
                    },
                    isPlaying = { id->
                        playerConnection.isPlaying(id)
                    }
                ) { songs, index ->



                    val flatSongs = songs.flatMap { it.items }.map { it.resourceId }
                    if(playerConnection.isPlaying(flatSongs[index])){
                        playerConnection.player.togglePlayPause()
                    }else{
                        playerConnection.playQueue(
                            ListQueue(
                                id = UUID.randomUUID().toString(),
                                title = "PRIVATE_RCMD_SONG",
                                items = flatSongs,
                                startIndex = index
                            )
                        )
                    }

                }

            }

            "PAGE_RECOMMEND_MIXED_ARTIST_PLAYLIST" -> {
                val value = selectSpecialField(block.dslData) ?: return
                BlockWithTitle(
                    title = value.get("title").asString,
                    resources = value.get("resources").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }
                ) { resource ->
                    PlaylistCard(
                        id = resource.resourceId,
                        title = resource.title,
                        coverImg = resource.coverImg,
                        showPlay = true,
                        extInfo = resource.resourceInteractInfo?.playCount,
                        imageSize = !resource.coverImg.contains("thumbnail")
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }
                }
            }

            "PAGE_RECOMMEND_RANK" -> {
                Log.d("PAGE_RECOMMEND_RANK", "${block.dslData}")
                val value = selectSpecialField(block.dslData) ?: return
                BlockWithTitle(
                    title = value.get("title").asString,
                    resources = value.get("resources").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }
                ) { resource ->

                    PlaylistCard(
                        id = resource.resourceId,
                        title = resource.title,
                        coverImg = resource.coverImg,
                        showPlay = true,
                        imageSize = !resource.coverImg.contains("thumbnail")
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }

                }
            }


            "PAGE_RECOMMEND_RED_SIMILAR_SONG" -> {
                val value = selectSpecialField(block.dslData) ?: return
                Title(value.get("header").asJsonObject.get("title").asString)
                TripleLaneSlider(
                    songsArray = value.get("content").asJsonObject.get("items").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item::class.java
                        )
                    },
                    isPlaying = { id->
                        playerConnection.isPlaying(id)
                    }
                ) { songs, index ->
                    val flatSongs = songs.flatMap { it.items }.map { it.resourceId }
                    playerConnection.playQueue(
                        ListQueue(
                            id = UUID.randomUUID().toString(),
                            title = "RED_SIMILAR_SONG",
                            items = flatSongs,
                            startIndex = index
                        )
                    )
                }
            }

            "PAGE_RECOMMEND_MY_SHEET" -> {
                val value = selectSpecialField(block.dslData) ?: return
                BlockWithTitle(
                    title = value.get("title").asString,
                    resources = value.get("resources").asJsonArray.map {
                        gson.fromJson(
                            it.asJsonObject,
                            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java
                        )
                    }.dropLast(1)
                ) { resource ->
                    PlaylistCard(
                        id = resource.resourceId ?: "",
                        title = resource.title,
                        coverImg = resource.coverImg,
                        showPlay = true,
                    ) {
                        Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }
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
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
    Spacer(modifier = Modifier.height(10.dp))
}


@Composable
fun TripleLaneSlider(
    songsArray: List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>,
    isPlaying: @Composable (String) -> Boolean,
    onClick: (List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>, Int) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { 4 }
    )
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 24.dp)
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            songsArray[page].items.forEachIndexed { index, song ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingImageView(
                        imageUrl = song.coverUrl,
                        isPlaying = isPlaying(song.resourceId),
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
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = song.artistName,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
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
                        Icon(
                            imageVector = if(isPlaying(song.resourceId)) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }


    }
}


fun selectSpecialField(jsonObject: JsonObject): JsonObject? {
    // 1. 优先返回 "blockResource"
    if (jsonObject.has("blockResource")) {
        return jsonObject.getAsJsonObject("blockResource")
    }

    // 2. 否则找键名最长的字段
    val longestEntry = jsonObject.entrySet()
        .maxByOrNull { it.key.length }

    // 3. 确保值是 JsonObject（防止是基础类型或数组）
    if (longestEntry?.value?.asJsonObject?.get("blockResource") != null)
        return longestEntry.value.asJsonObject?.get("blockResource")?.asJsonObject
    return longestEntry?.value?.asJsonObject
}