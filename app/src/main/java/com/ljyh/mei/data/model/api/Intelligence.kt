package com.ljyh.mei.data.model.api
import com.google.gson.annotations.SerializedName


//songId: query.id,
//type: 'fromPlayOne',
//playlistId: query.pid,
//startMusicId: query.sid || query.id,
//count: query.count || 1,


data class GetIntelligence(
    @SerializedName("c")
    val songId: String,
    @SerializedName("type")
    val type: String = "fromPlayOne",
    @SerializedName("playlistId")
    val playlistId: String,
    @SerializedName("startMusicId")
    val startMusicId: String,
    @SerializedName("count")
    val count: Int = 1
)
data class Intelligence(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String
)

data class Data(
    @SerializedName("alg")
    val alg: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("recommended")
    val recommended: Boolean,
    @SerializedName("songInfo")
    val songInfo: SongInfo
)

data class SongInfo(
    @SerializedName("a")
    val a: Any,
    @SerializedName("al")
    val al: Al,
    @SerializedName("alia")
    val alia: List<String>,
    @SerializedName("ar")
    val ar: List<Ar>,
    @SerializedName("cd")
    val cd: String,
    @SerializedName("cf")
    val cf: String,
    @SerializedName("copyright")
    val copyright: Int,
    @SerializedName("cp")
    val cp: Int,
    @SerializedName("crbt")
    val crbt: Any,
    @SerializedName("djId")
    val djId: Int,
    @SerializedName("dt")
    val dt: Int,
    @SerializedName("fee")
    val fee: Int,
    @SerializedName("ftype")
    val ftype: Int,
    @SerializedName("h")
    val h: H,
    @SerializedName("id")
    val id: Long,
    @SerializedName("l")
    val l: L,
    @SerializedName("m")
    val m: M,
    @SerializedName("mst")
    val mst: Int,
    @SerializedName("mv")
    val mv: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("no")
    val no: Int,
    @SerializedName("pc")
    val pc: Pc,
    @SerializedName("pop")
    val pop: Int,
    @SerializedName("privilege")
    val privilege: Privilege,
    @SerializedName("pst")
    val pst: Int,
    @SerializedName("publishTime")
    val publishTime: Long,
    @SerializedName("rt")
    val rt: String,
    @SerializedName("rtUrl")
    val rtUrl: Any,
    @SerializedName("rtUrls")
    val rtUrls: List<Any>,
    @SerializedName("rtype")
    val rtype: Int,
    @SerializedName("rurl")
    val rurl: Any,
    @SerializedName("s_id")
    val sId: Long,
    @SerializedName("st")
    val st: Int,
    @SerializedName("t")
    val t: Int,
    @SerializedName("tns")
    val tns: List<String>?,
    @SerializedName("v")
    val v: Int
)

data class Al(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("pic")
    val pic: Long,
    @SerializedName("pic_str")
    val picStr: String,
    @SerializedName("picUrl")
    val picUrl: String,
    @SerializedName("tns")
    val tns: List<String>
)

data class Ar(
    @SerializedName("alias")
    val alias: List<Any>,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("tns")
    val tns: List<String>?
)

data class H(
    @SerializedName("br")
    val br: Int,
    @SerializedName("fid")
    val fid: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("vd")
    val vd: Int
)

data class L(
    @SerializedName("br")
    val br: Int,
    @SerializedName("fid")
    val fid: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("vd")
    val vd: Int
)

data class M(
    @SerializedName("br")
    val br: Int,
    @SerializedName("fid")
    val fid: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("sr")
    val sr: Int,
    @SerializedName("vd")
    val vd: Int
)

data class Pc(
    @SerializedName("alb")
    val alb: String,
    @SerializedName("ar")
    val ar: String,
    @SerializedName("br")
    val br: Int,
    @SerializedName("cid")
    val cid: String,
    @SerializedName("fn")
    val fn: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("sn")
    val sn: String,
    @SerializedName("uid")
    val uid: Long
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
    @SerializedName("payed")
    val payed: Int,
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







