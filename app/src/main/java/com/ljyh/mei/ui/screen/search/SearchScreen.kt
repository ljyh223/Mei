package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.constants.SearchHistoryKey
import com.ljyh.mei.constants.SuggestionItemHeight
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SearchBarIconOffsetX
import com.ljyh.mei.utils.dataStore
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO: 听歌识曲功能
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val searchSuggest by viewModel.searchSuggest.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val searchHistory by remember(context) {
        context.dataStore.data.map { preferences ->
            decodeSearchHistory(preferences[SearchHistoryKey])
        }
    }.collectAsState(initial = emptyList())

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.updateInputQuery( query)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
    ) {

        if (query.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                item(key = "search_history_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .padding(start = 16.dp, end = SearchBarIconOffsetX),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("搜索记录", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = {
                            coroutineScope.launch { context.clearSearchHistory() }
                        }) {
                            Text("清空")
                        }
                    }
                }
                items(searchHistory, key = { "history:$it" }) { keyword ->
                    SuggestionItem(
                        query = keyword,
                        type = SearchType.History,
                        onClick = {
                            onSearch(keyword, SearchType.Song.type)
                            onDismiss()
                        },
                        onDelete = {
                            coroutineScope.launch { context.removeSearchHistory(keyword) }
                        },
                        onFillTextField = {
                            onQueryChange(
                                TextFieldValue(
                                    text = keyword,
                                    selection = TextRange(keyword.length),
                                ),
                            )
                        },
                    )
                }
            }
        } else when (val result=searchSuggest) {
            is Resource.Loading -> {}
            is Resource.Success -> {
                Timber.tag("SearchSuggest").d("result: ${result.data}")
                result.data.songs.let { songs->
                    items(
                        items = songs,
                        key = { it.id }
                    ) { query ->
                        SuggestionItem(
                            query = query.name,
                            type = SearchType.Song,
                            onClick = {
                                onSearch(query.name, SearchType.Song.type)
                                onDismiss()
                            },
                            onDelete = {
                            },
                            onFillTextField = {
                                onQueryChange(
                                    TextFieldValue(
                                        text = query.name,
                                        selection = TextRange(query.name.length)
                                    )
                                )
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                result.data.artists.let { artists->
                    items(
                        items = artists,
                        key = { it.id }
                    ) { query ->
                        SuggestionItem(
                            query = query.name,
                            type = SearchType.Artist,
                            onClick = {
                                onSearch(query.name, SearchType.Artist.type)
                                onDismiss()
                            },
                            onDelete = {
                            },
                            onFillTextField = {
                                onQueryChange(
                                    TextFieldValue(
                                        text = query.name,
                                        selection = TextRange(query.name.length)
                                    )
                                )
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                result.data.albums.let { albums->
                    items(
                        items = albums,
                        key = { it.id }
                    ) { query ->
                        SuggestionItem(
                            query = query.name,
                            type = SearchType.Album,
                            onClick = {
                                onSearch(query.name, SearchType.Album.type)
                                onDismiss()
                            },
                            onDelete = {
                            },
                            onFillTextField = {
                                onQueryChange(
                                    TextFieldValue(
                                        text = query.name,
                                        selection = TextRange(query.name.length)
                                    )
                                )
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

            }

            is Resource.Error -> {}
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    type: SearchType,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(SuggestionItemHeight)
            .clickable(onClick = onClick)
            .padding(end = SearchBarIconOffsetX)
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(0.5f)
        )

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (type == SearchType.History) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.5f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}
//type: 搜索类型；默认为 1 即单曲 , 取值意义 : 1: 单曲, 10: 专辑, 100: 歌手, 1000: 歌单, 1002: 用户, 1004: MV, 1006: 歌词, 1009: 电台, 1014: 视频, 1018:综合, 2000:声音(搜索声音返回字段格式会不一样)

enum class SearchType(val icon: ImageVector, val type: Int, val displayName: String) {
    Song(Icons.Rounded.MusicNote, 1, "单曲"),
    Artist(Icons.Rounded.Person, 100, "歌手"),
    Album(Icons.Rounded.Album, 10, "专辑"),
    Playlist(Icons.AutoMirrored.Filled.PlaylistPlay, 1000, "歌单"),
    History(Icons.Rounded.History, -1, "历史")
}
