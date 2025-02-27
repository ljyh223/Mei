package com.ljyh.music.ui.screen.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SongUrl
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.repository.PlaylistRepository
import com.ljyh.music.di.LikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val likeRepository: LikeRepository,
    val apiService: ApiService
) : ViewModel() {

    private val _playlistDetail = MutableStateFlow<Resource<PlaylistDetail>>(Resource.Loading)
    val playlistDetail: StateFlow<Resource<PlaylistDetail>> = _playlistDetail


    private val _songUrl= MutableStateFlow<Resource<SongUrl>>(Resource.Loading)
    val songUrl: StateFlow<Resource<SongUrl>> = _songUrl

    fun getSongUrl(id: String) {
        viewModelScope.launch {
            _songUrl.value = Resource.Loading
            _songUrl.value = repository.getSongUrl(id)
        }
    }

    fun getPlaylistDetail(id: String) {
        viewModelScope.launch {
            _playlistDetail.value = Resource.Loading
            _playlistDetail.value = repository.getPlaylistDetail(id)
        }
    }
    fun getPlaylistTracks(
        id: String,
        currentUserId: String,
        playlistDetailResource: Resource<PlaylistDetail>
    ): Flow<PagingData<PlaylistDetail.Playlist.Track>> {
        return when (playlistDetailResource) {
            is Resource.Success -> {
                Log.d("PlaylistViewModel", "getPlaylistTracks: 成功")
                val playlistDetail = playlistDetailResource.data
                if (currentUserId == playlistDetail.playlist.creator.userId.toString()) {
                    // ✅ 本人歌单：直接返回完整的歌曲数据
                    flowOf(PagingData.from(playlistDetail.playlist.tracks))
                } else {
                    // ❌ 非本人歌单：使用分页加载
                    Pager(
                        config = PagingConfig(
                            pageSize = 20, // 每页加载的数据量
                            prefetchDistance = 5, // 预加载距离
                            enablePlaceholders = true // 是否启用占位符
                        ),
                        pagingSourceFactory = { PlaylistTrackSource(apiService,playlistDetail.playlist.tracks, playlistDetail.playlist.trackIds.map { it.id.toString() }.toList() ) }
                    ).flow
                }
            }
            is Resource.Loading -> {
                // 处于加载状态时，返回空的数据流
                Log.d("PlaylistViewModel", "getPlaylistTracks: 正在加载")
                flowOf(PagingData.empty())
            }
            is Resource.Error -> {
                // 出错时，返回空的数据流
                Log.d("PlaylistViewModel", "getPlaylistTracks: 出错")
                flowOf(PagingData.empty())
            }
        }
    }




    fun updateAllLike(likes: List<Like>){
        viewModelScope.launch {
            likeRepository.updateAllLike(likes)
        }
    }




}

