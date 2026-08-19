package com.ljyh.mei.ui.screen.main.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserDetail
import com.ljyh.mei.data.model.UserVipInfo
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: UserRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
):ViewModel() {
    private val _account = MutableStateFlow<Resource<UserAccount>>(Resource.Loading)
    val account: StateFlow<Resource<UserAccount>> = _account

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

    private val localPlaylists: StateFlow<List<Playlist>> = localPlaylistRepository.getAllPlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // 5秒内无订阅者则停止
            initialValue = emptyList() // 初始值为空列表
        )
    private val selectedSection = MutableStateFlow(LibrarySection.Created)

    val libraryUiState: StateFlow<LibraryUiState> = combine(
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState.Loading,
    )

    private var loadedUid: String? = null
    private var libraryLoadJob: Job? = null

    fun getUserAccount() {
        if (account.value is Resource.Success) return
        viewModelScope.launch {
            _account.value = Resource.Loading
            _account.value = repository.getUserAccount()
        }
    }

    fun loadLibrary(profile: LibraryProfileUi) {
        fallbackProfile.value = profile
        val uid = profile.userId
        if (uid.isBlank() || (loadedUid == uid && libraryLoadJob?.isActive == true)) return
        loadedUid = uid
        libraryLoadJob?.cancel()
        _userDetail.value = Resource.Loading
        _userVipInfo.value = Resource.Loading
        _photoAlbum.value = Resource.Loading
        _albumList.value = Resource.Loading
        libraryLoadJob = viewModelScope.launch {
            coroutineScope {
                launch { _userDetail.value = repository.getUserDetail(uid) }
                launch { _userVipInfo.value = repository.getUserVipInfo(uid) }
                launch { _photoAlbum.value = repository.getPhotoAlbum(uid) }
                launch { _albumList.value = repository.getAlbumList() }
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
                val playlistsToInsert = networkResult.data.playlist.map {
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
