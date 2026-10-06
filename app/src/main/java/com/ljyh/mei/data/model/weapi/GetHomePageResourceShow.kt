package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.AndroidIdKey
import com.ljyh.mei.constants.AndroidUserAgent
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.netease.NeteaseUtils.getAndroidId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/* ==========================
 * 顶层请求参数
 * ========================== */
@Serializable
data class GetHomePageResourceShow(
    @SerialName("pageCode") val pageCode: String,
    @SerialName("isFirstScreen") val isFirstScreen: String,
    @SerialName("cursor") val cursor: String,
    @SerialName("refresh") val refresh: String,
    @SerialName("widthDp") val widthDp: String,
    @SerialName("heightDp") val heightDp: String,
    @SerialName("loadedPositionCodes") val loadedPositionCodes: String,
    @SerialName("clientCacheBlockCode") val clientCacheBlockCode: String,
    @SerialName("callbackParameters") val callbackParameters: String,
    @SerialName("extJson") val extJson: String,
    @SerialName("pageStyleType") val pageStyleType: String,
    @SerialName("adExtJson") val adExtJson: String,
    @SerialName("reqTimeStamp") val reqTimeStamp: String,
    @SerialName("clientTime") val clientTime: String,
    @SerialName("ruleJson") val ruleJson: String,
    @SerialName("algDemoteBlockCodeOrderList") val algDemoteBlockCodeOrderList: String,
    @SerialName("header") val header: String,
    @SerialName("e_r") val eR: Boolean
)

/* ==========================
 * AdExtJson 及其嵌套结构
 * ========================== */
@Serializable
data class AdExtJson(
    @SerialName("terminal") val terminal: String,
    @SerialName("network") val network: Int,
    @SerialName("op") val op: Int,
    @SerialName("dev_type") val devType: Int,
    @SerialName("pid") val pid: String,
    @SerialName("resolution") val resolution: Resolution,
    @SerialName("adReqId") val adReqId: String,
    @SerialName("memory") val memory: String,
    @SerialName("disk") val disk: String,
    @SerialName("mobilename") val mobilename: String,
    @SerialName("ipv4") val ipv4: String,
    @SerialName("ipv6") val ipv6: String,
    @SerialName("android_id") val androidId: String,
    @SerialName("manufacturer") val manufacturer: String,
    @SerialName("lbs") val lbs: String,
    @SerialName("opensdkVer") val opensdkVer: Int,
    @SerialName("wxApiVer") val wxApiVer: String,
    @SerialName("wxInstalled") val wxInstalled: Boolean,
    @SerialName("supportWechatCanvas") val supportWechatCanvas: Boolean,
    @SerialName("supportQuickApp") val supportQuickApp: Boolean,
    @SerialName("newAgent") val newAgent: String,
    @SerialName("useragent") val useragent: String,
    @SerialName("teenMode") val teenMode: Boolean,
    @SerialName("homePageType") val homePageType: Int,
    @SerialName("sourceFrame") val sourceFrame: String,
    @SerialName("subOsName") val subOsName: String,
    @SerialName("subOsVersion") val subOsVersion: String,
    @SerialName("bootMark") val bootMark: String,
    @SerialName("updateMark") val updateMark: String,
    @SerialName("vipRight") val vipRight: Boolean,
    @SerialName("isNativeSampling") val isNativeSampling: Boolean,
    @SerialName("ext") val ext: Ext
) {
    @Serializable
    data class Resolution(
        @SerialName("width") val width: Int,
        @SerialName("height") val height: Int
    )

    @Serializable

    data class Ext(
        @SerialName("bannerRefreshTypes") val bannerRefreshTypes: String,
        @SerialName("teenMode") val teenMode: Boolean,
        @SerialName("homePageType") val homePageType: Int,
        @SerialName("sourceFrame") val sourceFrame: String,
        @SerialName("opensdkVer") val opensdkVer: Int,
        @SerialName("wxApiVer") val wxApiVer: String,
        @SerialName("wxInstalled") val wxInstalled: Boolean,
        @SerialName("supportWechatCanvas") val supportWechatCanvas: Boolean,
        @SerialName("supportQuickApp") val supportQuickApp: Boolean,
        @SerialName("subOsName") val subOsName: String,
        @SerialName("subOsVersion") val subOsVersion: String,
        @SerialName("memory") val memory: String,
        @SerialName("disk") val disk: String,
        @SerialName("ipv4") val ipv4: String,
        @SerialName("ipv6") val ipv6: String,
        @SerialName("bootMark") val bootMark: String,
        @SerialName("updateMark") val updateMark: String
    )
}

/* =========================================================
 * 以下仅做“常量池”用，不对外暴露，避免调用方误用默认值
 * ========================================================= */
private object DefaultBuilder {

