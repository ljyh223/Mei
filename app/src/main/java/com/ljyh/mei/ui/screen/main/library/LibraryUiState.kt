package com.ljyh.mei.ui.screen.main.library

import androidx.compose.runtime.Immutable
import com.ljyh.mei.data.model.UserAccount
import com.ljyh.mei.data.model.UserAlbumList
import com.ljyh.mei.data.model.UserVipInfo
import com.ljyh.mei.data.model.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.ListenDataReportResponse
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.model.toAlbum

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

enum class ListeningPeriod { Week, Month }

@Immutable
data class ListeningPeriodUi(
    val period: ListeningPeriod,
    val totalMinutes: Int,
    val activeDays: Int,
    val dailyMinutes: List<Int>,
    val todaySongCount: Int? = null,
    val todayRedCount: Int? = null,
)

@Immutable
data class ListeningInsightUi(
    val title: String? = null,
    val subtitle: String? = null,
    val topStyle: String? = null,
    val topArtist: String? = null,
    val topSong: String? = null,
    val comparisonText: String? = null,
)

@Immutable
data class ListeningFootprintUi(
    val week: ListeningPeriodUi? = null,
    val month: ListeningPeriodUi? = null,
    val weekInsight: ListeningInsightUi? = null,
) {
    val hasContent: Boolean get() = week != null || month != null
}

@Immutable
data class LibraryContentUiState(
    val profile: LibraryProfileUi,
    val section: LibrarySection,
    val createdPlaylists: List<Playlist>,
    val collectedPlaylists: List<Playlist>,
    val albums: List<Album>,
    val listeningFootprint: ListeningFootprintUi = ListeningFootprintUi(),
) {
    val createdCount: Int get() = createdPlaylists.size
    val collectedCount: Int get() = collectedPlaylists.size
    val albumCount: Int get() = albums.size
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(
        val data: LibraryContentUiState,
        val warning: String? = null,
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

sealed interface LibraryEvent {
    data class SelectSection(val section: LibrarySection) : LibraryEvent
    data class OpenPlaylist(val id: String) : LibraryEvent
    data class OpenAlbum(val id: String) : LibraryEvent
    data object ChangeProfilePhoto : LibraryEvent
    data object OpenHistory : LibraryEvent
    data object OpenLocalMusic : LibraryEvent
    data object OpenDownloads : LibraryEvent
}

internal fun buildLibraryUiState(
    profile: LibraryProfileUi,
    section: LibrarySection,
    playlists: List<Playlist>,
    albums: List<Album>,
    now: Long,
    listeningFootprint: ListeningFootprintUi = ListeningFootprintUi(),
): LibraryContentUiState {
    val (created, collected) = playlists.partition { it.author == profile.userId }
    return LibraryContentUiState(
        profile = profile,
        section = section,
        createdPlaylists = created.sortedForLibrary(now),
        collectedPlaylists = collected.sortedForLibrary(now),
        albums = albums,
        listeningFootprint = listeningFootprint,
    )
}

internal fun resolveLibraryUiState(
    accountResource: Resource<UserAccount>,
    profile: LibraryProfileUi?,
    playlists: List<Playlist>,
    albumResource: Resource<UserAlbumList>,
    section: LibrarySection,
    now: Long,
    listeningFootprint: ListeningFootprintUi = ListeningFootprintUi(),
): LibraryUiState {
    if (profile == null) {
        return when (accountResource) {
            is Resource.Error -> LibraryUiState.Error(accountResource.message)
            is Resource.Success -> LibraryUiState.Error("账户信息不可用")
            Resource.Loading -> LibraryUiState.Loading
        }
    }
    val albums = (albumResource as? Resource.Success)
        ?.data
        ?.data
        ?.map { it.toAlbum() }
        .orEmpty()
    val warning = when {
        accountResource is Resource.Error -> accountResource.message
        albumResource is Resource.Error -> albumResource.message
        else -> null
    }
    return LibraryUiState.Content(
        data = buildLibraryUiState(
            profile = profile,
            section = section,
            playlists = playlists,
            albums = albums,
            now = now,
            listeningFootprint = listeningFootprint,
        ),
        warning = warning,
    )
}

internal fun resolveListeningFootprint(
    weekRealtime: Resource<ListenDataRealtimeResponse>,
    monthRealtime: Resource<ListenDataRealtimeResponse>,
    weekReport: Resource<ListenDataReportResponse>,
): ListeningFootprintUi = ListeningFootprintUi(
    week = (weekRealtime as? Resource.Success)
        ?.data
        ?.takeIf { it.code == 200 }
        ?.toListeningPeriodUi(ListeningPeriod.Week),
    month = (monthRealtime as? Resource.Success)
        ?.data
        ?.takeIf { it.code == 200 }
        ?.toListeningPeriodUi(ListeningPeriod.Month),
    weekInsight = (weekReport as? Resource.Success)
        ?.data
        ?.takeIf { it.code == 200 }
        ?.toListeningInsightUi(),
)

private fun ListenDataRealtimeResponse.toListeningPeriodUi(
    period: ListeningPeriod,
): ListeningPeriodUi? {
    val report = data ?: return null
    val distribution = report.listenTimeDistributionBlock ?: return null
    val totalMinutes = distribution.playDuration ?: return null
    return ListeningPeriodUi(
        period = period,
        totalMinutes = totalMinutes.coerceAtLeast(0),
        activeDays = distribution.listenDays?.coerceAtLeast(0) ?: 0,
        dailyMinutes = distribution.durationDetails.mapNotNull { it.duration?.coerceAtLeast(0) },
        todaySongCount = report.weekTodayListenBlock?.songCount?.coerceAtLeast(0),
        todayRedCount = report.weekTodayListenBlock?.redCount?.coerceAtLeast(0),
    )
}

private fun ListenDataReportResponse.toListeningInsightUi(): ListeningInsightUi? {
    val report = data ?: return null
    val insight = ListeningInsightUi(
        title = report.listenTimeDistributionBlock?.achievementTitle?.mainTitle,
        subtitle = report.listenTimeDistributionBlock?.achievementTitle?.subTitle,
        topStyle = report.topStyleBlock?.genreName,
        topArtist = report.topArtistBlock?.sections?.firstOrNull()?.artistName,
        topSong = report.topSongBlock?.sections?.firstOrNull()?.songName,
        comparisonText = report.listenTimeBlock?.playDurationText,
    )
    return insight.takeIf {
        listOf(it.title, it.subtitle, it.topStyle, it.topArtist, it.topSong, it.comparisonText)
            .any { value -> !value.isNullOrBlank() }
    }
}

internal fun List<Playlist>.sortedForLibrary(now: Long): List<Playlist> {
    val maxLocalPlayCount = maxOfOrNull { it.localPlayCount } ?: 0
    val maxPlayCount = maxOfOrNull { it.playCount } ?: 0L
    return sortedByDescending {
        it.sortScore(maxLocalPlayCount, maxPlayCount, now)
    }
}
