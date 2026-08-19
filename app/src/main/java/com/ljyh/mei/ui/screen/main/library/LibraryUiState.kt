package com.ljyh.mei.ui.screen.main.library

import androidx.compose.runtime.Immutable
import com.ljyh.mei.data.model.UserVipInfo
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.model.Album

@Immutable
data class LibraryProfileUi(
    val userId: String,
    val nickname: String,
    val avatarUrl: String,
    val signature: String,
    val membershipLabel: String? = null,
    val membershipIconUrl: String? = null,
    val follows: Int? = null,
    val followers: Int? = null,
    val level: Int? = null,
    val listenSongs: Int? = null,
)

@Immutable
data class MembershipUi(val label: String, val iconUrl: String?)

internal fun UserVipInfo.toMembershipUi(now: Long): MembershipUi? {
    val vipData = data ?: return null
    val membership = listOf(
        "SVIP" to vipData.redplus,
        "VIP" to vipData.associator,
        "音乐包" to vipData.musicPackage,
    ).firstOrNull { (_, benefit) ->
        benefit != null && benefit.vipLevel > 0 && benefit.expireTime > now
    } ?: return null

    return MembershipUi(
        label = membership.first,
        iconUrl = membership.second?.iconUrl
            ?: if (membership.first == "VIP") vipData.redVipLevelIcon else null,
    )
}

enum class LibrarySection { Created, Collected, Albums }

@Immutable
data class LibraryTabletUiState(
    val profile: LibraryProfileUi,
    val section: LibrarySection,
    val createdPlaylists: List<Playlist>,
    val collectedPlaylists: List<Playlist>,
    val albums: List<Album>,
) {
    val createdCount: Int get() = createdPlaylists.size
    val collectedCount: Int get() = collectedPlaylists.size
    val albumCount: Int get() = albums.size
}

sealed interface LibraryTabletEvent {
    data class SelectSection(val section: LibrarySection) : LibraryTabletEvent
    data class OpenPlaylist(val id: String) : LibraryTabletEvent
    data class OpenAlbum(val id: String) : LibraryTabletEvent
    data object ChangeProfilePhoto : LibraryTabletEvent
    data object OpenHistory : LibraryTabletEvent
    data object OpenLocalMusic : LibraryTabletEvent
    data object OpenDownloads : LibraryTabletEvent
}
