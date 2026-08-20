package com.ljyh.mei.data.model

data class ListenDataRealtimeResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: ListenDataRealtime? = null,
)

data class ListenDataReportResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: ListenDataReport? = null,
)

data class ListenDataRealtime(
    val type: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val listenTimeDistributionBlock: ListenTimeDistributionBlock? = null,
    val weekTodayListenBlock: TodayListenBlock? = null,
)

data class ListenDataReport(
    val type: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val listenTimeBlock: ListenTimeBlock? = null,
    val listenTimeDistributionBlock: ListenTimeDistributionBlock? = null,
    val topStyleBlock: TopStyleBlock? = null,
    val topArtistBlock: RankedArtistBlock? = null,
    val topSongBlock: RankedSongBlock? = null,
)

data class ListenTimeBlock(
    val playDuration: Int? = null,
    val playDurationText: String? = null,
    val sections: List<ListenTimeSection> = emptyList(),
    val circleTimePeriodDurations: List<TimePeriodDuration> = emptyList(),
)

data class ListenTimeDistributionBlock(
    val playDuration: Int? = null,
    val listenDays: Int? = null,
    val durationDetails: List<ListenDurationDetail> = emptyList(),
    val achievementTitle: ListenAchievementTitle? = null,
)

data class ListenDurationDetail(
    val period: String? = null,
    val duration: Int? = null,
    val podcastDuration: Int? = null,
    val audiobookDuration: Int? = null,
    val reachLimit: Boolean? = null,
)

data class ListenAchievementTitle(
    val mainTitle: String? = null,
    val subTitle: String? = null,
)

data class TodayListenBlock(
    val songCount: Int? = null,
    val redCount: Int? = null,
    val coverUrls: List<String> = emptyList(),
)

data class ListenTimeSection(
    val type: String? = null,
    val field: String? = null,
    val valueA: String? = null,
    val textB: String? = null,
)

data class TimePeriodDuration(
    val period: String? = null,
    val duration: Int? = null,
)

data class TopStyleBlock(
    val genreId: Long? = null,
    val genreName: String? = null,
    val genreEnglishName: String? = null,
    val picUrl: String? = null,
    val sections: List<TopStyleSection> = emptyList(),
)

data class TopStyleSection(
    val genreId: Long? = null,
    val genreName: String? = null,
    val percent: String? = null,
)

data class RankedArtistBlock(
    val sections: List<RankedArtist> = emptyList(),
)

data class RankedArtist(
    val artistId: Long? = null,
    val artistName: String? = null,
    val picUrl: String? = null,
    val text: String? = null,
)

data class RankedSongBlock(
    val sections: List<RankedSong> = emptyList(),
)

data class RankedSong(
    val songId: Long? = null,
    val songName: String? = null,
    val picUrl: String? = null,
    val text: String? = null,
)
