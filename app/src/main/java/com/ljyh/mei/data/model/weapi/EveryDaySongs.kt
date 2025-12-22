package com.ljyh.mei.data.model.weapi
import com.google.gson.annotations.SerializedName


data class EveryDaySongs(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data
) {
    data class Data(
        @SerializedName("fromCache")
        val fromCache: Boolean,
        @SerializedName("dailySongs")
        val dailySongs: List<DailySong>,
        @SerializedName("orderSongs")
        val orderSongs: List<Any>,
        @SerializedName("recommendReasons")
        val recommendReasons: List<RecommendReason>,
        @SerializedName("mvResourceInfos")
        val mvResourceInfos: Any,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("dailyRecommendInfo")
        val dailyRecommendInfo: Any
    ) {
        data class DailySong(
            @SerializedName("name")
            val name: String,
            @SerializedName("id")
            val id: Long,
            @SerializedName("pst")
            val pst: Int,
            @SerializedName("t")
            val t: Int,
            @SerializedName("ar")
            val ar: List<Ar>,
            @SerializedName("alia")
            val alia: List<String>,
            @SerializedName("pop")
            val pop: Int,
            @SerializedName("st")
            val st: Int,
            @SerializedName("rt")
            val rt: String,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("v")
            val v: Int,
            @SerializedName("crbt")
            val crbt: Any,
            @SerializedName("cf")
            val cf: String,
            @SerializedName("al")
            val al: Al,
            @SerializedName("dt")
            val dt: Long,
            @SerializedName("h")
            val h: H,
            @SerializedName("m")
            val m: M,
            @SerializedName("l")
            val l: L,
            @SerializedName("sq")
            val sq: Sq,
            @SerializedName("hr")
            val hr: Hr,
            @SerializedName("a")
            val a: Any,
            @SerializedName("cd")
            val cd: String,
            @SerializedName("no")
            val no: Int,
            @SerializedName("rtUrl")
            val rtUrl: Any,
            @SerializedName("ftype")
            val ftype: Int,
            @SerializedName("rtUrls")
            val rtUrls: List<Any>,
            @SerializedName("djId")
            val djId: Int,
            @SerializedName("copyright")
            val copyright: Int,
            @SerializedName("s_id")
            val sId: Long,
            @SerializedName("mark")
            val mark: Long,
            @SerializedName("originCoverType")
            val originCoverType: Int,
            @SerializedName("originSongSimpleData")
            val originSongSimpleData: Any,
            @SerializedName("tagPicList")
            val tagPicList: Any,
            @SerializedName("resourceState")
            val resourceState: Boolean,
            @SerializedName("version")
            val version: Int,
            @SerializedName("songJumpInfo")
            val songJumpInfo: Any,
            @SerializedName("entertainmentTags")
            val entertainmentTags: Any,
            @SerializedName("single")
            val single: Int,
            @SerializedName("noCopyrightRcmd")
            val noCopyrightRcmd: Any,
            @SerializedName("rtype")
            val rtype: Int,
            @SerializedName("rurl")
            val rurl: Any,
            @SerializedName("mst")
            val mst: Int,
            @SerializedName("cp")
            val cp: Int,
            @SerializedName("mv")
            val mv: Int,
            @SerializedName("publishTime")
            val publishTime: Long,
            @SerializedName("reason")
            val reason: String,
            @SerializedName("tns")
            val tns: List<String>?,
            @SerializedName("recommendReason")
            val recommendReason: String,
            @SerializedName("privilege")
            val privilege: Privilege,
            @SerializedName("alg")
            val alg: String
        ) {
            data class Ar(
                @SerializedName("id")
                val id: Long,
                @SerializedName("name")
                val name: String,
                @SerializedName("tns")
                val tns: List<Any>,
                @SerializedName("alias")
                val alias: List<String>
            )

            data class Al(
                @SerializedName("id")
                val id: Long,
                @SerializedName("name")
                val name: String,
                @SerializedName("picUrl")
                val picUrl: String,
                @SerializedName("tns")
                val tns: List<String>,
                @SerializedName("pic_str")
                val picStr: String,
                @SerializedName("pic")
                val pic: Long
            )

            data class H(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fid: Int,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Int,
                @SerializedName("sr")
                val sr: Int
            )

            data class M(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fid: Int,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Int,
                @SerializedName("sr")
                val sr: Int
            )

            data class L(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fid: Int,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Int,
                @SerializedName("sr")
                val sr: Int
            )

            data class Sq(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fid: Int,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Int,
                @SerializedName("sr")
                val sr: Int
            )

            data class Hr(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fid: Int,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Int,
                @SerializedName("sr")
                val sr: Int
            )

            data class Privilege(
                @SerializedName("id")
                val id: Long,
                @SerializedName("fee")
                val fee: Int,
                @SerializedName("payed")
                val payed: Int,
                @SerializedName("realPayed")
                val realPayed: Int,
                @SerializedName("st")
                val st: Int,
                @SerializedName("pl")
                val pl: Int,
                @SerializedName("dl")
                val dl: Int,
                @SerializedName("sp")
                val sp: Int,
                @SerializedName("cp")
                val cp: Int,
                @SerializedName("subp")
                val subp: Int,
                @SerializedName("cs")
                val cs: Boolean,
                @SerializedName("maxbr")
                val maxbr: Int,
                @SerializedName("fl")
                val fl: Int,
                @SerializedName("pc")
                val pc: Any,
                @SerializedName("toast")
                val toast: Boolean,
                @SerializedName("flag")
                val flag: Int,
                @SerializedName("paidBigBang")
                val paidBigBang: Boolean,
                @SerializedName("preSell")
                val preSell: Boolean,
                @SerializedName("playMaxbr")
                val playMaxbr: Int,
                @SerializedName("downloadMaxbr")
                val downloadMaxbr: Int,
                @SerializedName("maxBrLevel")
                val maxBrLevel: String,
                @SerializedName("playMaxBrLevel")
                val playMaxBrLevel: String,
                @SerializedName("downloadMaxBrLevel")
                val downloadMaxBrLevel: String,
                @SerializedName("plLevel")
                val plLevel: String,
                @SerializedName("dlLevel")
                val dlLevel: String,
                @SerializedName("flLevel")
                val flLevel: String,
                @SerializedName("rscl")
                val rscl: Any,
                @SerializedName("freeTrialPrivilege")
                val freeTrialPrivilege: FreeTrialPrivilege,
                @SerializedName("rightSource")
                val rightSource: Int,
                @SerializedName("chargeInfoList")
                val chargeInfoList: List<ChargeInfo>,
                @SerializedName("code")
                val code: Int,
                @SerializedName("message")
                val message: Any
            ) {
                data class FreeTrialPrivilege(
                    @SerializedName("resConsumable")
                    val resConsumable: Boolean,
                    @SerializedName("userConsumable")
                    val userConsumable: Boolean,
                    @SerializedName("listenType")
                    val listenType: Any,
                    @SerializedName("cannotListenReason")
                    val cannotListenReason: Any,
                    @SerializedName("playReason")
                    val playReason: Any,
                    @SerializedName("freeLimitTagType")
                    val freeLimitTagType: Any
                )

                data class ChargeInfo(
                    @SerializedName("rate")
                    val rate: Int,
                    @SerializedName("chargeUrl")
                    val chargeUrl: Any,
                    @SerializedName("chargeMessage")
                    val chargeMessage: Any,
                    @SerializedName("chargeType")
                    val chargeType: Int
                )
            }
        }

        data class RecommendReason(
            @SerializedName("songId")
            val songId: Long,
            @SerializedName("reason")
            val reason: String,
            @SerializedName("reasonId")
            val reasonId: String,
            @SerializedName("targetUrl")
            val targetUrl: Any
        )
    }
}