package com.ljyh.mei.ui.screen.index.home

import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val scrollToTop by backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)
        ?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

    // 替换为 LazyListState
    val listState = rememberLazyListState()

    val homePageResourceShowPage1 by viewModel.homePageResourceShow.collectAsState()
    val userId by rememberPreference(UserIdKey, "")
    val isRefreshing by remember { mutableStateOf(false) }

    // 滚动到顶部逻辑
    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(userId) {
        viewModel.homePageResourceShow()
    }

    val systemBarsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.homePageResourceShow(true) },
        ) {
            when (val result = homePageResourceShowPage1) {
                is Resource.Success -> {
                    // 排序逻辑移出 LazyColumn，减少重组时的计算
                    val sortedBlocks = remember(result.data) {
                        result.data.sortedWith(positionComparator)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = systemBarsPadding.calculateTopPadding() + 16.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp) // 块与块之间的间距
                    ) {
                        items(
                            items = sortedBlocks,
                            // 假设 positionCode 是唯一的，用作 key 可以大幅提升性能
                            key = { it.positionCode }
                        ) { block ->
                            HomeBlockItem(
                                block = block,
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    // 这里可以加一个错误重试页面
                }
                Resource.Loading -> {
                    // 这里可以加 Loading Skeleton
                }
            }
        }
    }
}

/**
 * 独立的 Block 渲染组件，分离逻辑，保持代码清晰
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun HomeBlockItem(
    block: HomePageResourceShow.Data.Block,
    navController: NavController,
    viewModel: HomeViewModel
) {
    val gson = remember { Gson() } // 复用 Gson
    val playerConnection = LocalPlayerConnection.current ?: return

    // 解析逻辑缓存，只要 block 不变，就不会重新解析 JSON
    val blockData = remember(block) {
        Log.d("Block", block.positionCode)
        selectSpecialField(block.dslData)
    } ?: return

    Log.d("Block", block.positionCode)

    when (block.positionCode) {
        // --- 每日推荐 ---
        "PAGE_RECOMMEND_DAILY_RECOMMEND" -> {
            val resources = remember(blockData) {
                blockData.get("resources").asJsonArray.map {
                    gson.fromJson(it.asJsonObject, HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java)
                }
            }

            BlockWithTitle(
                title = getGreeting(),
                resources = resources
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
                        "daily_song_rec" -> Screen.EveryDay.navigate(navController)
                        else -> Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }
                }
            }
        }

        // --- 各种歌单推荐 (雷达、云村、场景等) ---
        "PAGE_RECOMMEND_RADAR",
        "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
        "PAGE_RECOMMEND_MIXED_ARTIST_PLAYLIST",
        "PAGE_RECOMMEND_RANK",
        "PAGE_RECOMMEND_MY_SHEET" -> {
            // 将这些相似的逻辑合并处理，减少代码重复
            val title = if(block.positionCode == "PAGE_RECOMMEND_RADAR")
                (blockData.get("title")?.asString ?: "雷达歌单")
            else blockData.get("title").asString

            // 处理数据源字段差异
            val resourceArray = if(block.positionCode == "PAGE_RECOMMEND_RADAR")
                (blockData.get("resources") ?: blockData.get("blockResource")).asJsonArray
            else blockData.get("resources").asJsonArray

            val resources = remember(resourceArray) {
                resourceArray.map {
                    gson.fromJson(it.asJsonObject, HomePageResourceShow.Data.Block.DslData.BlockResource.Resource::class.java)
                }.let { list ->
                    // 如果是我的歌单，去掉最后一个（通常是添加按钮或其他）
                    if (block.positionCode == "PAGE_RECOMMEND_MY_SHEET") list.dropLast(1) else list
                }
            }

            BlockWithTitle(title = title, resources = resources) { resource ->
                PlaylistCard(
                    id = resource.resourceId,
                    title = resource.title,
                    coverImg = resource.coverImg,
                    subTitle = resource.resourceExtInfo?.coverText,
                    showPlay = true,
                    extInfo = resource.resourceInteractInfo?.playCount,
                    // 只有非 thumbnail 的图片才使用大图加载逻辑，优化内存
                    imageSize = !resource.coverImg.contains("thumbnail"),
                ) {
                    Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                }
            }
        }

        // --- 私人推荐歌曲 / 相似歌曲 (三行滑动) ---
        "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
        "PAGE_RECOMMEND_RED_SIMILAR_SONG" -> {
            val title = blockData.get("header").asJsonObject.get("title").asString
            val itemsArray = blockData.get("content").asJsonObject.get("items").asJsonArray

            val songsBlocks = remember(itemsArray) {
                itemsArray.map {
                    gson.fromJson(it.asJsonObject, HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item::class.java)
                }
            }

            Title(title)
            TripleLaneSlider(
                songsArray = songsBlocks,
                isPlaying = { id -> playerConnection.isPlaying(id) }
            ) { songs, index ->
                val flatSongs = songs.flatMap { it.items }.map { it.resourceId to null }
                if (playerConnection.isPlaying(flatSongs[index].first)) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        ListQueue(
                            id = UUID.randomUUID().toString(),
                            title = if(block.positionCode == "PAGE_RECOMMEND_PRIVATE_RCMD_SONG") "PRIVATE_RCMD_SONG" else "RED_SIMILAR_SONG",
                            items = flatSongs,
                            startIndex = index
                        )
                    )
                }
            }
        }
    }
}

/**
 * 优化后的水平滚动行，使用 LazyRow 提升性能
 */
