package com.ljyh.mei.ui.screen.main.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.response.AlbumPhoto
import com.ljyh.mei.data.model.domain.UserAccountSummary
import com.ljyh.mei.data.model.response.UserAlbumList
import com.ljyh.mei.data.model.response.UserDetail
import com.ljyh.mei.data.model.response.UserVipInfo
import com.ljyh.mei.data.model.response.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.response.ListenDataReportResponse
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.playback.queue.ListQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: UserRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
):ViewModel() {
    private val _account = MutableStateFlow<Resource<UserAccountSummary>>(Resource.Loading)
    val account: StateFlow<Resource<UserAccountSummary>> = _account

    private val _userDetail = MutableStateFlow<Resource<UserDetail>>(Resource.Loading)

    private val _userVipInfo = MutableStateFlow<Resource<UserVipInfo>>(Resource.Loading)
    private val fallbackProfile = MutableStateFlow<LibraryProfileUi?>(null)

    private val profileUi: StateFlow<LibraryProfileUi?> = combine(
        fallbackProfile,
        account,
        _userDetail,
        _userVipInfo,
    ) { fallback, accountResource, detailResource, vipResource ->
        val networkProfile = (accountResource as? Resource.Success)?.data?.profile
        val profile = networkProfile?.let {
            LibraryProfileUi(
                userId = it.userId.toString(),
                nickname = it.nickname,
                avatarUrl = it.avatarUrl,
                signature = it.signature,
            )
        } ?: fallback ?: return@combine null
        val detail = (detailResource as? Resource.Success)?.data
        val membership = (vipResource as? Resource.Success)
            ?.data
            ?.toMembershipUi(System.currentTimeMillis())

        profile.copy(
            membershipLabel = membership?.label,
            membershipIconUrl = membership?.iconUrl,
            follows = detail?.profile?.follows,
            followers = detail?.profile?.followeds,
            level = detail?.level,
            listenSongs = detail?.listenSongs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null,
    )

    private val _photoAlbum=MutableStateFlow<Resource<AlbumPhoto>>(Resource.Loading)
    val photoAlbum:StateFlow<Resource<AlbumPhoto>> = _photoAlbum

    private val _albumList = MutableStateFlow<Resource<UserAlbumList>>(Resource.Loading)

    private val _weekListenRealtime =
        MutableStateFlow<Resource<ListenDataRealtimeResponse>>(Resource.Loading)
    private val _monthListenRealtime =
        MutableStateFlow<Resource<ListenDataRealtimeResponse>>(Resource.Loading)
    private val _weekListenReport =
        MutableStateFlow<Resource<ListenDataReportResponse>>(Resource.Loading)

    private val localPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // 5秒内无订阅者则停止
            initialValue = emptyList() // 初始值为空列表
        )
    private val playlistSync = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    private val recentCollectedOrder = MutableStateFlow<List<String>>(emptyList())
    private val _likedArtworkLoading = MutableStateFlow(false)
    val likedArtworkLoading: StateFlow<Boolean> = _likedArtworkLoading
    private val _likedArtworks = MutableStateFlow<List<String>>(emptyList())
    val likedArtworks: StateFlow<List<String>> = _likedArtworks
    private val _likedArtwork = MutableStateFlow<String?>(null)
    val likedArtwork: StateFlow<String?> = _likedArtwork
    private val _likedSongTitle = MutableStateFlow<String?>(null)
    val likedSongTitle: StateFlow<String?> = _likedSongTitle
    private var likedArtworkId: String? = null
    private var likedArtworkJob: Job? = null
    private val selectedSection = MutableStateFlow(LibrarySection.Created)

    private val listeningFootprint: StateFlow<ListeningFootprintUi> = combine(
        _weekListenRealtime,
        _monthListenRealtime,
        _weekListenReport,
        ::resolveListeningFootprint,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ListeningFootprintUi(),
    )

    private val coreLibraryUiState = combine(
        account,
        profileUi,
        localPlaylists,
        _albumList,
        selectedSection,
    ) { accountResource, profile, playlists, albumResource, section ->
        resolveLibraryUiState(
            accountResource = accountResource,
            profile = profile,
            playlists = playlists,
            albumResource = albumResource,
            section = section,
            now = System.currentTimeMillis(),
        )
    }

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        coreLibraryUiState,
        listeningFootprint,
        playlistSync,
        recentCollectedOrder,
    ) { state, footprint, sync, recentIds ->
        when (state) {
            is LibraryUiState.Content -> state.copy(
                data = state.data.copy(
                    listeningFootprint = footprint,
                    playlistsLoading = sync is Resource.Loading,
                    playlistsError = (sync as? Resource.Error)?.message,
                    recentCollectedPlaylists = recentIds.mapNotNull { id -> state.data.collectedPlaylists.firstOrNull { it.id == id } },
                ),
                warning = state.warning ?: (sync as? Resource.Error)?.message,
            )
            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState.Loading,
    )

    private var loadedUid: String? = null
    private var observedCookie: String? = null
    private var libraryLoadJob: Job? = null
    private var accountLoadJob: Job? = null

    fun onCookieAvailable(cookie: String) {
        if (cookie.isBlank()) return
        val cookieChanged = observedCookie != null && observedCookie != cookie
        observedCookie = cookie
        getUserAccount(force = cookieChanged)
    }

    fun getUserAccount(force: Boolean = false) {
        if (!force && (account.value is Resource.Success || accountLoadJob?.isActive == true)) return
        accountLoadJob?.cancel()
        if (force) {
            libraryLoadJob?.cancel()
            libraryLoadJob = null
            loadedUid = null
            fallbackProfile.value = null
            _userDetail.value = Resource.Loading
            _userVipInfo.value = Resource.Loading
            _photoAlbum.value = Resource.Loading
            _albumList.value = Resource.Loading
            _weekListenRealtime.value = Resource.Loading
            _monthListenRealtime.value = Resource.Loading
            _weekListenReport.value = Resource.Loading
        }
        accountLoadJob = viewModelScope.launch {
            _account.value = Resource.Loading
            val result = repository.getUserAccount()
            if (currentCoroutineContext().isActive) {
                _account.value = result
                if (force) {
                    (result as? Resource.Success)?.data?.profile?.let { profile ->
                        loadLibrary(
                            LibraryProfileUi(
                                userId = profile.userId.toString(),
                                nickname = profile.nickname,
                                avatarUrl = profile.avatarUrl,
                                signature = profile.signature,
                            )
                        )
                    }
                }
            }
        }
    }

    fun loadLibrary(profile: LibraryProfileUi) {
        fallbackProfile.value = profile
        val uid = profile.userId
        if (uid.isBlank() || (loadedUid == uid && libraryLoadJob?.isActive == true)) return
        val userChanged = loadedUid != uid
        loadedUid = uid
        libraryLoadJob?.cancel()
        if (userChanged) {
            recentCollectedOrder.value = emptyList()
            _userDetail.value = Resource.Loading
            _userVipInfo.value = Resource.Loading
            _photoAlbum.value = Resource.Loading
            _albumList.value = Resource.Loading
            _weekListenRealtime.value = Resource.Loading
            _monthListenRealtime.value = Resource.Loading
            _weekListenReport.value = Resource.Loading
        } else if (_albumList.value is Resource.Error) {
            _albumList.value = Resource.Loading
        }
        playlistSync.value = Resource.Loading
        libraryLoadJob = viewModelScope.launch {
            coroutineScope {
                launch { _userDetail.value = repository.getUserDetail(uid) }
                launch { _userVipInfo.value = repository.getUserVipInfo(uid) }
                launch { _photoAlbum.value = repository.getPhotoAlbum(uid) }
                launch {
                    val result = repository.getAlbumList()
                    if (result is Resource.Success || _albumList.value !is Resource.Success) {
                        _albumList.value = result
                    }
                }
                launch {
                    _weekListenRealtime.value =
                        repository.getListenDataRealtimeReport(type = "week")
                }
                launch {
                    _monthListenRealtime.value =
                        repository.getListenDataRealtimeReport(type = "month")
                }
                launch {
                    _weekListenReport.value = repository.getListenDataReport(type = "week")
                }
                launch { syncUserPlaylists(uid) }
            }
        }
    }

    fun refreshPhotoAlbum(uid: String) {
        viewModelScope.launch {
            _photoAlbum.value = Resource.Loading
            _photoAlbum.value = repository.getPhotoAlbum(uid)
        }
    }

    fun selectSection(section: LibrarySection) {
        selectedSection.value = section
    }

    fun loadLikedArtwork(id: String?) {
        if (likedArtworkId == id && likedArtworkJob?.isActive == true) return
        likedArtworkJob?.cancel()
        if (likedArtworkId != id) {
            _likedArtwork.value = null
            _likedArtworks.value = emptyList()
            _likedSongTitle.value = null
        }
        likedArtworkId = id
        _likedArtworkLoading.value = id != null
        if (id == null) return
        likedArtworkJob = viewModelScope.launch {
            try {
                val result = playlistRepository.getPlaylistDetail(id)
                if (result is Resource.Success) {
                    val ids = likedArtworkTrackIds(result.data)
                    val songs = result.data.tracks.associateBy { it.id }.toMutableMap()
                    fun publishArtwork() {
                        if (likedArtworkId != id) return
                        val first = ids.firstOrNull()?.let(songs::get)
                        _likedArtwork.value = first?.coverUrl?.takeIf(String::isNotBlank)
                        _likedSongTitle.value = first?.title?.takeIf(String::isNotBlank)
                        _likedArtworks.value = ids.map { songs[it]?.coverUrl.orEmpty() }
                    }
                    // Publish available covers immediately; each image loads independently through Coil.
                    if (currentCoroutineContext().isActive) publishArtwork()
                    val missing = ids.filterNot(songs::containsKey)
                    if (missing.isNotEmpty()) {
                        try {
                            playlistRepository.getSongDetails(missing.map(Long::toString)).forEach { songs[it.id] = it }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Keep covers already present in the playlist detail.
                        }
                        if (currentCoroutineContext().isActive) publishArtwork()
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive && likedArtworkId == id) {
                    _likedArtworkLoading.value = false
                }
            }
        }
    }

    suspend fun preparePlaylistPlayback(id: String): Resource<ListQueue> =
        when (val result = playlistRepository.getPlaylistDetail(id)) {
            is Resource.Success -> withContext(Dispatchers.Default) {
                buildLibraryPlaylistQueue(result.data)?.let { Resource.Success(it) }
                    ?: Resource.Error("歌单中暂无可播放的歌曲")
            }
            is Resource.Error -> Resource.Error("暂时无法播放，请稍后重试")
            Resource.Loading -> Resource.Loading
        }

    fun recordPlaylistPlayback(id: String) {
        viewModelScope.launch { localPlaylistRepository.touchPlaylist(id, System.currentTimeMillis()) }
    }

    private suspend fun syncUserPlaylists(uid: String, limit: Int = 100) {
        val orderKey = stringPreferencesKey("library.recentCollected.$uid")
        val cachedOrder = try { context.dataStore.data.first()[orderKey].orEmpty() }
            catch (_: IOException) { "" }
        val cached = cachedOrder.split(',')
            .mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0 }?.toString() }.distinct().take(2)
        if (recentCollectedOrder.value.isEmpty()) recentCollectedOrder.value = cached
        when (val networkResult = repository.getUserPlaylist(uid, limit)) {
            is Resource.Success -> {
                localPlaylistRepository.mergeUserPlaylists(networkResult.data.playlists)
                val recentIds = recentCollectedIds(networkResult.data.playlists, uid)
                recentCollectedOrder.value = recentIds
                playlistSync.value = Resource.Success(Unit)
                try { context.dataStore.edit { it[orderKey] = recentIds.joinToString(",") } }
                catch (_: IOException) { /* Collection data is still available for this session. */ }
            }
            is Resource.Error -> playlistSync.value = Resource.Error(networkResult.message)
            Resource.Loading -> Unit
        }
    }
}
