package com.ljyh.mei.data.model.api
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

//songId: query.id,
//type: 'fromPlayOne',
//playlistId: query.pid,
//startMusicId: query.sid || query.id,
//count: query.count || 1,

@Serializable

data class GetIntelligence(
    @SerialName("c")
    val songId: String,
    @SerialName("type")
    val type: String = "fromPlayOne",
    @SerialName("playlistId")
    val playlistId: String,
    @SerialName("startMusicId")
    val startMusicId: String,
    @SerialName("count")
    val count: Int = 1
)
@Serializable
data class Intelligence(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: List<Data>,
    @SerialName("message")
    val message: String
)

@Serializable

data class Data(
    @SerialName("alg")
    val alg: String,
    @SerialName("id")
    val id: Long,
    @SerialName("recommended")
    val recommended: Boolean,
    @SerialName("songInfo")
    val songInfo: SongInfo
)

@Serializable

data class SongInfo(
    @SerialName("a")
    val a: JsonElement,
    @SerialName("al")
    val al: Al,
    @SerialName("alia")
    val alia: List<String>,
    @SerialName("ar")
    val ar: List<Ar>,
    @SerialName("cd")
    val cd: String,
    @SerialName("cf")
    val cf: String,
    @SerialName("copyright")
    val copyright: Int,
    @SerialName("cp")
    val cp: Int,
    @SerialName("crbt")
    val crbt: JsonElement,
    @SerialName("djId")
    val djId: Int,
    @SerialName("dt")
    val dt: Int,
    @SerialName("fee")
    val fee: Int,
    @SerialName("ftype")
    val ftype: Int,
    @SerialName("h")
    val h: H,
    @SerialName("id")
    val id: Long,
    @SerialName("l")
    val l: L,
    @SerialName("m")
    val m: M,
    @SerialName("mst")
    val mst: Int,
    @SerialName("mv")
    val mv: Int,
    @SerialName("name")
    val name: String,
    @SerialName("no")
    val no: Int,
    @SerialName("pc")
    val pc: Pc,
    @SerialName("pop")
    val pop: Int,
    @SerialName("privilege")
    val privilege: Privilege,
    @SerialName("pst")
    val pst: Int,
    @SerialName("publishTime")
    val publishTime: Long,
    @SerialName("rt")
    val rt: String,
    @SerialName("rtUrl")
    val rtUrl: JsonElement,
    @SerialName("rtUrls")
    val rtUrls: List<JsonElement>,
    @SerialName("rtype")
    val rtype: Int,
    @SerialName("rurl")
    val rurl: JsonElement,
    @SerialName("s_id")
    val sId: Long,
    @SerialName("st")
    val st: Int,
    @SerialName("t")
    val t: Int,
    @SerialName("tns")
    val tns: List<String>? = null,
    @SerialName("v")
    val v: Int
)

@Serializable

data class Al(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("pic")
    val pic: Long,
    @SerialName("pic_str")
    val picStr: String,
    @SerialName("picUrl")
    val picUrl: String,
    @SerialName("tns")
    val tns: List<String>
)

@Serializable

data class Ar(
    @SerialName("alias")
    val alias: List<JsonElement>,
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("tns")
    val tns: List<String>? = null
)

@Serializable

data class H(
    @SerialName("br")
    val br: Int,
    @SerialName("fid")
    val fid: Int,
    @SerialName("size")
    val size: Int,
    @SerialName("sr")
    val sr: Int,
    @SerialName("vd")
    val vd: Int
)

@Serializable

data class L(
    @SerialName("br")
    val br: Int,
    @SerialName("fid")
    val fid: Int,
    @SerialName("size")
    val size: Int,
    @SerialName("sr")
    val sr: Int,
    @SerialName("vd")
    val vd: Int
)

@Serializable

data class M(
    @SerialName("br")
    val br: Int,
    @SerialName("fid")
    val fid: Int,
    @SerialName("size")
    val size: Int,
    @SerialName("sr")
    val sr: Int,
    @SerialName("vd")
    val vd: Int
)

@Serializable

data class Pc(
    @SerialName("alb")
    val alb: String,
    @SerialName("ar")
    val ar: String,
    @SerialName("br")
    val br: Int,
    @SerialName("cid")
    val cid: String,
    @SerialName("fn")
    val fn: String,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("sn")
    val sn: String,
    @SerialName("uid")
    val uid: Long
)

@Serializable

data class Privilege(
    @SerialName("bd")
    val bd: JsonElement,
    @SerialName("chargeInfoList")
    val chargeInfoList: List<ChargeInfo>,
    @SerialName("code")
    val code: Int,
    @SerialName("cp")
    val cp: Int,
    @SerialName("cs")
    val cs: Boolean,
    @SerialName("dl")
    val dl: Int,
    @SerialName("dlLevel")
    val dlLevel: String,
    @SerialName("dlLevels")
    val dlLevels: JsonElement,
    @SerialName("downloadMaxBrLevel")
    val downloadMaxBrLevel: String,
    @SerialName("downloadMaxbr")
    val downloadMaxbr: Int,
    @SerialName("fee")
    val fee: Int,
    @SerialName("fl")
    val fl: Int,
    @SerialName("flLevel")
    val flLevel: String,
    @SerialName("flag")
    val flag: Int,
    @SerialName("freeTrialPrivilege")
    val freeTrialPrivilege: FreeTrialPrivilege,
    @SerialName("id")
    val id: Long,
    @SerialName("ignoreCache")
    val ignoreCache: JsonElement,
    @SerialName("maxBrLevel")
    val maxBrLevel: String,
    @SerialName("maxbr")
    val maxbr: Int,
    @SerialName("message")
    val message: JsonElement,
    @SerialName("payed")
    val payed: Int,
    @SerialName("pl")
    val pl: Int,
    @SerialName("plLevel")
    val plLevel: String,
    @SerialName("plLevels")
    val plLevels: JsonElement,
    @SerialName("playMaxBrLevel")
    val playMaxBrLevel: String,
    @SerialName("playMaxbr")
    val playMaxbr: Int,
    @SerialName("preSell")
    val preSell: Boolean,
    @SerialName("rightSource")
    val rightSource: Int,
    @SerialName("rscl")
    val rscl: JsonElement,
    @SerialName("sp")
    val sp: Int,
    @SerialName("st")
    val st: Int,
    @SerialName("subp")
    val subp: Int,
    @SerialName("toast")
    val toast: Boolean
)

@Serializable

data class ChargeInfo(
    @SerialName("chargeMessage")
    val chargeMessage: JsonElement,
    @SerialName("chargeType")
    val chargeType: Int,
    @SerialName("chargeUrl")
    val chargeUrl: JsonElement,
    @SerialName("rate")
    val rate: Int
)

@Serializable

data class FreeTrialPrivilege(
    @SerialName("cannotListenReason")
    val cannotListenReason: JsonElement,
    @SerialName("freeLimitTagType")
    val freeLimitTagType: JsonElement,
    @SerialName("listenType")
    val listenType: JsonElement,
    @SerialName("playReason")
    val playReason: JsonElement,
    @SerialName("resConsumable")
    val resConsumable: Boolean,
    @SerialName("userConsumable")
    val userConsumable: Boolean
)
