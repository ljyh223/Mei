package com.ljyh.mei.ui.component.player

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.DownloadPathKey
import com.ljyh.mei.constants.DownloadQualityKey
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.UserPlaylistPage
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.weapi.Radio
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.data.repository.DynamicCoverRepository
import com.ljyh.mei.data.repository.LikedSongsRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.di.repository.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import com.ljyh.mei.utils.lyric.LyricManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    private val userRepository: UserRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val likedSongsRepository: LikedSongsRepository,
    private val colorRepository: ColorRepository,
    private val dynamicCoverRepository: DynamicCoverRepository,
    val lyricManager: LyricManager
) : ViewModel() {
    private val _dynamicCover = MutableStateFlow<DynamicCover?>(null)
    val dynamicCover: StateFlow<DynamicCover?> = _dynamicCover.asStateFlow()
    private var coverJob: Job? = null
    private var preloadCoverJob: Job? = null
    private var coverRequest: Pair<Long, Boolean>? = null

    fun loadDynamicCover(
        song: MediaMetadata?, next: MediaMetadata?, nextId: Long?, enabled: Boolean,
        englishTitlesOnly: Boolean,
    ) {
        val request = if (enabled && song != null) song.id to englishTitlesOnly else null
        if (request != coverRequest) {
            coverJob?.cancel()
            coverRequest = request
            _dynamicCover.value = null
            if (request != null && song != null) {
                coverJob = viewModelScope.launch {
                    val cover = dynamicCoverRepository.resolve(song, englishTitlesOnly)
                    if (coverRequest == request) _dynamicCover.value = cover
                }
            }
        }
        preloadCoverJob?.cancel()
        if (request == null || song == null) return
        preloadCoverJob = viewModelScope.launch {
            // Give rapid skips time to settle before issuing a request for the next song.
            delay(5_000)
            if (next != null && next.id != song.id) {
                dynamicCoverRepository.resolve(next, englishTitlesOnly)?.let {
                    dynamicCoverRepository.prefetch(it.portraitVariant() ?: it)
                }
            } else if (next == null && nextId != null && nextId != song.id) {
                dynamicCoverRepository.prefetchNetease(nextId)
            }
        }
    }

    fun fallbackDynamicCover(song: MediaMetadata) {
        val current = _dynamicCover.value ?: return
        if (current.songId != song.id) return
        viewModelScope.launch {
            val fallback = if (current.source == DynamicCover.Source.APPLE_MUSIC) {
                dynamicCoverRepository.neteaseFallback(song)
            } else null
            if (_dynamicCover.value == current) _dynamicCover.value = fallback
        }
    }
    val searchResult: StateFlow<Resource<List<MusicTrack>>> = lyricManager.qqSearchResult
    val lyric: StateFlow<LyricData> = lyricManager.lyricData

    private val _like = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val like: StateFlow<Resource<Boolean>> = _like
    private val likeActionLock = Mutex()
    private var likeSongId: Long? = null
    private var likeObservationJob: Job? = null
    private val pendingLikeActions = mutableMapOf<String, Int>()
    private val _likeFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val likeFeedback: SharedFlow<String> = _likeFeedback.asSharedFlow()

    private val _networkPlaylistsState = MutableStateFlow<Resource<UserPlaylistPage>>(Resource.Loading)
    val networkPlaylistsState: StateFlow<Resource<UserPlaylistPage>> = _networkPlaylistsState

    private val _createPlaylist = MutableStateFlow<Resource<CreatePlaylistResult>>(Resource.Loading)
    val createPlaylist: StateFlow<Resource<CreatePlaylistResult>> = _createPlaylist


    private val _intelligenceList = MutableStateFlow<Resource<Intelligence>>(Resource.Loading)
    val intelligenceList: StateFlow<Resource<Intelligence>> = _intelligenceList


    private val _songDetail = MutableStateFlow<Resource<List<MediaMetadata>>>(Resource.Loading)
    val songDetail: StateFlow<Resource<List<MediaMetadata>>> = _songDetail


    var mediaMetadata: MediaMetadata? = null

    val userId: String get() = AppContext.instance.dataStore[UserIdKey] ?: ""

    val localPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val myPlaylists: StateFlow<List<Playlist>> = combine(
        localPlaylistRepository.getAllPlaylist(),
        AppContext.instance.dataStore.data.map { it[UserIdKey].orEmpty() },
    ) { playlists, uid ->
        if (uid.isBlank()) emptyList() else playlists.filter { it.author == uid }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun observeLike(id: Long) {
        likeObservationJob?.cancel()
        likeSongId = id
        _like.value = Resource.Loading
        likeObservationJob = viewModelScope.launch {
            likedSongsRepository.observeLiked(id.toString()).collect { liked ->
                if (likeSongId == id && pendingLikeActions[id.toString()] == null) {
                    _like.value = Resource.Success(liked)
                }
            }
        }
    }

    fun like(id: String) {
        if (id.toLongOrNull() != likeSongId) return
        val current = (_like.value as? Resource.Success)?.data ?: return
        val desired = !current
        _like.value = Resource.Success(desired)
        pendingLikeActions[id] = (pendingLikeActions[id] ?: 0) + 1
        viewModelScope.launch {
            likeActionLock.withLock {
                try {
                    likedSongsRepository.setLiked(id, desired)
                    _likeFeedback.emit(if (desired) "已添加到我喜欢的音乐" else "已取消喜欢")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Timber.tag("PlayerViewModel").e(e, "Failed to update song like")
                    _likeFeedback.emit("红心操作失败：${e.message ?: "请重试"}")
                } finally {
                    val remaining = (pendingLikeActions[id] ?: 1) - 1
                    if (remaining > 0) pendingLikeActions[id] = remaining
                    else {
                        pendingLikeActions.remove(id)
                        if (likeSongId?.toString() == id) {
                            _like.value = Resource.Success(likedSongsRepository.isLiked(id))
                        }
                    }
                }
            }
        }
    }


    private val _qqSong = MutableStateFlow<QQSong?>(null)
    val qqSong: StateFlow<QQSong?> = _qqSong

    fun searchQQSong(keyword: String) {
        lyricManager.searchQQSong(keyword)
    }

    fun selectQQSong(song: MusicTrack) {
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

    suspend fun getQQSongId(metadataId: Long): String? {
        return qqSongRepository.getQQSong(metadataId.toString()).firstOrNull()?.qid
    }



    fun syncUserPlaylists(uid: String, limit: Int = 100) {
        viewModelScope.launch {
            _networkPlaylistsState.value = Resource.Loading
            when (val networkResult = userRepository.getUserPlaylist(uid, limit)) {
                is Resource.Success -> {
                    val playlistsToInsert = networkResult.data.playlists.map {
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

    fun downloadSong(
        metadata: MediaMetadata,
        context: android.content.Context,
        saveSeparateLyrics: Boolean = false,
    ) {
        viewModelScope.launch {
            val songId = metadata.id.toString()
            val downloadPath = AppContext.instance.dataStore[DownloadPathKey]
                ?: com.ljyh.mei.download.DownloadManager.getDefaultDownloadPath()
            if (songId in com.ljyh.mei.download.DownloadManager.existingLocalSongIds(context, listOf(songId))) {
                com.ljyh.mei.download.DownloadManager.enqueue(
                    context = context,
                    songs = listOf(
                        com.ljyh.mei.download.SongDownloadInfo(
                            songId = songId,
                            url = null,
                            songTitle = metadata.title,
                            songArtist = metadata.artists.map { it.name },
                            songAlbum = metadata.album.title,
                            songCover = metadata.coverUrl,
                            duration = metadata.duration,
                        )
                    ),
                    playlistName = "单曲下载",
                    downloadPath = downloadPath,
                    saveSeparateLyrics = saveSeparateLyrics,
                )
                android.widget.Toast.makeText(context, "已添加到歌词修复队列", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            val quality = try {
                val saved = AppContext.instance.dataStore[DownloadQualityKey]
                if (saved != null) com.ljyh.mei.constants.DownloadQuality.valueOf(saved).toMusicQuality()
                else MusicQuality.EXHIGH
            } catch (_: Exception) {
                MusicQuality.EXHIGH
            }

            val result = playlistRepository.getSongUrlV1(
                ids = listOf(songId),
                quality = quality
            )

            if (result is Resource.Success) {
                val songData = result.data.data.firstOrNull()
                val url = songData?.url
                if (url != null) {
                    com.ljyh.mei.download.DownloadManager.enqueue(
                        context = context,
                        songs = listOf(
                            com.ljyh.mei.download.SongDownloadInfo(
                                songId = songId,
                                url = url,
                                songTitle = metadata.title,
                                songArtist = metadata.artists.map { it.name },
                                songAlbum = metadata.album.title,
                                songCover = metadata.coverUrl,
                                duration = metadata.duration,
                                fileType = songData.encodeType.orEmpty()
                            )
                        ),
                        playlistName = "单曲下载",
                        downloadPath = downloadPath,
                        saveSeparateLyrics = saveSeparateLyrics,
                    )
                    android.widget.Toast.makeText(context, "已添加到下载队列", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "无法获取歌曲链接", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "获取链接失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

}