    const val PAGE_CODE = "HOME_RECOMMEND_PAGE"
    const val IS_FIRST_SCREEN = "true"
    const val CURSOR = "0"
    const val WIDTH_DP = "1080"
    const val HEIGHT_DP = "1920"
    const val LOADED_POSITION_CODES = ""
    var CLIENT_CACHE_BLOCK_CODE: String = Json.encodeToString(
        listOf(
            "PAGE_RECOMMEND_RADAR",
            "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
            "PAGE_RECOMMEND_RED_SIMILAR_SONG",
            "PAGE_RECOMMEND_DAILY_RECOMMEND"
        )
    )
    const val CALLBACK_PARAMETERS = ""
    const val EXT_JSON =
        """{"fmName":{"fmTitle":"漫游","fmLongTitle":"私人漫游"},"currentExploreHomeType":"main","homeFrameworkType":"fastPlay","latitude":"4.9E-324","homeReqSource":"home","adSceneExt":"","clientLibraAbTest":{"similarFmNameTest0422":"c","fmNameTest0422":"c"},"network":"wifi","carrier":"","forceFreshForNewUser":false,"bluetooth":false,"requestLongVideoBanner":true,"currentNewUserExploreHomeType":"main","longitude":"4.9E-324","refreshAction":"init","firstRequestPerLaunch":true,"clientMobileSize":"{\"width\":720,\"length\":1475}","noteHomeType":"note"}"""
    const val PAGE_STYLE_TYPE = "cutBlock"
    const val RULE_JSON = "{}"
    val ALG_DEMOTE_BLOCK_CODE_ORDER_LIST = Json.encodeToString(
        listOf(
            "PAGE_RECOMMEND_DAILY_RECOMMEND",
            "PAGE_RECOMMEND_VIP_SMALL_CARD",
            "PAGE_RECOMMEND_VIP_MODULE",
            "PAGE_RECOMMEND_BANNER_6",
            "PAGE_RECOMMEND_RADAR",
            "PAGE_RECOMMEND_BANNER_1",
            "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
            "PAGE_RECOMMEND_SHORTCUT",
            "PAGE_RECOMMEND_RED_SIMILAR_SONG",
            "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
            "PAGE_RECOMMEND_MIXED_ARTIST_PLAYLIST",
            "PAGE_RECOMMEND_SURVEY",
            "PAGE_RECOMMEND_PODCAST_MUSIC_RADIO",
            "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
            "PAGE_RECOMMEND_RANK",
            "PAGE_RECOMMEND_MUSIC_FM_LIST",
            "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM"
        )
    )
    const val HEADER = "{}"
    const val ER = true
}

/* =========================================================
 * 构造 DSL
 * ========================================================= */
inline fun buildGetHomePageResourceShow(
    refresh: String,
    block: GetHomePageResourceShowDsl.() -> Unit = {}
): GetHomePageResourceShow {
    val dsl = GetHomePageResourceShowDsl(refresh).apply(block)
    return GetHomePageResourceShow(
        pageCode = dsl.pageCode,
        isFirstScreen = dsl.isFirstScreen,
        cursor = dsl.cursor,
        refresh = dsl.refresh,
        widthDp = dsl.widthDp,
        heightDp = dsl.heightDp,
        loadedPositionCodes = dsl.loadedPositionCodes,
        clientCacheBlockCode = dsl.clientCacheBlockCode,
        callbackParameters = dsl.callbackParameters,
        extJson = dsl.extJson,
        pageStyleType = dsl.pageStyleType,
        adExtJson = dsl.adExtJson,
        reqTimeStamp = dsl.reqTimeStamp,
        clientTime = dsl.clientTime,
        ruleJson = dsl.ruleJson,
        algDemoteBlockCodeOrderList = dsl.algDemoteBlockCodeOrderList,
        header = dsl.header,
        eR = dsl.eR
    )
}

class GetHomePageResourceShowDsl(
    val refresh: String
) {
    var pageCode: String = DefaultBuilder.PAGE_CODE
    var isFirstScreen: String = DefaultBuilder.IS_FIRST_SCREEN
    var cursor: String = DefaultBuilder.CURSOR
    var widthDp: String = DefaultBuilder.WIDTH_DP
    var heightDp: String = DefaultBuilder.HEIGHT_DP
    var loadedPositionCodes: String = DefaultBuilder.LOADED_POSITION_CODES
    var clientCacheBlockCode: String = DefaultBuilder.CLIENT_CACHE_BLOCK_CODE
    var callbackParameters: String = DefaultBuilder.CALLBACK_PARAMETERS
    var extJson: String = DefaultBuilder.EXT_JSON
    var pageStyleType: String = DefaultBuilder.PAGE_STYLE_TYPE
    var adExtJson: String = Json.encodeToString(buildAdExtJson { })
    var reqTimeStamp: String = System.currentTimeMillis().toString()
    var clientTime: String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    var ruleJson: String = DefaultBuilder.RULE_JSON
    var algDemoteBlockCodeOrderList: String = DefaultBuilder.ALG_DEMOTE_BLOCK_CODE_ORDER_LIST
    var header: String = DefaultBuilder.HEADER
    var eR: Boolean = DefaultBuilder.ER
}

