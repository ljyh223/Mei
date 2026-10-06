package com.ljyh.mei.ui.screen.main.library

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
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: UserRepository,
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
    ) { state, footprint ->
        when (state) {
            is LibraryUiState.Content -> state.copy(
                data = state.data.copy(listeningFootprint = footprint),
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

    private suspend fun syncUserPlaylists(uid: String, limit: Int = 100) {
        when (val networkResult = repository.getUserPlaylist(uid, limit)) {
            is Resource.Success -> {
                val existingPlaylists = localPlaylistRepository.getPlaylistByAuthor(uid)
                val existingMap = existingPlaylists.associateBy { it.id }
                val playlistsToInsert = networkResult.data.playlists.map {
                    val existing = existingMap[it.id.toString()]
                    Playlist(
                        id = it.id.toString(),
                        title = it.name,
                        cover = it.coverImgUrl,
                        author = it.creator.userId.toString(),
                        authorName = it.creator.nickname,
                        authorAvatar = it.creator.avatarUrl,
                        count = it.trackCount,
                        playCount = it.playCount,
                        lastPlayTime = existing?.lastPlayTime ?: 0L,
                        localPlayCount = existing?.localPlayCount ?: 0
                    )
                }
                localPlaylistRepository.insertPlaylists(playlistsToInsert)
            }
            is Resource.Error -> Unit
            Resource.Loading -> Unit
        }
    }
}
