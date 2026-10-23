package com.ljyh.mei.ui.screen.main.home

import com.ljyh.mei.ui.component.card.PlaylistCard
import com.ljyh.mei.playback.queue.QueueSource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.constants.PlaylistCardSizeTablet
import com.ljyh.mei.constants.RecommendCardHeight
import com.ljyh.mei.constants.RecommendCardHeightTablet
import com.ljyh.mei.constants.RecommendCardWidth
import com.ljyh.mei.constants.RecommendCardWidthTablet
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.eapi.homeResourcePositionComparator
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.glass.LocalMeiPageProgress
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.player.state.PlayerViewModel
import com.ljyh.mei.ui.component.MeiSectionTitle
import com.ljyh.mei.ui.component.track.PlayingImageView
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.preferences.rememberPreference
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import timber.log.Timber
import com.ljyh.mei.ui.component.layout.meiPageGutter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
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
    val sortedBlocks = remember(homePageResourceShowPage1) {
        val result = homePageResourceShowPage1
        if (result is Resource.Success) result.data.sortedWith(homeResourcePositionComparator)
        else emptyList()
    }
    val isRefreshing by remember { mutableStateOf(false) }
    val device = rememberDeviceInfo()

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

    val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pageHaze = rememberHazeState()
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val titleProgress by remember(listState, collapseDistance) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        // Capture the page and its base colour; the compact bar stays outside this source.
        Box(Modifier.fillMaxSize().hazeSource(pageHaze).background(MaterialTheme.colorScheme.surface)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.homePageResourceShow(true) },
            ) {
                CompositionLocalProvider(LocalMeiPageProgress provides titleProgress) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = statusBarTop + 12.dp,
                            bottom = playerAwarePadding.calculateBottomPadding() + 16.dp,
                        ),
                    ) {
                        item(key = "home_title") {
                            MeiLargeTitle("推荐", modifier = Modifier.padding(bottom = 16.dp))
                        }
                        when (homePageResourceShowPage1) {
                            is Resource.Success -> {
                                if (sortedBlocks.isEmpty()) {
                                    item(key = "home_empty") {
                                        Box(Modifier.fillParentMaxHeight(0.65f), contentAlignment = Alignment.Center) {
                                            HomeStatus(
                                                title = "还没有推荐内容",
                                                onRetry = { viewModel.homePageResourceShow(true) },
                                            )
                                        }
                                    }
                                }
                                sortedBlocks.forEach { block ->
                                    item(key = block.positionCode) {
                                        Column(Modifier.padding(bottom = 28.dp)) {
                                            HomeBlockItem(
                                                block = block,
                                                navController = navController,
                                                viewModel = viewModel,
                                                playerViewModel = playerViewModel,
                                                device = device,
                                            )
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                item(key = "home_error") {
                                    Box(Modifier.fillParentMaxHeight(0.65f), contentAlignment = Alignment.Center) {
                                        HomeStatus(
                                            title = "暂时无法加载推荐",
                                            onRetry = { viewModel.homePageResourceShow(true) },
                                        )
                                    }
                                }
                            }
                            Resource.Loading -> {
                                item(key = "home_loading") { HomeShimmer(viewModel = viewModel) }
                            }
                        }
                    }
                }
            }
        }
        MeiHazeTopBar(state = pageHaze, title = "推荐", progress = titleProgress)
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
    viewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    device: com.ljyh.mei.ui.component.utils.DeviceInfo
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val playerConnection = LocalPlayerConnection.current ?: return

    val playlistCardSize = if (device.isTablet) PlaylistCardSizeTablet else PlaylistCardSize
    val recommendCardWidth = if (device.isTablet) RecommendCardWidthTablet else RecommendCardWidth
    val recommendCardHeight = if (device.isTablet) RecommendCardHeightTablet else RecommendCardHeight

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var preparingIntelligence by remember { mutableStateOf(false) }

    // 解析逻辑缓存，只要 block 不变，就不会重新解析 JSON
    val blockData = remember(block) {
        Timber.tag("Block").d(block.positionCode)
        selectSpecialField(block.dslData)
    } ?: return

    Timber.tag("Block").d(block.positionCode)

    when (block.positionCode) {
        // --- 每日推荐 ---
        "PAGE_RECOMMEND_DAILY_RECOMMEND" -> {
            val resources = remember(blockData) {
                blockData["resources"]?.jsonArray.orEmpty().mapNotNull {
                    runCatching {
                        json.decodeFromJsonElement<HomePageResourceShow.Data.Block.DslData.BlockResource.Resource>(it)
                    }.onFailure { error ->
                        Timber.tag("HomeBlock").w(error, "Failed to decode ${block.positionCode} card")
                    }.getOrNull()
                }
            }

            BlockWithTitle(
                title = null,
                resources = resources
            ) { resource ->

                RecommendCard(
                    cover = resource.coverImg,
                    title = resource.singleLineTitle,
                    extInfo = CardExtInfo(
                        icon = resource.iconDesc?.image,
                        text = resource.subTitle.orEmpty()
                    ),
                    cardWidth = recommendCardWidth,
                    cardHeight = recommendCardHeight,
                    preparingPlayback = resource.resourceType == "star" && preparingIntelligence,
                    viewModel = viewModel
                ) {
                    when (resource.resourceType) {
                        "dailySongs" -> Screen.EveryDay.navigate(navController)
                        "star" -> {
                            if (!preparingIntelligence) {
                                preparingIntelligence = true
                                coroutineScope.launch {
                                    try {
                                        when (val result = playerViewModel.prepareIntelligence(
                                            resource.extInfo.songIdText.orEmpty(), resource.resourceId,
                                        )) {
                                            is Resource.Success -> {
                                                val items = result.data.map { it.toMediaItem() }
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        id = "intelligence-${System.currentTimeMillis()}",
                                                        title = "心动模式",
                                                        source = QueueSource(QueueSource.Type.RECOMMENDATION, "intelligence", "心动模式"),
                                                        items = items.map { it.mediaId to it },
                                                    ),
                                                )
                                            }
                                            is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                            Resource.Loading -> Unit
                                        }
                                    } finally {
                                        preparingIntelligence = false
                                    }
                                }
                            }
                        }

                        "fm" -> {
                            // 私人FM
                            playerConnection.fmStart(resource.resourceId)

                        }

                        "similarSo  ng" -> {
                            // 相似歌曲
                            //val id = resource.resourceId // 需要解析 json
//                            "resourceId": [
//                            "2060083093",
//                            "2060086839",
//                            "2097485069"
//                            ]
                        }

                        "similarArtist" -> {
                            // 从喜欢的艺人听起
                            // val ids = resourceId // 需要解析 json， 跳转艺人界面
                            val artistIds = runCatching {
                                json.decodeFromString<List<String>>(resource.resourceId)
                            }.getOrDefault(emptyList())
                            artistIds.firstOrNull()?.let { artistId ->
                                Screen.Artist.navigate(navController) { addPath(artistId) }
                            }
                        }

                        "playList" -> Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                    }
                }
            }
        }

        // --- 各种歌单推荐 (雷达、云村、场景等) ---
        "PAGE_RECOMMEND_RADAR",
        "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
        "PAGE_RECOMMEND_MIXED_ARTIST_PLAYLIST",
        "PAGE_RECOMMEND_RANK",
        "PAGE_RECOMMEND_MY_SHEET",
        "PAGE_RECOMMEND_COMBINATION",
        "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST",

            -> {
            // 将这些相似的逻辑合并处理，减少代码重复
            val title = if (block.positionCode == "PAGE_RECOMMEND_RADAR")
                (blockData["title"]?.jsonPrimitive?.content ?: "雷达歌单")
            else blockData["title"]?.jsonPrimitive?.content.orEmpty()

            // 处理数据源字段差异
            val resourceArray = if (block.positionCode == "PAGE_RECOMMEND_RADAR")
                (blockData["resources"] ?: blockData["blockResource"])?.jsonArray.orEmpty()
            else blockData["resources"]?.jsonArray.orEmpty()

            val resources = remember(resourceArray) {
                resourceArray.mapNotNull {
                    runCatching {
                        json.decodeFromJsonElement<HomePageResourceShow.Data.Block.DslData.BlockResource.Resource>(it)
                    }.onFailure { error ->
                        Timber.tag("HomeBlock").w(error, "Failed to decode ${block.positionCode} card")
                    }.getOrNull()
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
                    cardSize = playlistCardSize,
                ) {
                    Screen.PlayList.navigate(navController) { addPath(resource.resourceId) }
                }
            }
        }

        // --- 私人推荐歌曲 / 相似歌曲 (三行滑动) ---
        "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
        "PAGE_RECOMMEND_RED_SIMILAR_SONG" -> {
            val title = blockData["header"]?.jsonObject?.get("title")?.jsonPrimitive?.content.orEmpty()
            val itemsArray = blockData["content"]?.jsonObject?.get("items")?.jsonArray.orEmpty()

            val songsBlocks = remember(itemsArray) {
                itemsArray.mapNotNull {
                    runCatching {
                        json.decodeFromJsonElement<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>(it)
                    }.onFailure { error ->
                        Timber.tag("HomeBlock").w(error, "Failed to decode ${block.positionCode} song group")
                    }.getOrNull()
                }
            }

            Title(title)
            TripleLaneSlider(
                songsArray = songsBlocks,
                isTablet = device.isTablet,
            ) { songs, index ->
                val flatSongs = songs.flatMap { it.items }.map { it.resourceId to null }
                if (playerConnection.isPlaying(flatSongs[index].first)) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.onTrackClicked(
                        trackId = flatSongs[index].first,
                        fallbackSource = QueueSource(QueueSource.Type.RECOMMENDATION, block.positionCode, title),
                        buildQueue = {
                            ListQueue(
                                id = UUID.randomUUID().toString(),
                                title = if (block.positionCode == "PAGE_RECOMMEND_PRIVATE_RCMD_SONG") "PRIVATE_RCMD_SONG" else "RED_SIMILAR_SONG",
                                items = flatSongs,
                                startIndex = index,
                                source = QueueSource(QueueSource.Type.RECOMMENDATION, block.positionCode, title),
                            )
                        }
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
    title: String?,
    resources: List<T>,
    content: @Composable (T) -> Unit
) {
    Column {
        if (!title.isNullOrBlank()) Title(title)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = meiPageGutter()), // 左右留白
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Item 间距
        ) {
            items(resources) { resource ->
                content(resource)
            }
        }
    }
}

