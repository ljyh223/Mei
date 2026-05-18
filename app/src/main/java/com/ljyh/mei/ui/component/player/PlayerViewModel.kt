package com.ljyh.mei.ui.component.player

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.Tracks
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.weapi.Radio
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.LocalPlaylistRepository
import com.ljyh.mei.di.ColorRepository
import com.ljyh.mei.di.LikeRepository
import com.ljyh.mei.di.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.model.SortOrder
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.lyric.LyricManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    private val userRepository: UserRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val likeRepository: LikeRepository,
    private val colorRepository: ColorRepository,
    val lyricManager: LyricManager
) : ViewModel() {
    val searchResult: StateFlow<Resource<SearchResult>> = lyricManager.qqSearchResult
    val lyric: StateFlow<LyricData> = lyricManager.lyricData

    private val _like = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val like: StateFlow<Resource<Boolean>> = _like

    private val _networkPlaylistsState = MutableStateFlow<Resource<UserPlaylist>>(Resource.Loading)
    val networkPlaylistsState: StateFlow<Resource<UserPlaylist>> = _networkPlaylistsState

    private val _createPlaylist = MutableStateFlow<Resource<CreatePlaylistResult>>(Resource.Loading)
    val createPlaylist: StateFlow<Resource<CreatePlaylistResult>> = _createPlaylist


    private val _intelligenceList = MutableStateFlow<Resource<Intelligence>>(Resource.Loading)
    val intelligenceList: StateFlow<Resource<Intelligence>> = _intelligenceList


    private val _songDetail = MutableStateFlow<Resource<Tracks>>(Resource.Loading)
    val songDetail: StateFlow<Resource<Tracks>> = _songDetail


    var mediaMetadata: MediaMetadata? = null

    val userId = AppContext.instance.dataStore[UserIdKey] ?: ""

    val localPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val myPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .map { it.filter { p -> p.author == userId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // 获取点赞状态
    fun getLike(id: Long) {
        viewModelScope.launch {
            Timber.tag("PlayerViewModel").d("get like $id")
            _like.value = repository.checkSongLike(id)
        }
    }

    // 切换点赞状态
    fun like(id: String) {
        viewModelScope.launch {
            try {
                val currentLiked = (_like.value as? Resource.Success)?.data == true
                repository.like(id, !currentLiked)
                _like.value = Resource.Success(!currentLiked)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private val _qqSong = MutableStateFlow<QQSong?>(null)
    val qqSong: StateFlow<QQSong?> = _qqSong

    fun searchQQSong(keyword: String) {
        lyricManager.searchQQSong(keyword)
    }

    fun selectQQSong(song: SearchResult.Req0.Data.Body.Song.S) {
        lyricManager.selectQQSongForLyric(mediaMetadata ?: return, song)
    }

    fun insertSong(song: QQSong) {
        viewModelScope.launch {
            qqSongRepository.insertSong(song)
        }
    }

    fun deleteSongById(id: String) {
        viewModelScope.launch {
            qqSongRepository.deleteSongById(id)
            lyricManager.loadLyrics(mediaMetadata ?: return@launch, forceReload = true)
        }
    }

    fun manualTranslate() {
        lyricManager.manualTranslate()
    }

    suspend fun getQQSongId(metadataId: Long): String? {
        return qqSongRepository.getQQSong(metadataId.toString()).firstOrNull()?.qid
    }



    fun syncUserPlaylists(uid: String, limit: Int = 100) {
        viewModelScope.launch {
            _networkPlaylistsState.value = Resource.Loading
            when (val networkResult = userRepository.getUserPlaylist(uid, limit)) {
                is Resource.Success -> {
                    val playlistsToInsert = networkResult.data.playlist.map {
                        Playlist(
                            id = it.id.toString(),
                            title = it.name,
                            cover = it.coverImgUrl,
                            author = it.creator.userId.toString(),
                            authorName = it.creator.nickname,
                            authorAvatar = it.creator.avatarUrl,
                            count = it.trackCount
                        )
                    }
                    localPlaylistRepository.insertPlaylists(playlistsToInsert)
                    _networkPlaylistsState.value = networkResult
                }

                is Resource.Error -> {
                    _networkPlaylistsState.value = networkResult
                }

                Resource.Loading -> {}
            }
        }
    }

    fun createPlaylist(
        name: String,
        privacy: Boolean = false, // 0 普通歌单, 10 隐私歌单
        type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
    ) {
        viewModelScope.launch {
            _createPlaylist.value = Resource.Loading
            _createPlaylist.value = playlistRepository.createPlaylist(name, privacy, type)
        }
    }

    fun intelligenceList(id: String, playlistId: String, startSongId: String) {
        viewModelScope.launch {
            _intelligenceList.value = Resource.Loading
            _intelligenceList.value = repository.getIntelligenceList(id, playlistId, startSongId)
        }
    }

    fun getSongDetail(id:String){
        viewModelScope.launch {
            _songDetail.value = Resource.Loading
            val result = repository.getSongDetail(id)
            Timber.tag("songDetail").d("getSongDetail: $result")
            _songDetail.value = result
        }
    }

    private val _moreSortOrder = MutableStateFlow(SortOrder.FREQUENCY)
    val moreSortOrder = _moreSortOrder.asStateFlow()
    val sortedMoreActions: StateFlow<List<MoreAction>> =
        _moreSortOrder
            .map { sortOrder ->
                val actions = MoreAction.entries.toMutableList()
                sortMoreActions(actions, sortOrder)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MoreAction.entries.toList()
            )



    fun setMoreSortOrder(order: SortOrder) {
        _moreSortOrder.value = order
    }

    // 排序函数
    private fun sortMoreActions(actions: List<MoreAction>, order: SortOrder): List<MoreAction> {
        return when (order) {
            SortOrder.FREQUENCY -> actions.sortedByDescending { it.frequency }
            SortOrder.RISK -> actions.sortedBy { it.riskLevel }
        }
    }


}