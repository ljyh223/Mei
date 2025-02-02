package com.ljyh.music.ui.screen.index.home

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.CardExtInfo
import com.ljyh.music.ui.component.CircularSearchBar
import com.ljyh.music.ui.component.HomeShimmer
import com.ljyh.music.ui.component.PlaylistCard
import com.ljyh.music.ui.component.RecommendCard
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.screen.Screen
import com.ljyh.music.utils.DateUtils.getGreeting


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {

    val navController = LocalNavController.current
    val homePageResourceShow by viewModel.homePageResourceShow.collectAsState()
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
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()))
            when (val resource = homePageResourceShow) {
                is Resource.Error -> {}
                Resource.Loading -> {
                    HomeShimmer()
                }

                is Resource.Success -> {
                    val data = resource.data
                    CircularSearchBar()
                    Spacer(Modifier.height(10.dp))
                    Text(getGreeting(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(), state = rememberLazyListState()
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_DAILY_RECOMMEND" }
                        if (cards.isEmpty()) return@LazyRow
                        items(cards[0].dslData.blockResource.resources, key = { it.resourceId }) {
                            RecommendCard(
                                picUrl = it.coverImg,
                                title = it.singleLineTitle,
                                extInfo = CardExtInfo(
                                    icon = it.iconDesc.image,
                                    text = it.subTitle
                                ),
                                viewModel = viewModel
                            ) {

                            }
                        }
                    }



                    Spacer(Modifier.height(10.dp))
                    Text(text = "雷达歌单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = rememberLazyListState()
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_RADAR" }
                        if (cards.isEmpty()) return@LazyRow
                        items(cards[0].dslData.blockResource.resources, key = { it.resourceId }) {
                            PlaylistCard(
                                id = it.resourceId,
                                title = it.title,
                                coverImg = it.coverImg,
                                subTitle = it.resourceExtInfo.coverText,
                                showPlay = true,
                                extInfo = it.resourceInteractInfo.playCount
                            ) {
                                Screen.PlayList.navigate(navController) {
                                    addPath(it.resourceId)
                                }
                            }
                        }


                    }




                    Spacer(Modifier.height(10.dp))
                    Text(text = "推荐歌单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))


                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = rememberLazyListState()
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" }
                        if (cards.isEmpty()) return@LazyRow
                        items(cards[0].dslData.blockResource.resources, key = { it.resourceId }) {
                            PlaylistCard(
                                id = it.resourceId,
                                title = it.title,
                                coverImg = it.coverImg,
                                showPlay = true,

                                extInfo = it.resourceInteractInfo.playCount,
                            ) {
                                Screen.PlayList.navigate(navController) {
                                    addPath(it.resourceId)
                                }
                            }
                        }
                    }


                }
            }
            Spacer(Modifier.height(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()))
        }
    }




}