@Composable
private fun <T> BlockWithTitle(
    title: String,
    resources: List<T>,
    content: @Composable (T) -> Unit
) {
    Column {
        Title(title)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp), // 左右留白
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Item 间距
        ) {
            items(resources) { resource ->
                content(resource)
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(
        text = text,
        fontSize = 20.sp, // 稍微调大一点
        fontWeight = FontWeight.Bold, // 使用 Bold 而不是 Black，视觉更舒适
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun TripleLaneSlider(
    songsArray: List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>,
    isPlaying: @Composable (String) -> Boolean,
    onClick: (List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>, Int) -> Unit
) {
    // 假设每页有 3 首歌，总高度需要固定或计算
    val pagerState = rememberPagerState(pageCount = { songsArray.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
            // 给 Card 一个背景或圆角容器，如果需要卡片式设计
        ) {
            songsArray[page].items.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick(songsArray, page * 3 + index) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingImageView(
                        imageUrl = song.coverUrl,
                        isPlaying = isPlaying(song.resourceId),
                        modifier = Modifier
                            .size(56.dp) // 图片稍大一点更易点击
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.artistName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // 使用 Variant 颜色
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // 播放/暂停按钮
                    // 去掉 IconButton 的额外 padding，使其视觉对齐
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = if (isPlaying(song.resourceId)) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// 辅助函数保持不变
fun selectSpecialField(jsonObject: JsonObject): JsonObject? {
    // 1. 优先直接查找当前层级的 blockResource
    if (jsonObject.has("blockResource") && jsonObject.get("blockResource").isJsonObject) {
        return jsonObject.getAsJsonObject("blockResource")
    }

    // 2. 寻找键名最长 且 值为 JsonObject 的字段
    // 关键修复：添加 .filter { it.value.isJsonObject }
    val longestEntry = jsonObject.entrySet()
        .filter { it.value.isJsonObject } // <--- 过滤掉 boolean, string, array 等非对象类型
        .maxByOrNull { it.key.length }

    // 如果没有找到任何 JsonObject 类型的字段，直接返回 null
    val candidate = longestEntry?.value?.asJsonObject ?: return null

    // 3. 检查找到的候选对象里面是否包裹了 blockResource (递归查找逻辑)
    if (candidate.has("blockResource") && candidate.get("blockResource").isJsonObject) {
        return candidate.getAsJsonObject("blockResource")
    }

    // 4. 返回这个最长 key 对应的对象
    return candidate
}