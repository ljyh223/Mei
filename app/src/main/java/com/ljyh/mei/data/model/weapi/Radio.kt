package com.ljyh.mei.data.model.weapi

import com.google.gson.annotations.SerializedName


data class Radio(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("extTransMap")
    val extTransMap: ExtTransMap,
    @SerializedName("popAdjust")
    val popAdjust: Boolean,
    @SerializedName("tag")
    val tag: Any
)

data class Data(
    @SerializedName("album")
    val album: Album,
    @SerializedName("alg")
    val alg: String,
    @SerializedName("alias")
    val alias: List<Any?>,
    @SerializedName("artists")
    val artists: List<ArtistXX>,
    @SerializedName("audition")
    val audition: Any,
    @SerializedName("bMusic")
    val bMusic: BMusic,
    @SerializedName("commentThreadId")
    val commentThreadId: String,
    @SerializedName("copyFrom")
    val copyFrom: String,
    @SerializedName("copyright")
    val copyright: Int,
    @SerializedName("copyrightId")
    val copyrightId: Int,
    @SerializedName("crbt")
    val crbt: Any,
    @SerializedName("dayPlays")
    val dayPlays: Int,
    @SerializedName("disc")
    val disc: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("fee")
    val fee: Int,
    @SerializedName("ftype")
    val ftype: Int,
    @SerializedName("hMusic")
    val hMusic: HMusic,
    @SerializedName("hearTime")
    val hearTime: Int,
    @SerializedName("id")
    val id: Long,
    @SerializedName("lMusic")
    val lMusic: LMusic,
    @SerializedName("mMusic")
    val mMusic: MMusic,
    @SerializedName("mp3Url")
    val mp3Url: Any,
    @SerializedName("mvid")
    val mvid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("no")
    val no: Int,
    @SerializedName("playedNum")
    val playedNum: Int,
    @SerializedName("popularity")
    val popularity: Int,
    @SerializedName("position")
    val position: Int,
    @SerializedName("privilege")
    val privilege: Privilege,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("reasonId")
    val reasonId: String,
    @SerializedName("ringtone")
    val ringtone: String,
    @SerializedName("rtUrl")
    val rtUrl: Any,
    @SerializedName("rtUrls")
    val rtUrls: List<Any?>,
    @SerializedName("rtype")
    val rtype: Int,
    @SerializedName("rurl")
    val rurl: Any,
    @SerializedName("score")
    val score: Int,
    @SerializedName("sign")
    val sign: Any,
    @SerializedName("starred")
    val starred: Boolean,
    @SerializedName("starredNum")
    val starredNum: Int,
    @SerializedName("status")
    val status: Int,
    @SerializedName("transName")
    val transName: String,
    @SerializedName("transNames")
    val transNames: List<String>?
)

class ExtTransMap

data class Album(
    @SerializedName("alias")
    val alias: List<Any?>,
    @SerializedName("artist")
    val artist: ArtistXX,
    @SerializedName("artists")
    val artists: List<ArtistXX>,
    @SerializedName("blurPicUrl")
    val blurPicUrl: String,
    @SerializedName("briefDesc")
    val briefDesc: String,
    @SerializedName("commentThreadId")
    val commentThreadId: String,
    @SerializedName("company")
    val company: String,
    @SerializedName("companyId")
    val companyId: Int,
    @SerializedName("copyrightId")
    val copyrightId: Int,
    @SerializedName("description")
    val description: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("pic")
    val pic: Long,
    @SerializedName("picId")
    val picId: Long,
    @SerializedName("picId_str")
    val picIdStr: String,
    @SerializedName("picUrl")
    val picUrl: String,
    @SerializedName("publishTime")
    val publishTime: Long,
    @SerializedName("size")
    val size: Int,
    @SerializedName("songs")
    val songs: List<Any?>,
    @SerializedName("status")
    val status: Int,
    @SerializedName("subType")
    val subType: String,
    @SerializedName("tags")
    val tags: String,
    @SerializedName("transName")
    val transName: Any,
    @SerializedName("type")
    val type: String
)

data class ArtistXX(
    @SerializedName("albumSize")
    val albumSize: Int,
    @SerializedName("alias")
    val alias: List<String>?,
    @SerializedName("briefDesc")
    val briefDesc: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("img1v1Id")
    val img1v1Id: Int,
    @SerializedName("img1v1Url")
    val img1v1Url: String,
    @SerializedName("musicSize")
    val musicSize: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("picId")
    val picId: Int,
    @SerializedName("picUrl")
    val picUrl: String,
    @SerializedName("trans")
    val trans: String
)

