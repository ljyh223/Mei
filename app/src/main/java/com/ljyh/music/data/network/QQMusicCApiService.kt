package com.ljyh.music.data.network

import com.ljyh.music.data.model.qq.c.Search
import retrofit2.http.GET
import retrofit2.http.Query

interface QQMusicCApiService {
    // 这个api返回的body 外层包了一层注释
    // 响应头的Content-Type不可信
    @GET("/qqmusic/fcgi-bin/lyric_download.fcg")
    suspend fun getQQMusicLyric(
        @Query("musicid") musicId: String,
        @Query("version") version: String = "15",
        @Query("lrctype") lrcType: String = "4",
        @Query("miniversion") miniVersion: String = "82",
    ): String


    //    'https://c.y.qq.com/lyric/fcgi-bin/fcg_search_pc_lrc.fcg?SONGNAME=Surges&SINGERNAME=orangestar&TYPE=2&RANGE_MIN=1&RANGE_MAX=20'
    // 这个api就是xml可以直接解析
    @GET("/lyric/fcgi-bin/fcg_search_pc_lrc.fcg")
    suspend fun searchLyric(
        @Query("SONGNAME") songName: String,
        @Query("SINGERNAME") singerName: String,
        @Query("TYPE") type: String = "2",
        @Query("RANGE_MIN") rangeMin: Int = 1,
        @Query("RANGE_MAX") rangeMax: Int = 20
    ): String

    // 哎哟喂，我真是艾草了，妈的三个接口，三种不同的格式
    //这个用的json，
    @GET("/splcloud/fcgi-bin/smartbox_new.fcg")
    suspend fun search(@Query("key") key: String, @Query("g_tk") tk: Int = 5381): Search
}