@Composable
fun Title(text: String, isLoading: Boolean = false) {
    MeiSectionTitle(
        text = text,
        modifier = Modifier.padding(horizontal = meiPageGutter(), vertical = 12.dp),
        isLoading = isLoading,
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TripleLaneSlider(
    songsArray: List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>,
    isTablet: Boolean = false,
    onClick: (List<HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item>, Int) -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    // 手机上的一组（通常为 3 首）就是一页；平板将连续 3 组合并为一页，形成 3 列 × 3 行。
    val lanesPerPage = if (isTablet) 3 else 1
    val pagerState = rememberPagerState(
        pageCount = { (songsArray.size + lanesPerPage - 1) / lanesPerPage }
    )

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = meiPageGutter()),
        pageSpacing = 16.dp
    ) { page ->
        val firstLaneIndex = page * lanesPerPage
        val pageLanes = songsArray.subList(
            firstLaneIndex,
            minOf(firstLaneIndex + lanesPerPage, songsArray.size)
        )

        if (!isTablet) {
            val items = pageLanes.first().items
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, song ->
                    val globalIndex = songsArray.take(firstLaneIndex).sumOf { it.items.size } + index
                    SongRow(
                        song = song,
                        currentMetadataId = currentMetadata?.id?.toString(),
                        showPause = isPlaying,
                        onClick = { onClick(songsArray, globalIndex) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pageLanes.forEachIndexed { laneOffset, lane ->
                    Column(modifier = Modifier.weight(1f)) {
                        lane.items.forEachIndexed { itemIndex, song ->
                            val globalIndex = songsArray
                                .take(firstLaneIndex + laneOffset)
                                .sumOf { it.items.size } + itemIndex
                            SongRow(
                                song = song,
                                currentMetadataId = currentMetadata?.id?.toString(),
                                showPause = isPlaying,
                                onClick = { onClick(songsArray, globalIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: HomePageResourceShow.Data.Block.DslData.HomeCommon.Content.Item.Item,
    currentMetadataId: String?,
    showPause: Boolean,
    onClick: () -> Unit
) {
    val isCurrentSong = currentMetadataId == song.resourceId && showPause
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayingImageView(
            imageUrl = song.coverUrl,
            isPlaying = isCurrentSong,
            modifier = Modifier
                .size(56.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isCurrentSong) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
internal fun HomeStatus(title: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onRetry) { Text("重新加载") }
    }
}

// 辅助函数保持不变
fun selectSpecialField(jsonObject: JsonObject): JsonObject? {
    (jsonObject["blockResource"] as? JsonObject)?.let { return it }
    val candidate = jsonObject.entries
        .filter { it.value is JsonObject }
        .maxByOrNull { it.key.length }
        ?.value as? JsonObject ?: return null
    return (candidate["blockResource"] as? JsonObject) ?: candidate
}