data class BMusic(
    @SerializedName("bitrate")
    val bitrate: Int,
    @SerializedName("dfsId")
    val dfsId: Int,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: Any,
    @SerializedName("playTime")
    val playTime: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("volumeDelta")
    val volumeDelta: Int
)

data class HMusic(
    @SerializedName("bitrate")
    val bitrate: Int,
    @SerializedName("dfsId")
    val dfsId: Int,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: Any,
    @SerializedName("playTime")
    val playTime: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("volumeDelta")
    val volumeDelta: Int
)

data class LMusic(
    @SerializedName("bitrate")
    val bitrate: Int,
    @SerializedName("dfsId")
    val dfsId: Int,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: Any,
    @SerializedName("playTime")
    val playTime: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("volumeDelta")
    val volumeDelta: Int
)

data class MMusic(
    @SerializedName("bitrate")
    val bitrate: Int,
    @SerializedName("dfsId")
    val dfsId: Int,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: Any,
    @SerializedName("playTime")
    val playTime: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("volumeDelta")
    val volumeDelta: Int
)

data class Privilege(
    @SerializedName("bd")
    val bd: Any,
    @SerializedName("chargeInfoList")
    val chargeInfoList: List<ChargeInfo>,
    @SerializedName("code")
    val code: Int,
    @SerializedName("cp")
    val cp: Int,
    @SerializedName("cs")
    val cs: Boolean,
    @SerializedName("dl")
    val dl: Int,
    @SerializedName("dlLevel")
    val dlLevel: String,
    @SerializedName("dlLevels")
    val dlLevels: Any,
    @SerializedName("downloadMaxBrLevel")
    val downloadMaxBrLevel: String,
    @SerializedName("downloadMaxbr")
    val downloadMaxbr: Int,
    @SerializedName("fee")
    val fee: Int,
    @SerializedName("fl")
    val fl: Int,
    @SerializedName("flLevel")
    val flLevel: String,
    @SerializedName("flag")
    val flag: Int,
    @SerializedName("freeTrialPrivilege")
    val freeTrialPrivilege: FreeTrialPrivilege,
    @SerializedName("id")
    val id: Long,
    @SerializedName("ignoreCache")
    val ignoreCache: Any,
    @SerializedName("maxBrLevel")
    val maxBrLevel: String,
    @SerializedName("maxbr")
    val maxbr: Int,
    @SerializedName("message")
    val message: Any,
    @SerializedName("paidBigBang")
    val paidBigBang: Boolean,
    @SerializedName("payed")
    val payed: Int,
    @SerializedName("pc")
    val pc: Any,
    @SerializedName("pl")
    val pl: Int,
    @SerializedName("plLevel")
    val plLevel: String,
    @SerializedName("plLevels")
    val plLevels: Any,
    @SerializedName("playMaxBrLevel")
    val playMaxBrLevel: String,
    @SerializedName("playMaxbr")
    val playMaxbr: Int,
    @SerializedName("preSell")
    val preSell: Boolean,
    @SerializedName("realPayed")
    val realPayed: Int,
    @SerializedName("rightSource")
    val rightSource: Int,
    @SerializedName("rscl")
    val rscl: Any,
    @SerializedName("sp")
    val sp: Int,
    @SerializedName("st")
    val st: Int,
    @SerializedName("subp")
    val subp: Int,
    @SerializedName("toast")
    val toast: Boolean
)

data class ChargeInfo(
    @SerializedName("chargeMessage")
    val chargeMessage: Any,
    @SerializedName("chargeType")
    val chargeType: Int,
    @SerializedName("chargeUrl")
    val chargeUrl: Any,
    @SerializedName("rate")
    val rate: Int
)

data class FreeTrialPrivilege(
    @SerializedName("cannotListenReason")
    val cannotListenReason: Any,
    @SerializedName("freeLimitTagType")
    val freeLimitTagType: Any,
    @SerializedName("listenType")
    val listenType: Any,
    @SerializedName("playReason")
    val playReason: Any,
    @SerializedName("resConsumable")
    val resConsumable: Boolean,
    @SerializedName("userConsumable")
    val userConsumable: Boolean
)


