package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.ListenAchievementTitle
import com.ljyh.mei.data.model.ListenDataRealtime
import com.ljyh.mei.data.model.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.ListenDataReport
import com.ljyh.mei.data.model.ListenDataReportResponse
import com.ljyh.mei.data.model.ListenDurationDetail
import com.ljyh.mei.data.model.ListenTimeDistributionBlock
import com.ljyh.mei.data.model.RankedArtist
import com.ljyh.mei.data.model.RankedArtistBlock
import com.ljyh.mei.data.model.TodayListenBlock
import com.ljyh.mei.data.model.TopStyleBlock
import com.ljyh.mei.data.network.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListeningFootprintMapperTest {
    @Test
    fun realtimeReportsMapToIndependentWeekAndMonthPeriods() {
        val footprint = resolveListeningFootprint(
            weekRealtime = successRealtime(
                minutes = 874,
                days = 5,
                dailyMinutes = listOf(153, 142, 31, 360, 188),
                todaySongs = 47,
            ),
            monthRealtime = successRealtime(
                minutes = 3_154,
                days = 17,
                dailyMinutes = listOf(246, 251, 1_024),
            ),
            weekReport = Resource.Loading,
        )

        assertEquals(874, footprint.week?.totalMinutes)
        assertEquals(47, footprint.week?.todaySongCount)
        assertEquals(listOf(153, 142, 31, 360, 188), footprint.week?.dailyMinutes)
        assertEquals(3_154, footprint.month?.totalMinutes)
        assertEquals(17, footprint.month?.activeDays)
    }

    @Test
    fun reportInsightUsesOnlySupportedServerFields() {
        val report = ListenDataReportResponse(
            code = 200,
            data = ListenDataReport(
                listenTimeDistributionBlock = ListenTimeDistributionBlock(
                    achievementTitle = ListenAchievementTitle(
                        mainTitle = "云村夜猫子",
                        subTitle = "54%时间晚上听歌!",
                    ),
                ),
                topStyleBlock = TopStyleBlock(genreName = "二次元"),
                topArtistBlock = RankedArtistBlock(
                    sections = listOf(RankedArtist(artistName = "ヨルシカ")),
                ),
            ),
        )

        val footprint = resolveListeningFootprint(
            weekRealtime = Resource.Loading,
            monthRealtime = Resource.Loading,
            weekReport = Resource.Success(report),
        )

        assertEquals("云村夜猫子", footprint.weekInsight?.title)
        assertEquals("二次元", footprint.weekInsight?.topStyle)
        assertEquals("ヨルシカ", footprint.weekInsight?.topArtist)
    }

    @Test
    fun failedRealtimeReportDoesNotInventAListeningPeriod() {
        val footprint = resolveListeningFootprint(
            weekRealtime = Resource.Error("offline"),
            monthRealtime = Resource.Loading,
            weekReport = Resource.Loading,
        )

        assertNull(footprint.week)
        assertNull(footprint.month)
    }

    private fun successRealtime(
        minutes: Int,
        days: Int,
        dailyMinutes: List<Int>,
        todaySongs: Int? = null,
    ): Resource<ListenDataRealtimeResponse> = Resource.Success(
        ListenDataRealtimeResponse(
            code = 200,
            data = ListenDataRealtime(
                listenTimeDistributionBlock = ListenTimeDistributionBlock(
                    playDuration = minutes,
                    listenDays = days,
                    durationDetails = dailyMinutes.mapIndexed { index, duration ->
                        ListenDurationDetail(period = index.toString(), duration = duration)
                    },
                ),
                weekTodayListenBlock = todaySongs?.let { TodayListenBlock(songCount = it) },
            ),
        ),
    )
}
