package com.ljyh.unblockneteasemusic.qq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchRequest(
    val comm: SearchComm = SearchComm(),
    @SerialName("request") val request: SearchCall,
)

@Serializable
internal data class SearchComm(
    val ct: Int = 11,
    val cv: String = "1003006",
    val v: String = "1003006",
    @SerialName("os_ver") val osVersion: String = "15",
    val phonetype: String = "24122RKC7C",
    val tmeAppID: String = "qqmusiclight",
    val nettype: String = "NETWORK_WIFI",
    val udid: String = "0",
)

@Serializable
internal data class SearchCall(
    val method: String = "DoSearchForQQMusicLite",
    val module: String = "music.search.SearchCgiService",
    val param: SearchParams,
)

@Serializable
internal data class SearchParams(
    val query: String,
    @SerialName("search_type") val searchType: Int = 0,
    @SerialName("page_num") val pageNumber: Int = 1,
    @SerialName("num_per_page") val pageSize: Int = 20,
    val highlight: Int = 0,
    @SerialName("nqc_flag") val nqcFlag: Int = 0,
    @SerialName("page_id") val pageId: Int = 1,
    val grp: Int = 1,
)

@Serializable
internal data class SearchResponse(val request: SearchPayload? = null)
@Serializable
internal data class SearchPayload(val data: SearchData? = null)
@Serializable
internal data class SearchData(val body: SearchBody? = null)
@Serializable
internal data class SearchBody(@SerialName("item_song") val songs: List<SongDto> = emptyList())

@Serializable
internal data class DesktopSearchRequest(val search: DesktopSearchCall)
@Serializable
internal data class DesktopSearchCall(
    val method: String = "DoSearchForQQMusicDesktop",
    val module: String = "music.search.SearchCgiService",
    val param: DesktopSearchParams,
)
@Serializable
internal data class DesktopSearchParams(
    @SerialName("num_per_page") val pageSize: Int,
    @SerialName("page_num") val pageNumber: Int = 1,
    val query: String,
    @SerialName("search_type") val searchType: Int = 0,
)
@Serializable
internal data class DesktopSearchResponse(val search: DesktopSearchPayload? = null)
@Serializable
internal data class DesktopSearchPayload(val data: DesktopSearchData? = null)
@Serializable
internal data class DesktopSearchData(val body: DesktopSearchBody? = null)
@Serializable
internal data class DesktopSearchBody(val song: DesktopSearchSongs? = null)
@Serializable
internal data class DesktopSearchSongs(val list: List<SongDto> = emptyList())
@Serializable
internal data class SongDto(
    val id: Long,
    val mid: String = "",
    val name: String = "",
    val title: String = "",
    val interval: Long = 0,
    val singer: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
)

@Serializable
internal data class VkeyRequest(
    @SerialName("req_0") val call: VkeyCall,
)
@Serializable
internal data class VkeyCall(
    val module: String = "vkey.GetVkeyServer",
    val method: String = "CgiGetVkey",
    val param: VkeyParams,
)
@Serializable
internal data class VkeyParams(
    val guid: String,
    val loginflag: Int = 1,
    val filename: List<String>? = null,
    val songmid: List<String>,
    val songtype: List<Int> = listOf(0),
    val uin: String = "0",
    val platform: String = "20",
)
@Serializable
internal data class VkeyResponse(@SerialName("req_0") val payload: VkeyPayload? = null)
@Serializable
internal data class VkeyPayload(val data: VkeyData? = null)
@Serializable
internal data class VkeyData(
    val sip: List<String> = emptyList(),
    val midurlinfo: List<VkeyUrl> = emptyList(),
)
@Serializable
internal data class VkeyUrl(val purl: String = "")
@Serializable
internal data class ArtistDto(val id: Long = 0, val name: String = "")
@Serializable
internal data class AlbumDto(
    val id: Long = 0,
    val name: String = "",
    val title: String = "",
    val pmid: String = "",
)

@Serializable
internal data class LyricRequestDto(
    val comm: LyricComm = LyricComm(),
    @SerialName("music.musichallSong.PlayLyricInfo.GetPlayLyricInfo") val call: LyricCall,
)
@Serializable
internal data class LyricComm(
    @SerialName("_channelid") val channelId: String = "",
    @SerialName("_os_version") val osVersion: String = "6.2.9200-2",
    val authst: String = "",
    val ct: Int = 11,
    val cv: String = "1003006",
    val patch: String = "118",
    @SerialName("psrf_access_token_expiresAt") val accessTokenExpiresAt: Int = 0,
    @SerialName("psrf_qqaccess_token") val accessToken: String = "",
    @SerialName("psrf_qqopenid") val openId: String = "",
    @SerialName("psrf_qqunionid") val unionId: String = "",
    val tmeAppID: String = "qqmusiclight",
    val tmeLoginType: Int = 0,
    val uin: String = "",
    val wid: String = "",
)
@Serializable
internal data class LyricCall(
    val method: String = "GetPlayLyricInfo",
    val module: String = "music.musichallSong.PlayLyricInfo",
    val param: LyricParams,
)
@Serializable
internal data class LyricParams(
    val albumName: String,
    val crypt: Int = 1,
    val ct: Int = 19,
    val cv: Int = 2111,
    val interval: Long,
    @SerialName("lrc_t") val lrcTime: Int = 0,
    val qrc: Int = 1,
    @SerialName("qrc_t") val qrcTime: Int = 0,
    val roma: Int = 1,
    @SerialName("roma_t") val romaTime: Int = 0,
    val singerName: String,
    val songID: Long,
    val songName: String,
    val trans: Int = 1,
    @SerialName("trans_t") val transTime: Int = 0,
    val type: Int = 0,
)
@Serializable
internal data class LyricResponse(
    @SerialName("music.musichallSong.PlayLyricInfo.GetPlayLyricInfo") val payload: LyricPayload? = null,
)
@Serializable
internal data class LyricPayload(val data: LyricData? = null)
@Serializable
internal data class LyricData(
    val lyric: String = "",
    val trans: String = "",
    val roma: String = "",
    @SerialName("qrc_t") val qrcTime: Int = 0,
)
