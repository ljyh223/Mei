package com.ljyh.music.ui.screen.index.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context= LocalContext.current

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
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll( rememberScrollState()),
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_DAILY_RECOMMEND" }
                        if (cards.isEmpty()) return@Row
                        cards[0].dslData.blockResource.resources.forEach {
                            RecommendCard(
                                cover = it.coverImg,
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
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll( rememberScrollState()),
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_RADAR" }
                        if (cards.isEmpty()) return@Row
                        cards[0].dslData.blockResource.resources.forEach {
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


                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        val cards =
                            data.data.blocks.filter { it.positionCode == "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" }
                        if (cards.isEmpty()) return@Row
                        cards[0].dslData.blockResource.resources.forEach {
                            PlaylistCard(
                                id = it.resourceId,
                                title = it.title,
                                coverImg = it.coverImg,
                                showPlay = true,
                                extInfo = it.resourceInteractInfo.playCount
                            ) {
                                Screen.PlayList.navigate(navController) {
                                    addPath(it.resourceId)
                                }
                            }
                        }
                    }


                }
            }

            Button(
                onClick = {
                    Screen.Test.navigate(navController)
                }
            ) {
                Text("go test")
            }
            Spacer(Modifier.height(LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()))
        }
    }




}




