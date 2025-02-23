package com.ljyh.music.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface QQMusicApiService {
    @GET("/qqmusic/fcgi-bin/lyric_download.fcg")
    suspend fun getQQMusicLyric(
        @Query("musicid") musicId: String,
        @Query("version") version: String = "15",
        @Query("lrctype") lrcType: String = "4",
        @Query("miniversion") miniVersion: String = "82",
    ): String


//    'https://c.y.qq.com/lyric/fcgi-bin/fcg_search_pc_lrc.fcg?SONGNAME=Surges&SINGERNAME=orangestar&TYPE=2&RANGE_MIN=1&RANGE_MAX=20'
    @GET("/lyric/fcgi-bin/fcg_search_pc_lrc.fcg")
    suspend fun searchLyric(
        @Query("SONGNAME") songName: String,
        @Query("SINGERNAME") singerName: String,
        @Query("TYPE") type: String = "2",
        @Query("RANGE_MIN") rangeMin: Int = 1,
        @Query("RANGE_MAX") rangeMax: Int = 20
    ):String
}