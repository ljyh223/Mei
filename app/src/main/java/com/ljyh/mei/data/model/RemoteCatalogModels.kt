package com.ljyh.mei.data.model

import com.ljyh.mei.data.model.weapi.HighQualityPlaylistResult

/** Values used by the album screen, independent of the NetEase response shape. */
data class AlbumContent(
    val album: AlbumOverview,
    val songs: List<MediaMetadata>,
)

data class AlbumOverview(
    val id: Long,
    val name: String,
    val picUrl: String,
    val description: String,
    val size: Int,
    val artistNames: List<String>,
)

fun AlbumDetail.toDomain() = AlbumContent(
    album = AlbumOverview(
        id = album.id,
        name = album.name,
        picUrl = album.picUrl,
        description = album.description,
        size = album.size,
        artistNames = album.artists.map { it.name },
    ),
    songs = songs.map { it.toMediaMetadata().copy(coverUrl = album.picUrl) },
)

data class FeaturedPlaylistPage(
    val playlists: List<FeaturedPlaylist>,
    val more: Boolean,
    val lastTime: Long,
    val total: Int,
)

data class FeaturedPlaylist(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val playCount: Int,
)

fun HighQualityPlaylistResult.toDomain() = FeaturedPlaylistPage(
    playlists = playlists.map {
        FeaturedPlaylist(it.id, it.name, it.coverImgUrl, it.playCount)
    },
    more = more,
    lastTime = lasttime,
    total = total,
)

data class UserPlaylistPage(val playlists: List<UserPlaylistItem>, val more: Boolean)

data class UserPlaylistItem(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val creator: PlaylistCreator,
    val playCount: Long,
    val trackCount: Int,
)

data class PlaylistCreator(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
)

fun UserPlaylist.toDomain() = UserPlaylistPage(
    playlists = playlist.map {
        UserPlaylistItem(
            id = it.id,
            name = it.name,
            coverImgUrl = it.coverImgUrl,
            creator = PlaylistCreator(it.creator.userId, it.creator.nickname, it.creator.avatarUrl),
            playCount = it.playCount,
            trackCount = it.trackCount,
        )
    },
    more = more,
)

data class UserAccountSummary(val code: Int, val profile: AccountProfile?)

data class AccountProfile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
    val signature: String,
)

fun UserAccount.toDomain() = UserAccountSummary(
    code = code,
    profile = profile?.let { AccountProfile(it.userId, it.nickname, it.avatarUrl, it.signature) },
)
