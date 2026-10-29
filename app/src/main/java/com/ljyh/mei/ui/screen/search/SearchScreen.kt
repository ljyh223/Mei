package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    showTitle: Boolean = false,
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
    var showAllHistory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { it }
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.updateInputQuery(query)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        // The host owns system/IME insets and reserves the input bar's measured height.
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (showTitle) item(key = "search_title") {
            MeiLargeTitle("搜索")
        }
        if (query.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                item(key = "search_history_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .padding(start = meiPageGutter(), end = (meiPageGutter() - 12.dp).coerceAtLeast(0.dp)),
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
                items(if (showAllHistory) searchHistory else searchHistory.take(5), key = { "history:$it" }) { keyword ->
                    SuggestionItem(
                        query = keyword,
                        type = SearchType.History,
                        onClick = {
                            onSearch(keyword, SearchType.All.type)
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
                if (searchHistory.size > 5) item(key = "search_history_expand") {
                    TextButton(
                        onClick = { showAllHistory = !showAllHistory },
                        modifier = Modifier.padding(horizontal = (meiPageGutter() - 12.dp).coerceAtLeast(0.dp)),
                    ) {
                        Text(if (showAllHistory) "收起" else "查看全部记录")
                    }
                }
            } else item(key = "search_history_empty") {
                SearchStatus("暂无搜索记录")
            }
        } else {
            item(key = "submit_search") {
                SuggestionItem(
                    query = "搜索“${query.trim()}”",
                    type = SearchType.Song,
                    icon = Icons.Rounded.Search,
                    onClick = { onSearch(query.trim(), SearchType.All.type); onDismiss() },
                )
            }
            when (val result = searchSuggest) {
                is Resource.Loading -> item(key = "search_loading") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("正在查找建议", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is Resource.Success -> {
                    if (result.data.songs.isEmpty() && result.data.artists.isEmpty() && result.data.albums.isEmpty()) {
                        item(key = "search_suggestions_empty") { SearchStatus("暂无匹配建议，可以直接搜索") }
                    }
                    result.data.songs.let { songs->
                        items(
                            items = songs,
                            key = { "song:${it.id}" }
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
                            key = { "artist:${it.id}" }
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
                            key = { "album:${it.id}" }
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

                is Resource.Error -> item(key = "search_suggestions_error") {
                    SearchStatus("搜索建议暂不可用，可以直接搜索")
                }
            }
        }
    }
}

@Composable
private fun SearchStatus(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    type: SearchType,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: (() -> Unit)? = null,
    icon: ImageVector = type.icon,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SuggestionItemHeight)
            .clickable(onClick = onClick)
            .padding(start = meiPageGutter(), end = (meiPageGutter() - 12.dp).coerceAtLeast(0.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
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
                    contentDescription = "删除搜索记录：$query"
                )
            }
        }

        if (onFillTextField != null) IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "填入搜索词：$query"
            )
        }
    }
}
//type: 搜索类型；默认为 1 即单曲 , 取值意义 : 1: 单曲, 10: 专辑, 100: 歌手, 1000: 歌单, 1002: 用户, 1004: MV, 1006: 歌词, 1009: 电台, 1014: 视频, 1018:综合, 2000:声音(搜索声音返回字段格式会不一样)

enum class SearchType(val icon: ImageVector, val type: Int, val displayName: String) {
    All(Icons.Rounded.Search, 1018, "综合"),
    Song(Icons.Rounded.MusicNote, 1, "单曲"),
    Artist(Icons.Rounded.Person, 100, "歌手"),
    Album(Icons.Rounded.Album, 10, "专辑"),
    Playlist(Icons.AutoMirrored.Filled.PlaylistPlay, 1000, "歌单"),
    History(Icons.Rounded.History, -1, "历史")
}
