package com.ljyh.mei.ui.component.player

import androidx.compose.ui.graphics.Color
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.AppContext
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.LocalPlaylistRepository
import com.ljyh.mei.di.ColorRepository
import com.ljyh.mei.di.LikeRepository
import com.ljyh.mei.di.QQSongRepository
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.model.SortOrder
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    private val userRepository: UserRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val likeRepository: LikeRepository,
    private val colorRepository: ColorRepository
) : ViewModel() {
    private val _searchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val searchResult: StateFlow<Resource<SearchResult>> = _searchResult

    private val _lyricResult = MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    val lyricResult: StateFlow<Resource<LyricResult>> = _lyricResult

    private val _lyric = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    val lyric: StateFlow<Resource<Lyric>> = _lyric

    private val _amLyric = MutableStateFlow<Resource<String>>(Resource.Loading)
    val amLyric: StateFlow<Resource<String>> = _amLyric

    private val _like = MutableStateFlow<Like?>(null)
    val like: StateFlow<Like?> = _like

    private val _networkPlaylistsState = MutableStateFlow<Resource<UserPlaylist>>(Resource.Loading)
    val networkPlaylistsState: StateFlow<Resource<UserPlaylist>> = _networkPlaylistsState

    private val _createPlaylist = MutableStateFlow<Resource<CreatePlaylistResult>>(Resource.Loading)
    val createPlaylist: StateFlow<Resource<CreatePlaylistResult>> = _createPlaylist


    var mediaMetadata: MediaMetadata? = null

    val localPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // 5秒内无订阅者则停止
            initialValue = emptyList() // 初始值为空列表
        )

    // 获取点赞状态
    fun getLike(id: String) {
        viewModelScope.launch {
            _like.value = likeRepository.getLike(id) // 只更新一次
        }
    }

    // 切换点赞状态
    // TODO 优化逻辑，需要toast提示
    fun like(id: String) {
        viewModelScope.launch {
            try {
                // 1. 缓存当前状态，避免多次查询
                val currentLike = _like.value ?: likeRepository.getLike(id)

                // 2. 确定点赞标志 (true: 点赞，false: 取消点赞)
                val like = currentLike == null

                // 3. 发送点赞状态到服务器
                repository.like(id, like)

                // 4. 更新数据库和 UI
                if (like) {
                    val newLike = Like(id)
                    likeRepository.insertLike(newLike)
                    _like.value = newLike
                } else {
                    likeRepository.deleteLike(id)
                    _like.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace() // 处理异常，防止崩溃
            }
        }
    }

    fun clear() {
        _searchResult.value = Resource.Loading
        _lyricResult.value = Resource.Loading
        _lyric.value = Resource.Loading
        _amLyric.value = Resource.Loading
        _qqSong.value = null
    }

    private val _qqSong = MutableStateFlow<QQSong?>(null)
    val qqSong: StateFlow<QQSong?> = _qqSong
    fun searchNew(keyword: String) {
        viewModelScope.launch {
            _searchResult.value = Resource.Loading
            _searchResult.value = repository.searchNew(keyword)
        }
    }


    fun getLyricNew(title: String, album: String, artist: String, duration: Long, id: Long) {
        viewModelScope.launch {
            _lyricResult.value = Resource.Loading
            _lyricResult.value = repository.getLyricNew(title, album, artist, duration, id)
        }
    }

    // 旧版 没有逐字歌词
    fun getLyric(id: String) {
        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyric(id)
        }
    }


    // 新版 有逐字歌词
    fun getLyricV1(id: String) {
        viewModelScope.launch {
            _lyric.value = Resource.Loading
            _lyric.value = repository.getLyricV1(id)
        }
    }

    fun fetchQQSong(id: String) {
        viewModelScope.launch {
            qqSongRepository.getQQSong(id)
                .catch { e ->
                    // 处理错误
                    println("Error fetching song: ${e.message}")
                }
                .collect { song ->
                    _qqSong.value = song
                }
        }
    }

    fun insertSong(song: QQSong) {
        viewModelScope.launch {
            qqSongRepository.insertSong(song)
        }
    }

    fun deleteSongById(id: String) {
        viewModelScope.launch {
            qqSongRepository.deleteSongById(id)
            _lyricResult.value = Resource.Loading
            _qqSong.value = null
        }
    }

    fun getAMLLyric(id: String) {
        viewModelScope.launch {
            _amLyric.value = Resource.Loading
            _amLyric.value = repository.getAMLLyric(id)
        }
    }

    fun getColor(url: String): Color? {
        return colorRepository.getFromMemory(url)
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