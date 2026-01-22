package com.ljyh.mei.ui.screen.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.di.LikeRepository
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
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
    private val playlistRepository: com.ljyh.mei.di.PlaylistRepository,
    val apiService: ApiService
) : ViewModel() {
    val userId = AppContext.instance.dataStore[UserIdKey] ?: ""
    private val _playlistDetail = MutableStateFlow<Resource<PlaylistDetail>>(Resource.Loading)
    val playlistDetail: StateFlow<Resource<PlaylistDetail>> = _playlistDetail

    private val _manipulateTracks =
        MutableStateFlow<Resource<ManipulateTrackResult>>(Resource.Loading)
    val manipulateTracks: StateFlow<Resource<ManipulateTrackResult>> = _manipulateTracks


    private val _playlist = MutableStateFlow<List<Playlist>>(emptyList())
    val playlist: StateFlow<List<Playlist>> = _playlist


    private val _everyDay = MutableStateFlow<Resource<EveryDaySongs>>(Resource.Loading)
    val everyDay: StateFlow<Resource<EveryDaySongs>> = _everyDay

    // 创建歌单状态
    private val _createPlaylist = MutableStateFlow<Resource<CreatePlaylistResult>>(Resource.Loading)
    val createPlaylist: StateFlow<Resource<CreatePlaylistResult>> = _createPlaylist

    // 收藏/取消收藏歌单状态
    private val _subscribePlaylist = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val subscribePlaylist: StateFlow<Resource<BaseResponse>> = _subscribePlaylist

    private val _unSubscribePlaylist = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val unSubscribePlaylist: StateFlow<Resource<BaseResponse>> = _unSubscribePlaylist

    // 删除歌单状态
    private val _deletePlaylist = MutableStateFlow<Resource<BaseMessageResponse>>(Resource.Loading)
    val deletePlaylist: StateFlow<Resource<BaseMessageResponse>> = _deletePlaylist

    fun getPlaylistDetail(id: String) {
        viewModelScope.launch {
            _playlistDetail.value = Resource.Loading
            _playlistDetail.value = repository.getPlaylistDetail(id)
        }
    }

    // 分页加载，不是根据歌单id加载，而是根据歌曲id加载
    fun getPlaylistTracks(
        playlistDetailResource: Resource<PlaylistDetail>
    ): Flow<PagingData<MediaMetadata>> {
        return when (playlistDetailResource) {
            is Resource.Success -> {
                val playlist = playlistDetailResource.data.playlist

                // 【本人歌单】直接全量，不分页
                if (playlist.name.endsWith("喜欢的音乐")) {
                    flowOf(PagingData.from(playlist.tracks.map { it.toMediaMetadata() }))
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                        pagingSourceFactory = {
                            PlaylistTrackSource(
                                apiService = apiService,
                                firstData = playlist.tracks,
                                ids = playlist.trackIds.map { it.id.toString() }
                            )
                        }
                    ).flow
                }
            }
            else -> flowOf(PagingData.empty())
        }.cachedIn(viewModelScope)
    }


    fun updateAllLike(likes: List<Like>) {
        viewModelScope.launch {
            likeRepository.updateAllLike(likes)
        }
    }

    fun addSongToPlaylist(pid: String, trackIds: String) {
        viewModelScope.launch {
            _manipulateTracks.value = Resource.Loading
            _manipulateTracks.value = repository.manipulateTrack("add", pid, trackIds)

        }
    }


    fun deleteSongFromPlaylist(pid: String, trackIds: String) {
        viewModelScope.launch {
            _manipulateTracks.value = Resource.Loading
            _manipulateTracks.value = repository.manipulateTrack("del", pid, trackIds)
        }
    }
    fun getAllMePlaylist(){
        viewModelScope.launch {
            _playlist.value = playlistRepository.getPlaylistByAuthor(userId)
        }
    }

    fun getEveryDayRecommendSongs() {
        viewModelScope.launch {
            _everyDay.value = Resource.Loading
            _everyDay.value = repository.getEveryDayRecommendSongs()
        }

    }

    /*
     * 创建歌单
     */
    fun createPlaylist(
        name: String,
        privacy: Boolean =  false, // 0 普通歌单, 10 隐私歌单
        type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
    ) {
        viewModelScope.launch {
            _createPlaylist.value = Resource.Loading
            _createPlaylist.value = repository.createPlaylist(name, privacy, type)
        }
    }

    /*
     * 收藏歌单
     */
    fun subscribePlaylist(id: String) {
        viewModelScope.launch {
            _subscribePlaylist.value = Resource.Loading
            _subscribePlaylist.value = repository.subscribePlaylist(id)
        }
    }

    /*
     * 取消收藏歌单
     */
    fun unsubscribePlaylist(id: String) {
        viewModelScope.launch {
            _unSubscribePlaylist.value = Resource.Loading
            _unSubscribePlaylist.value = repository.unSubscribePlaylist(id)
            playlistRepository.deletePlaylistById(id)
        }
    }

    /*
     * 删除歌单
     */
    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            _deletePlaylist.value = Resource.Loading
            _deletePlaylist.value = repository.deletePlaylist(id)
        }
    }



}

