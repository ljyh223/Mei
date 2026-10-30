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
import com.ljyh.mei.data.model.domain.MediaMetadata
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
class LibraryViewModel internal constructor(
    private val context: Context,
    private val source: LibraryDataSource,
    private val localPlaylistRepository: LocalPlaylistRepository,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    @Inject constructor(
        @ApplicationContext context: Context,
        repository: UserRepository,
        playlistRepository: PlaylistRepository,
        localPlaylistRepository: LocalPlaylistRepository,
    ) : this(context, RepositoryLibraryDataSource(repository, playlistRepository), localPlaylistRepository)

    private val requestScope = scopeOverride ?: viewModelScope
    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _refreshErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val refreshErrors = _refreshErrors.asSharedFlow()
    private var refreshJob: Job? = null
    private var accountRequested = false
    private var awaitingAccount = false
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
        scope = requestScope,
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
            scope = requestScope,
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
    private var likedTracks: Map<Long, MediaMetadata> = emptyMap()
    private val selectedSection = MutableStateFlow(LibrarySection.Created)

    private val listeningFootprint: StateFlow<ListeningFootprintUi> = combine(
        _weekListenRealtime,
        _monthListenRealtime,
        _weekListenReport,
        ::resolveListeningFootprint,
    ).stateIn(
        scope = requestScope,
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
        scope = requestScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState.Loading,
    )

    private var loadedUid: String? = null
    private var observedCookie: String? = null
    private var libraryLoadJob: Job? = null
    private var accountLoadJob: Job? = null

    fun onCookieAvailable(cookie: String) {
        _signedIn.value = cookie.isNotBlank()
        if (observedCookie == cookie) return
        val cookieChanged = observedCookie != null && observedCookie != cookie
        observedCookie = cookie
        if (cookieChanged) {
            awaitingAccount = true
            refreshJob?.cancel()
            accountLoadJob?.cancel()
            libraryLoadJob?.cancel()
            likedArtworkJob?.cancel()
            accountRequested = false
            loadedUid = null
            likedArtworkId = null
            _account.value = Resource.Loading
            fallbackProfile.value = null
            clearLibraryData()
        }
        if (cookie.isNotBlank()) getUserAccount()
    }

    /** Even failed attempts belong to this session; only an explicit refresh retries them. */
    private fun getUserAccount(): Job? {
        if (accountRequested) return accountLoadJob
        accountRequested = true
        accountLoadJob = requestScope.launch {
            val result = source.getUserAccount()
            if (currentCoroutineContext().isActive) {
                _account.value = result
                (result as? Resource.Success)?.data?.profile?.let {
                    awaitingAccount = false
                    loadLibrary(LibraryProfileUi(it.userId.toString(), it.nickname, it.avatarUrl, it.signature))
                }
            }
        }
        return accountLoadJob
    }

    fun loadLibrary(profile: LibraryProfileUi, force: Boolean = false): Job? {
        val uid = profile.userId
        if (uid.isBlank() || (!force && (_isRefreshing.value || awaitingAccount))) return libraryLoadJob
        fallbackProfile.value = profile
        if (!force && loadedUid == uid) return libraryLoadJob
        val userChanged = loadedUid != uid
        loadedUid = uid
        libraryLoadJob?.cancel()
        if (userChanged) {
            likedArtworkJob?.cancel()
            likedArtworkId = null
            clearLibraryData()
        }
        // Refresh retains the existing content and covers; only its indicator changes.
        if (userChanged || playlistSync.value !is Resource.Success) playlistSync.value = Resource.Loading
        libraryLoadJob = requestScope.launch {
            coroutineScope {
                launch { _userDetail.keepLoadedData(source.getUserDetail(uid)) }
                launch { _userVipInfo.keepLoadedData(source.getUserVipInfo(uid)) }
                launch { _photoAlbum.keepLoadedData(source.getPhotoAlbum(uid)) }
                launch {
                    val result = source.getAlbumList()
                    if (result is Resource.Success || _albumList.value !is Resource.Success) {
                        _albumList.value = result
                    }
                    if (result is Resource.Error && force) _refreshErrors.tryEmit("专辑刷新失败，已保留原内容")
                }
                launch { _weekListenRealtime.keepLoadedData(source.getListenDataRealtimeReport(type = "week")) }
                launch { _monthListenRealtime.keepLoadedData(source.getListenDataRealtimeReport(type = "month")) }
                launch { _weekListenReport.keepLoadedData(source.getListenDataReport(type = "week")) }
                launch { syncUserPlaylists(uid) }
            }
        }
        return libraryLoadJob
    }

    /** Explicit user refresh owns the full account → library → liked artwork sequence. */
    fun refresh(): Job? {
        if (refreshJob?.isActive == true) return refreshJob
        if (libraryLoadJob?.isActive == true || accountLoadJob?.isActive == true) return null
        _isRefreshing.value = true
        refreshJob = requestScope.launch {
            try {
                accountRequested = true
                val result = source.getUserAccount()
                if (!currentCoroutineContext().isActive) return@launch
                if (result is Resource.Success || _account.value !is Resource.Success) _account.value = result
                if (result is Resource.Error) _refreshErrors.tryEmit("账号刷新失败，已保留原内容")
                val profile = (_account.value as? Resource.Success)?.data?.profile?.let {
                    LibraryProfileUi(it.userId.toString(), it.nickname, it.avatarUrl, it.signature)
                } ?: fallbackProfile.value
                if (profile != null) {
                    awaitingAccount = false
                    loadLibrary(profile, force = true)?.join()
                    val liked = localPlaylistRepository.getPlaylistByAuthor(profile.userId)
                        .firstOrNull { it.title.endsWith("喜欢的音乐") }
                    loadLikedArtwork(liked?.id ?: likedArtworkId, force = true)?.join()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
        return refreshJob
    }

    private fun clearLibraryData() {
        recentCollectedOrder.value = emptyList()
        _userDetail.value = Resource.Loading
        _userVipInfo.value = Resource.Loading
        _photoAlbum.value = Resource.Loading
        _albumList.value = Resource.Loading
        _weekListenRealtime.value = Resource.Loading
        _monthListenRealtime.value = Resource.Loading
        _weekListenReport.value = Resource.Loading
        playlistSync.value = Resource.Loading
        _likedArtwork.value = null
        _likedArtworks.value = emptyList()
        _likedSongTitle.value = null
        likedTracks = emptyMap()
        _likedArtworkLoading.value = false
    }

    fun refreshPhotoAlbum(uid: String) {
        requestScope.launch {
            _photoAlbum.value = Resource.Loading
            _photoAlbum.value = source.getPhotoAlbum(uid)
        }
    }

    fun selectSection(section: LibrarySection) {
        selectedSection.value = section
    }

    fun loadLikedArtwork(id: String?, force: Boolean = false): Job? {
        if (id == null || (!force && _isRefreshing.value)) return likedArtworkJob
        if (!force && likedArtworkId == id) return likedArtworkJob
        likedArtworkJob?.cancel()
        if (likedArtworkId != id) {
            _likedArtwork.value = null
            _likedArtworks.value = emptyList()
            _likedSongTitle.value = null
            likedTracks = emptyMap()
        }
        likedArtworkId = id
        _likedArtworkLoading.value = _likedArtworks.value.isEmpty()
        likedArtworkJob = requestScope.launch {
            try {
                val result = source.getPlaylistDetail(id)
                if (result is Resource.Success) {
                    val ids = likedArtworkTrackIds(result.data)
                    val songs = likedTracks.filterKeys { it in ids }.toMutableMap()
                        .apply { putAll(result.data.tracks.associateBy { it.id }) }
                    fun publishArtwork() {
                        if (likedArtworkId != id) return
                        likedTracks = songs.toMap()
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
                            source.getSongDetails(missing.map(Long::toString)).forEach { songs[it.id] = it }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Keep covers already present in the playlist detail.
                        }
                        if (currentCoroutineContext().isActive) publishArtwork()
                    }
                } else if (result is Resource.Error && force) {
                    _refreshErrors.tryEmit("封面刷新失败，已保留原内容")
                }
            } finally {
                if (currentCoroutineContext().isActive && likedArtworkId == id) {
                    _likedArtworkLoading.value = false
                }
            }
        }
        return likedArtworkJob
    }

    suspend fun preparePlaylistPlayback(id: String): Resource<ListQueue> =
        when (val result = source.getPlaylistDetail(id)) {
            is Resource.Success -> withContext(Dispatchers.Default) {
                buildLibraryPlaylistQueue(result.data)?.let { Resource.Success(it) }
                    ?: Resource.Error("歌单中暂无可播放的歌曲")
            }
            is Resource.Error -> Resource.Error("暂时无法播放，请稍后重试")
            Resource.Loading -> Resource.Loading
        }

    fun recordPlaylistPlayback(id: String) {
        requestScope.launch { localPlaylistRepository.touchPlaylist(id, System.currentTimeMillis()) }
    }

    private suspend fun syncUserPlaylists(uid: String, limit: Int = 100) {
        val orderKey = stringPreferencesKey("library.recentCollected.$uid")
        val cachedOrder = try { context.dataStore.data.first()[orderKey].orEmpty() }
            catch (_: IOException) { "" }
        val cached = cachedOrder.split(',')
            .mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0 }?.toString() }.distinct().take(2)
        if (recentCollectedOrder.value.isEmpty()) recentCollectedOrder.value = cached
        when (val networkResult = source.getUserPlaylist(uid, limit)) {
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

/** A failed manual refresh must not erase a previously usable section. */
private fun <T> MutableStateFlow<Resource<T>>.keepLoadedData(result: Resource<T>) {
    if (result !is Resource.Error || value !is Resource.Success) value = result
}