/* ---------------- AdExtJson 构造器 ---------------- */
inline fun buildAdExtJson(block: AdExtJsonDsl.() -> Unit = {}): AdExtJson {
    val dsl = AdExtJsonDsl().apply(block)
    return AdExtJson(
        terminal = dsl.terminal,
        network = dsl.network,
        op = dsl.op,
        devType = dsl.devType,
        pid = dsl.pid,
        resolution = AdExtJson.Resolution(dsl.resolutionWidth, dsl.resolutionHeight),
        adReqId = dsl.adReqId,
        memory = dsl.memory,
        disk = dsl.disk,
        mobilename = dsl.mobilename,
        ipv4 = dsl.ipv4,
        ipv6 = dsl.ipv6,
        androidId = dsl.androidId,
        manufacturer = dsl.manufacturer,
        lbs = dsl.lbs,
        opensdkVer = dsl.opensdkVer,
        wxApiVer = dsl.wxApiVer,
        wxInstalled = dsl.wxInstalled,
        supportWechatCanvas = dsl.supportWechatCanvas,
        supportQuickApp = dsl.supportQuickApp,
        newAgent = dsl.newAgent,
        useragent = dsl.useragent,
        teenMode = dsl.teenMode,
        homePageType = dsl.homePageType,
        sourceFrame = dsl.sourceFrame,
        subOsName = dsl.subOsName,
        subOsVersion = dsl.subOsVersion,
        bootMark = dsl.bootMark,
        updateMark = dsl.updateMark,
        vipRight = dsl.vipRight,
        isNativeSampling = dsl.isNativeSampling,
        ext = AdExtJson.Ext(
            bannerRefreshTypes = dsl.extBannerRefreshTypes,
            teenMode = dsl.extTeenMode,
            homePageType = dsl.extHomePageType,
            sourceFrame = dsl.extSourceFrame,
            opensdkVer = dsl.extOpensdkVer,
            wxApiVer = dsl.extWxApiVer,
            wxInstalled = dsl.extWxInstalled,
            supportWechatCanvas = dsl.extSupportWechatCanvas,
            supportQuickApp = dsl.extSupportQuickApp,
            subOsName = dsl.extSubOsName,
            subOsVersion = dsl.extSubOsVersion,
            memory = dsl.extMemory,
            disk = dsl.extDisk,
            ipv4 = dsl.extIpv4,
            ipv6 = dsl.extIpv6,
            bootMark = dsl.extBootMark,
            updateMark = dsl.extUpdateMark
        )
    )
}

class AdExtJsonDsl {
    var terminal: String = "Mi+A3"
    var network: Int = 1
    var op: Int = 0
    var devType: Int = 1
    var pid: String = "4002"
    var resolutionWidth: Int = 720
    var resolutionHeight: Int = 1475
    var adReqId: String = "5128948380_1766489254611_6600"
    var memory: String = "5885825024"
    var disk: String = "52297560064"
    var mobilename: String = "Mi A3"
    var ipv4: String = "127.0.0.2"
    var ipv6: String = ""
    var androidId: String = AppContext.instance.dataStore[AndroidIdKey]?:getAndroidId()
    var manufacturer: String = "Xiaomi"
    var lbs: String = """{"latitude":"4.9E-324","longitude":"4.9E-324"}"""
    var opensdkVer: Int = 638065664
    var wxApiVer: String = "0"
    var wxInstalled: Boolean = false
    var supportWechatCanvas: Boolean = true
    var supportQuickApp: Boolean = true
    var newAgent: String = AndroidUserAgent
    var useragent: String = AndroidUserAgent
    var teenMode: Boolean = false
    var homePageType: Int = 1
    var sourceFrame: String = "note"
    var subOsName: String = ""
    var subOsVersion: String = ""
    var bootMark: String = "d99bbae5-25f8-4e1c-bfbd-f6e44d057d07"
    var updateMark: String = "183182981.779999997"
    var vipRight: Boolean = true
    var isNativeSampling: Boolean = false

    /* ext 映射 */
    var extBannerRefreshTypes: String = "[0]"
    var extTeenMode: Boolean = teenMode
    var extHomePageType: Int = homePageType
    var extSourceFrame: String = sourceFrame
    var extOpensdkVer: Int = opensdkVer
    var extWxApiVer: String = wxApiVer
    var extWxInstalled: Boolean = wxInstalled
    var extSupportWechatCanvas: Boolean = supportWechatCanvas
    var extSupportQuickApp: Boolean = supportQuickApp
    var extSubOsName: String = subOsName
    var extSubOsVersion: String = subOsVersion
    var extMemory: String = memory
    var extDisk: String = disk
    var extIpv4: String = ipv4
    var extIpv6: String = ipv6
    var extBootMark: String = bootMark
    var extUpdateMark: String = updateMark
}
