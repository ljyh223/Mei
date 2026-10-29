package com.ljyh.mei.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.api.BaseMessageResponse
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.CreatePlaylistResult
import com.ljyh.mei.data.model.api.ManipulateTrackResult
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.response.MiniPlaylistDetail
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.download.DownloadManager
import com.ljyh.mei.download.SongDownloadInfo
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.data.paging.PlaylistTrackPagingSource
import com.ljyh.mei.data.repository.LikedSongsRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repository: PlaylistRepository,
    private val userRepository: UserRepository,
    private val likedSongsRepository: LikedSongsRepository,
    private val localPlaylistRepository: com.ljyh.mei.di.repository.LocalPlaylistRepository,
) : ViewModel() {
    val searchQuery = savedState.getStateFlow("playlist_search_query", "")
    val searchActive = savedState.getStateFlow("playlist_search_active", false)
    fun setSearchQuery(query: String) { savedState["playlist_search_query"] = query }
    fun setSearchActive(active: Boolean) {
        savedState["playlist_search_active"] = active
        if (!active) setSearchQuery("")
    }
    internal val orderEditor = PlaylistOrderEditor(savedState, viewModelScope, ::loadTracksForReorder, ::saveTrackOrder)
    internal val downloads by lazy {
        PlaylistDownloadController(savedState, viewModelScope, ::prepareDownload, ::enqueueDownload)
    }

    private suspend fun prepareDownload(selection: PlaylistDownloadSelection): Resource<List<MediaMetadata>> = safeApiCall {
        val detail = (_playlistDetail.value as? Resource.Success)?.data?.takeIf { it.id == selection.playlistId }
        val known = detail?.tracks.orEmpty().associateBy(MediaMetadata::id).toMutableMap()
        selection.ids.filterNot(known::containsKey).chunked(200).forEach { ids ->
            repository.getSongDetails(ids.map(Long::toString)).forEach { known[it.id] = it }
        }
        selection.ids.mapNotNull(known::get)
    }

    private suspend fun enqueueDownload(
        selection: PlaylistDownloadSelection,
        tracks: List<MediaMetadata>,
        options: PlaylistDownloadOptions,
    ): Resource<Int> {
        val context = AppContext.instance
        val ids = tracks.map { it.id.toString() }
        val localIds = DownloadManager.existingLocalSongIds(context, ids)
        val remoteIds = ids.filterNot { it in localIds }
        val result = if (remoteIds.isNotEmpty()) repository.getSongUrlV1(remoteIds, options.quality.toMusicQuality()) else null
        val urls = if (result is Resource.Success) result.data.data.associate { it.id.toString() to (it.url to it.encodeType.orEmpty()) } else emptyMap()
        val songs = tracks.mapNotNull { track ->
            val id = track.id.toString()
            val local = id in localIds
            val (url, type) = if (local) null to "" else urls[id] ?: return@mapNotNull null
            if (!local && url == null) return@mapNotNull null
            SongDownloadInfo(id, url, track.title, track.artists.map { it.name }, track.album.title,
                track.coverUrl, track.duration, type)
        }
        if (songs.isEmpty()) return Resource.Error("无法获取歌曲链接，请稍后重试")
        DownloadManager.enqueue(context, songs, selection.title, selection.playlistId.toString(), options.path, options.separateLyrics)
        return Resource.Success(songs.size)
    }

    private var detailRequestId: String? = null
    private var detailRequest: kotlinx.coroutines.Job? = null
    private var cachedSearchKey: Triple<Resource<MiniPlaylistDetail>, String, Set<Long>>? = null
    private var cachedSearch: Flow<PagingData<MediaMetadata>>? = null

    val userId: String get() = AppContext.instance.dataStore[UserIdKey] ?: ""
    private val _playlistDetail = MutableStateFlow<Resource<MiniPlaylistDetail>>(Resource.Loading)
    val playlistDetail: StateFlow<Resource<MiniPlaylistDetail>> = _playlistDetail

    private val _removedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val removedTrackIds: StateFlow<Set<Long>> = _removedTrackIds

    private val _manipulateTracks =
        MutableStateFlow<Resource<ManipulateTrackResult>>(Resource.Loading)
    val manipulateTracks: StateFlow<Resource<ManipulateTrackResult>> = _manipulateTracks


    val playlist: StateFlow<List<Playlist>> = combine(
        localPlaylistRepository.getAllPlaylist(),
        AppContext.instance.dataStore.data.map { it[UserIdKey].orEmpty() },
    ) { playlists, uid ->
        if (uid.isBlank()) emptyList() else playlists.filter { it.author == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())


    private val _everyDay = MutableStateFlow<Resource<List<MediaMetadata>>>(Resource.Loading)
    val everyDay: StateFlow<Resource<List<MediaMetadata>>> = _everyDay

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
        if (detailRequestId == id && (detailRequest?.isActive == true || _playlistDetail.value is Resource.Success)) return
        detailRequest?.cancel()
        detailRequestId = id
        detailRequest = viewModelScope.launch {
            _removedTrackIds.value = emptySet()
            _playlistDetail.value = Resource.Loading
            val detail = repository.getPlaylistDetail(id)
            if (detailRequestId != id) return@launch
            _playlistDetail.value = detail
            val currentUserId = AppContext.instance.dataStore[UserIdKey].orEmpty()
            if (detail is Resource.Success && currentUserId.isNotEmpty() &&
                detail.data.creatorUserId.toString() == currentUserId &&
                detail.data.name.endsWith("喜欢的音乐")) {
                when (val result = likedSongsRepository.refresh(currentUserId)) {
                    is Resource.Error -> Timber.tag("LikedSongs").w("Playlist sync failed: %s", result.message)
                    else -> Unit
                }
            }
        }
    }

    fun recordPlaylistPlayback(id: String) {
        viewModelScope.launch {
            localPlaylistRepository.touchPlaylist(id, System.currentTimeMillis())
        }
    }

    suspend fun loadTracksForReorder(playlistId: Long): Resource<List<PlaylistOrderItem>> {
        val detail = (_playlistDetail.value as? Resource.Success)?.data
            ?: return Resource.Error("歌单尚未加载完成")
        val currentUserId = AppContext.instance.dataStore[UserIdKey] ?: ""
        if (detail.id != playlistId || currentUserId.isEmpty() ||
            detail.creatorUserId.toString() != currentUserId) {
            return Resource.Error("只能调整自己创建的歌单")
        }
        return safeApiCall {
            val visibleIds = detail.trackIds.filterNot { it in _removedTrackIds.value }
            val tracksById = detail.tracks.associateBy { it.id }.toMutableMap()
            visibleIds.filterNot(tracksById::containsKey).chunked(200).forEach { ids ->
                repository.getSongDetails(ids.map(Long::toString)).forEach { track ->
                    tracksById[track.id] = track
                }
            }
            visibleIds.map { id -> PlaylistOrderItem(id, tracksById[id]) }
        }
    }

    suspend fun saveTrackOrder(
        playlistId: Long,
        items: List<PlaylistOrderItem>,
    ): Resource<BaseResponse> {
        val detail = (_playlistDetail.value as? Resource.Success)?.data
            ?: return Resource.Error("歌单尚未加载完成")
        val currentUserId = AppContext.instance.dataStore[UserIdKey] ?: ""
        val ids = items.map(PlaylistOrderItem::id)
        val removedIds = _removedTrackIds.value
        val expectedIds = detail.trackIds.filterNot { it in removedIds }
        if (detail.id != playlistId || currentUserId.isEmpty() ||
            detail.creatorUserId.toString() != currentUserId) {
            return Resource.Error("只能调整自己创建的歌单")
        }
        if (ids.size != expectedIds.size || ids.toSet() != expectedIds.toSet()) {
            return Resource.Error("歌单歌曲已变化，请重新打开排序")
        }
        val result = repository.updatePlaylistTrackOrder(playlistId.toString(), ids)
        if (result is Resource.Success && result.data.code == 200) {
            val tracksById = (detail.tracks + items.mapNotNull(PlaylistOrderItem::track))
                .associateBy { it.id }
            _playlistDetail.value = Resource.Success(detail.copy(
                trackIds = ids,
                tracks = ids.mapNotNull(tracksById::get),
                count = (detail.count - removedIds.size).coerceAtLeast(0),
            ))
            _removedTrackIds.value = emptySet()
        }
        return result
    }

    // 分页加载，不是根据歌单id加载，而是根据歌曲id加载
    fun getPlaylistTracks(
        playlistDetailResource: Resource<MiniPlaylistDetail>,
        removedTrackIds: Set<Long> = emptySet()
    ): Flow<PagingData<MediaMetadata>> {
        return when (playlistDetailResource) {
            is Resource.Success -> {
                val playlist = playlistDetailResource.data

                // 【本人歌单】直接全量，不分页
                if (playlist.name.endsWith("喜欢的音乐")) {
                    flowOf(
                        PagingData.from(
                            playlist.tracks
                                .filterNot { it.id in removedTrackIds }
                        )
                    )
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                        pagingSourceFactory = {
                            PlaylistTrackPagingSource(
                                repository = repository,
                                firstData = playlist.tracks,
                                ids = playlist.trackIds.map(Long::toString)
                            )
                        }
                    ).flow.map { pagingData ->
                        pagingData.filter { track -> track.id !in removedTrackIds }
                    }
                }
            }
            else -> flowOf(PagingData.empty())
        }.cachedIn(viewModelScope)
    }

    /**
     * Loads every track in a playlist before filtering it.  The playlist detail endpoint only
     * includes an initial batch of tracks, so filtering that batch alone would miss results in
     * larger playlists.
     */
    fun searchPlaylistTracks(
        detail: Resource<MiniPlaylistDetail>,
        query: String,
        removed: Set<Long> = emptySet(),
    ): Flow<PagingData<MediaMetadata>> {
        val key = Triple(detail, query, removed.toSet())
        if (cachedSearchKey == key) cachedSearch?.let { return it }
        return createPlaylistSearch(detail, query, removed).also {
            cachedSearchKey = key
            cachedSearch = it
        }
    }

    private fun createPlaylistSearch(
        playlistDetailResource: Resource<MiniPlaylistDetail>,
        query: String,
        removedTrackIds: Set<Long> = emptySet()
    ): Flow<PagingData<MediaMetadata>> {
        if (playlistDetailResource !is Resource.Success || query.isBlank()) {
            return getPlaylistTracks(playlistDetailResource, removedTrackIds)
        }

        val playlist = playlistDetailResource.data
        return kotlinx.coroutines.flow.flow {
            val tracksById = playlist.tracks.associateBy { it.id }.toMutableMap()
            playlist.trackIds
                .filterNot(tracksById::containsKey)
                .chunked(200)
                .forEach { ids ->
                    repository.getSongDetails(ids.map(Long::toString)).forEach { track ->
                        tracksById[track.id] = track
                    }
                }

            val matchingTracks = playlist.trackIds
                .mapNotNull(tracksById::get)
                .filterNot { it.id in removedTrackIds }
                .filter { it.matchesPlaylistSearch(query) }
            emit(PagingData.from(matchingTracks))
        }.cachedIn(viewModelScope)
    }


    fun addSongToPlaylist(
        pid: String,
        trackIds: String,
        previousTrackCount: Int,
        onComplete: (PlaylistTrackAddOutcome) -> Unit = {}
    ) {
        viewModelScope.launch {
            _manipulateTracks.value = Resource.Loading
            val result = repository.manipulateTrack("add", pid, trackIds)
            _manipulateTracks.value = result
            val outcome = result.toPlaylistTrackAddOutcome(previousTrackCount)
            Timber.tag("PlaylistAdd").d(
                "pid=%s trackIds=%s previousCount=%d result=%s outcome=%s",
                pid,
                trackIds,
                previousTrackCount,
                result,
                outcome
            )
            onComplete(outcome)
        }
    }


    fun deleteSongFromPlaylist(
        pid: String,
        trackIds: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _manipulateTracks.value = Resource.Loading
            val result = repository.manipulateTrack("del", pid, trackIds)
            _manipulateTracks.value = result
            onComplete(result is Resource.Success && result.data.code == 200)
        }
    }

    fun markTrackRemoved(trackId: Long) {
        _removedTrackIds.value = _removedTrackIds.value + trackId
    }
    fun getAllMePlaylist(){
        viewModelScope.launch {
            if (userId.isNotEmpty()) {
                when (val result = userRepository.getUserPlaylist(userId, 100)) {
                    is Resource.Success -> {
                        localPlaylistRepository.mergeUserPlaylists(result.data.playlists)
                    }
                    is Resource.Error -> {}
                    Resource.Loading -> {}
                }
            }
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
            localPlaylistRepository.deletePlaylistById(id)
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

    suspend fun resolveSongUrls(ids: List<String>, quality: MusicQuality) =
        repository.getSongUrlV1(ids, quality)

    suspend fun getSongDetails(ids: List<String>): List<MediaMetadata> =
        repository.getSongDetails(ids)



}

enum class PlaylistTrackAddOutcome {
    Added,
    AlreadyExists,
    Failed
}

internal fun Resource<ManipulateTrackResult>.toPlaylistTrackAddOutcome(
    previousTrackCount: Int
): PlaylistTrackAddOutcome =
    when (this) {
        is Resource.Success -> when {
            data.code == 502 && data.message == "歌单内歌曲重复" ->
                PlaylistTrackAddOutcome.AlreadyExists
            data.code != 200 -> PlaylistTrackAddOutcome.Failed
            data.count > previousTrackCount -> PlaylistTrackAddOutcome.Added
            data.count == previousTrackCount -> PlaylistTrackAddOutcome.AlreadyExists
            else -> PlaylistTrackAddOutcome.Failed
        }
        else -> PlaylistTrackAddOutcome.Failed
    }

internal fun MediaMetadata.matchesPlaylistSearch(query: String): Boolean {
    val keyword = query.trim()
    return title.contains(keyword, ignoreCase = true) ||
        album.title.contains(keyword, ignoreCase = true) ||
        artists.any { artist ->
            artist.name.contains(keyword, ignoreCase = true) ||
                artist.alias.orEmpty().any { it.contains(keyword, ignoreCase = true) }
        }
}
