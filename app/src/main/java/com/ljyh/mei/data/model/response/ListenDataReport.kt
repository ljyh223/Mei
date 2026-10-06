package com.ljyh.mei.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListenDataRealtimeResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: ListenDataRealtime? = null,
)

@Serializable
data class ListenDataReportResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: ListenDataReport? = null,
)

@Serializable
data class ListenDataRealtime(
    val listenTimeDistributionBlock: ListenTimeDistributionBlock? = null,
    val weekTodayListenBlock: TodayListenBlock? = null,
)

@Serializable
data class ListenDataReport(
    val listenTimeBlock: ListenTimeBlock? = null,
    val listenTimeDistributionBlock: ListenTimeDistributionBlock? = null,
    val topStyleBlock: TopStyleBlock? = null,
    val topArtistBlock: RankedArtistBlock? = null,
    val topSongBlock: RankedSongBlock? = null,
)

@Serializable
data class ListenTimeBlock(
    val playDurationText: String? = null,
)

@Serializable
data class ListenTimeDistributionBlock(
    val playDuration: Int? = null,
    val listenDays: Int? = null,
    val durationDetails: List<ListenDurationDetail> = emptyList(),
    val achievementTitle: ListenAchievementTitle? = null,
)

@Serializable
data class ListenDurationDetail(
    val period: String? = null,
    val duration: Int? = null,
)

@Serializable
data class ListenAchievementTitle(
    val mainTitle: String? = null,
    val subTitle: String? = null,
)

@Serializable
data class TodayListenBlock(
    val songCount: Int? = null,
    val redCount: Int? = null,
    val coverUrls: List<String> = emptyList(),
)

@Serializable
data class TopStyleBlock(
    val genreName: String? = null,
)

@Serializable
data class RankedArtistBlock(
    val sections: List<RankedArtist> = emptyList(),
)

@Serializable
data class RankedArtist(
    val artistName: String? = null,
)

@Serializable
data class RankedSongBlock(
    val sections: List<RankedSong> = emptyList(),
)

@Serializable
data class RankedSong(
    val songName: String? = null,
)
