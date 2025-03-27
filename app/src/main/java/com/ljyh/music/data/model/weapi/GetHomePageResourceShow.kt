package com.ljyh.music.data.model.weapi

import com.google.gson.annotations.SerializedName
import com.ljyh.music.utils.TimeUtils.getCurrentTimestamp
import com.ljyh.music.utils.TimeUtils.getFormattedDate


data class GetHomePageResourceShow(
    @SerializedName("adExtJson")
    val adExtJson: AdExtJson = AdExtJson(),
    @SerializedName("callbackParameters")
    val callbackParameters: String = "",
    @SerializedName("clientCacheBlockCode")
    var clientCacheBlockCode: List<String> = emptyList(),
    @SerializedName("blockCodeOrderList")
    var blockCodeOrderList: List<String> = listOf(
        "PAGE_RECOMMEND_RANK",//排行榜
        "PAGE_RECOMMEND_MUSIC_FM_LIST",
        "PAGE_RECOMMEND_RADAR",// 雷达歌单
        "PAGE_RECOMMEND_ARTIST_FM_LIST",
        "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_DAILY_RECOMMEND", // 每日推荐
        "PAGE_RECOMMEND_STYLE_PLAYLIST_1",
        "PAGE_RECOMMEND_LBS",
        "PAGE_RECOMMEND_MY_SHEET",
        "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",// 推荐歌单
        "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
        "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
        "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",// 私人推荐
        "PAGE_RECOMMEND_SPECIAL_ORIGIN_SONG_LOCATION",
        "PAGE_RECOMMEND_VIP_CARD",
        "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_COMBINATION"
    ),
    @SerializedName("clientTime")
    val clientTime: String = getFormattedDate(),
    @SerializedName("cursor")
    var cursor: Int = 0,
    @SerializedName("e_r")
    val eR: Boolean = true,
    @SerializedName("extJson")
    val extJson: ExtJson = ExtJson(),
    @SerializedName("header")
    val header: String = "{}",
    @SerializedName("heightDp")
    val heightDp: String = "880.0",
    @SerializedName("isFirstScreen")
    var isFirstScreen: Boolean = true,
    @SerializedName("pageCode")
    val pageCode: String = "HOME_RECOMMEND_PAGE",
    @SerializedName("refresh")
    val refresh: String = "true",
    @SerializedName("reqTimeStamp")
    val reqTimeStamp: String = getCurrentTimestamp(),
    @SerializedName("widthDp")
    val widthDp: String = "411.42856"
) {
    private val BlockCodeOrderList = listOf(
        listOf(
            "PAGE_RECOMMEND_RANK",//排行榜
            "PAGE_RECOMMEND_MUSIC_FM_LIST",
            "PAGE_RECOMMEND_RADAR",// 雷达歌单
            "PAGE_RECOMMEND_ARTIST_FM_LIST",
            "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_DAILY_RECOMMEND", // 每日推荐
            "PAGE_RECOMMEND_STYLE_PLAYLIST_1",
            "PAGE_RECOMMEND_LBS",
            "PAGE_RECOMMEND_MY_SHEET",
            "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
            "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
            "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
            "PAGE_RECOMMEND_SPECIAL_ORIGIN_SONG_LOCATION",
            "PAGE_RECOMMEND_VIP_CARD",
            "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_COMBINATION"
        ),
        listOf(

            "PAGE_RECOMMEND_MIXED_ARTIST_PLAYLIST",
            "PAGE_RECOMMEND_RED_SIMILAR_SONG",
            "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
            "PAGE_RECOMMEND_LBS",
            "PAGE_RECOMMEND_RANK",
            "PAGE_RECOMMEND_SURVEY",
            "PAGE_RECOMMEND_MY_SHEET",
            "PAGE_RECOMMEND_FIRM_PLAYLIST",
            "PAGE_RECOMMEND_COMBINATION",
            "PAGE_RECOMMEND_STYLE_PLAYLIST_1",
            "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_MUSIC_FM_LIST",
            "PAGE_RECOMMEND_STYLE_PLAYLIST_2",
            "PAGE_RECOMMEND_REAL_TIME_INTEREST_RCMD",
            "PAGE_RECOMMEND_ARTIST_FM_LIST",
            "PAGE_RECOMMEND_PODCAST_ALBUM_COVER",
            "PAGE_RECOMMEND_PODCAST_MUSIC_RADIO",//音乐播客
            "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST",
            "PAGE_RECOMMEND_ARTIST_TREND",
            "PAGE_RECOMMEND_VIP_CARD",
            "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
            "PAGE_RECOMMEND_PODCAST_AUDIO_BOOK",
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
            "PAGE_RECOMMEND_BROADCAST",
            "PAGE_RECOMMEND_SPECIAL_ORIGIN_SONG_LOCATION"
        ),
        listOf(

            "PAGE_RECOMMEND_FIRM_PLAYLIST",
            "PAGE_RECOMMEND_COMBINATION",
            "PAGE_RECOMMEND_STYLE_PLAYLIST_1",
            "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_MUSIC_FM_LIST",
            "PAGE_RECOMMEND_STYLE_PLAYLIST_2",
            "PAGE_RECOMMEND_REAL_TIME_INTEREST_RCMD",
            "PAGE_RECOMMEND_ARTIST_FM_LIST",
            "PAGE_RECOMMEND_PODCAST_ALBUM_COVER",
            "PAGE_RECOMMEND_PODCAST_MUSIC_RADIO",
            "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST",
            "PAGE_RECOMMEND_ARTIST_TREND",
            "PAGE_RECOMMEND_VIP_CARD",
            "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
            "PAGE_RECOMMEND_PODCAST_AUDIO_BOOK",
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
            "PAGE_RECOMMEND_BROADCAST",
            "PAGE_RECOMMEND_SPECIAL_ORIGIN_SONG_LOCATION"
        ),
        listOf(
            "PAGE_RECOMMEND_ARTIST_FM_LIST",
            "PAGE_RECOMMEND_PODCAST_ALBUM_COVER",
            "PAGE_RECOMMEND_PODCAST_MUSIC_RADIO",
            "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST",
            "PAGE_RECOMMEND_ARTIST_TREND",
            "PAGE_RECOMMEND_VIP_CARD",
            "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
            "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
            "PAGE_RECOMMEND_PODCAST_AUDIO_BOOK",
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
            "PAGE_RECOMMEND_BROADCAST",
            "PAGE_RECOMMEND_SPECIAL_ORIGIN_SONG_LOCATION"
        )

    )

    init {
        if (cursor > 0) {
            isFirstScreen = false
            extJson.refreshAction = "pull"
            extJson.firstRequestPerLaunch = "false"
            clientCacheBlockCode = BlockCodeOrderList.take(cursor).flatten()
        }
        blockCodeOrderList = BlockCodeOrderList[cursor]


        when (cursor) {
            0 -> cursor = 0
            1 -> cursor = 8
            2 -> cursor = 15
            3 -> cursor = 22
            else -> cursor = 0
        }

    }


    data class ExtJson(
        @SerializedName("carrier")
        val carrier: String = "telecom",
        @SerializedName("currentExploreHomeType")
        val currentExploreHomeType: String = "main",
        @SerializedName("currentNewUserExploreHomeType")
        val currentNewUserExploreHomeType: String = "main",
        @SerializedName("firstRequestPerLaunch")
        var firstRequestPerLaunch: String = "false",
        @SerializedName("forceFreshForNewUser")
        val forceFreshForNewUser: Boolean = false,
        @SerializedName("refreshAction")
        var refreshAction: String = "init",
        @SerializedName("requestLongVideoBanner")
        val requestLongVideoBanner: Boolean = true,
        @SerializedName("clientMobileSize")
        val clientMobileSize: ClientMobileSize = ClientMobileSize()
    ) {
        data class ClientMobileSize(
            @SerializedName("height")
            val height: String = "2310",
            @SerializedName("width")
            val width: String = "1080"
        )
    }


    data class AdExtJson(
        @SerializedName("terminal")
        val terminal: String = "23013RK75C",
        @SerializedName("network")
        val network: Int = 1,
        @SerializedName("op")
        val op: Int = 1,
        @SerializedName("dev_type")
        val devType: Int = 1,
        @SerializedName("pid")
        val pid: String = "4002",
        @SerializedName("resolution")
        val resolution: Resolution = Resolution(),
        @SerializedName("adReqId")
        val adReqId: String = "5128948380_1737953870506_1042",
        @SerializedName("mobilename")
        val mobilename: String = "23013RK75C",
        @SerializedName("ipv4")
        val ipv4: String = "",
        @SerializedName("ipv6")
        val ipv6: String = "",
        @SerializedName("imei")
        val imei: String = "null",
        @SerializedName("android_id")
        val androidId: String = "bnVsbAkwMjowMDowMDowMDowMDowMAkzM2FmOTJkZjZmOTk2ZGZjCWNlZTZhODZhNzExN2UyODQ%3D",
        @SerializedName("manufacturer")
        val manufacturer: String = "Redmi",
        @SerializedName("lbs")
        val lbs: String = "",
        @SerializedName("opensdkVer")
        val opensdkVer: Int = 0,
        @SerializedName("oaid")
        val oaid: String = "ba6fe82aac1e63e0",
        @SerializedName("newAgent")
        val newAgent: String = "Mozilla/5.0 (Linux; Android 15; 23013RK75C Build/AQ3A.240912.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.107 Mobile Safari/537.36 NeteaseMusic/6.6.66.240906144801",
        @SerializedName("useragent")
        val useragent: String = "Mozilla/5.0 (Linux; Android 15; 23013RK75C Build/AQ3A.240912.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.107 Mobile Safari/537.36 NeteaseMusic/6.6.66.240906144801",
        @SerializedName("teenMode")
        val teenMode: Boolean = false,
        @SerializedName("bootMark")
        val bootMark: String = "ed44e45d-aef4-4d7d-8ff1-9bd235e3b364",
        @SerializedName("updateMark")
        val updateMark: String = "1737953868.800748929",
        @SerializedName("vipRight")
        val vipRight: Boolean = true,
        @SerializedName("associatedAppVer")
        val associatedAppVer: String = "9.0.40",
        @SerializedName("isNativeSampling")
        val isNativeSampling: Boolean = false,
        @SerializedName("ext")
        val ext: Ext = Ext()
    ) {
        data class Resolution(
            @SerializedName("width")
            val width: Int = 1080,
            @SerializedName("height")
            val height: Int = 2310
        )

        data class Lbs(
            @SerializedName("lat")
            val latitude: String = "4.9E-324",
            @SerializedName("lng")
            val longitude: String = "4.9E-324"
        )

        data class Ext(
            @SerializedName("associatedAppVer")
            val associatedAppVer: String = "9.0.40",
            @SerializedName("bannerRefreshTypes")
            val bannerRefreshTypes: String = "[0]",
            @SerializedName("teenMode")
            val teenMode: Boolean = false,
            @SerializedName("opensdkVer")
            val opensdkVer: Int = 638058496,
            @SerializedName("oaid")
            val oaid: String = "ba6fe82aac1e63e0",
            @SerializedName("ipv4")
            val ipv4: String = "",
            @SerializedName("ipv6")
            val ipv6: String = "",
            @SerializedName("bootMark")
            val bootMark: String = "ed44e45d-aef4-4d7d-8ff1-9bd235e3b364",
            @SerializedName("updateMark")
            val updateMark: String = "1737953868.800748929"
        )
    }
}



