package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.UserAccountSummary
import com.ljyh.mei.data.model.domain.UserPlaylistPage
import com.ljyh.mei.data.model.response.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.UserRepository

/** Network boundary for the library session; repositories retain protocol and mapping ownership. */
internal interface LibraryDataSource {
    suspend fun getUserAccount(): Resource<UserAccountSummary>
    suspend fun getUserDetail(uid: String): Resource<UserDetail>
    suspend fun getUserVipInfo(uid: String): Resource<UserVipInfo>
    suspend fun getPhotoAlbum(uid: String): Resource<AlbumPhoto>
    suspend fun getAlbumList(): Resource<UserAlbumList>
    suspend fun getListenDataRealtimeReport(type: String): Resource<ListenDataRealtimeResponse>
    suspend fun getListenDataReport(type: String): Resource<ListenDataReportResponse>
    suspend fun getUserPlaylist(uid: String, limit: Int): Resource<UserPlaylistPage>
    suspend fun getPlaylistDetail(id: String): Resource<MiniPlaylistDetail>
    suspend fun getSongDetails(ids: List<String>): List<MediaMetadata>
}

internal class RepositoryLibraryDataSource(
    private val user: UserRepository,
    private val playlists: PlaylistRepository,
) : LibraryDataSource {
    override suspend fun getUserAccount() = user.getUserAccount()
    override suspend fun getUserDetail(uid: String) = user.getUserDetail(uid)
    override suspend fun getUserVipInfo(uid: String) = user.getUserVipInfo(uid)
    override suspend fun getPhotoAlbum(uid: String) = user.getPhotoAlbum(uid)
    override suspend fun getAlbumList() = user.getAlbumList()
    override suspend fun getListenDataRealtimeReport(type: String) = user.getListenDataRealtimeReport(type)
    override suspend fun getListenDataReport(type: String) = user.getListenDataReport(type)
    override suspend fun getUserPlaylist(uid: String, limit: Int) = user.getUserPlaylist(uid, limit)
    override suspend fun getPlaylistDetail(id: String) = playlists.getPlaylistDetail(id)
    override suspend fun getSongDetails(ids: List<String>) = playlists.getSongDetails(ids)
}
