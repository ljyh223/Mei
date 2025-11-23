package com.ljyh.mei.ui.screen.search

import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.constants.SuggestionItemHeight
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SearchBarIconOffsetX
import kotlinx.coroutines.flow.drop


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

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.updateQuery( query)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
    ) {

        when (val result=searchSuggest) {
            is Resource.Loading -> {}
            is Resource.Success -> {
                Log.d("SearchSuggest", "result: ${result.data}")
                result.data.result.songs?.let { songs->
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

                result.data.result.artists?.let { artists->
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

                result.data.result.albums?.let { albums->
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

enum class SearchType(val icon: ImageVector, val type: Int) {
    Song(Icons.Rounded.MusicNote, 1),
    Artist(Icons.Rounded.Person, 100),
    Album(Icons.Rounded.Album, 10),
    Playlist(Icons.AutoMirrored.Filled.PlaylistPlay, 1000),
    History(Icons.Rounded.History, -1)
}

