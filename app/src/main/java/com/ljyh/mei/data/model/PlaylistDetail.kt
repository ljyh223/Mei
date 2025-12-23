package com.ljyh.mei.data.model
import com.google.gson.annotations.SerializedName


data class PlaylistDetail(
    @SerializedName("code")
    val code: Int,
    @SerializedName("fromUserCount")
    val fromUserCount: Int,
    @SerializedName("fromUsers")
    val fromUsers: Any,
    @SerializedName("playlist")
    val playlist: Playlist,
    @SerializedName("privileges")
    val privileges: List<Privilege>,
    @SerializedName("relatedVideos")
    val relatedVideos: Any,
    @SerializedName("resEntrance")
    val resEntrance: Any,
    @SerializedName("sharedPrivilege")
    val sharedPrivilege: Any,
    @SerializedName("songFromUsers")
    val songFromUsers: Any,
    @SerializedName("urls")
    val urls: Any
) {
    data class Playlist(
        @SerializedName("adType")
        val adType: Int,
        @SerializedName("algTags")
        val algTags: Any,
        @SerializedName("backgroundCoverId")
        val backgroundCoverId: Long,
        @SerializedName("backgroundCoverUrl")
        val backgroundCoverUrl: Any,
        @SerializedName("bannedTrackIds")
        val bannedTrackIds: Any,
        @SerializedName("bizExtInfo")
        val bizExtInfo: BizExtInfo,
        @SerializedName("cloudTrackCount")
        val cloudTrackCount: Int,
        @SerializedName("commentCount")
        val commentCount: Int,
        @SerializedName("commentThreadId")
        val commentThreadId: String,
        @SerializedName("copied")
        val copied: Boolean,
        @SerializedName("coverImgId")
        val coverImgId: Long,
        @SerializedName("coverImgId_str")
        val coverImgIdStr: String,
        @SerializedName("coverImgUrl")
        val coverImgUrl: String,
        @SerializedName("coverStatus")
        val coverStatus: Int,
        @SerializedName("createTime")
        val createTime: Long,
        @SerializedName("creator")
        val creator: Creator,
        @SerializedName("description")
        val description: String,
        @SerializedName("detailPageTitle")
        val detailPageTitle: Any,
        @SerializedName("displayTags")
        val displayTags: Any,
        @SerializedName("displayUserInfoAsTagOnly")
        val displayUserInfoAsTagOnly: Boolean,
        @SerializedName("distributeTags")
        val distributeTags: List<Any>,
        @SerializedName("englishTitle")
        val englishTitle: Any,
        @SerializedName("gradeStatus")
        val gradeStatus: String,
        @SerializedName("highQuality")
        val highQuality: Boolean,
        @SerializedName("historySharedUsers")
        val historySharedUsers: Any,
        @SerializedName("id")
        val Id: Long,
        @SerializedName("mvResourceInfos")
        val mvResourceInfos: Any,
        @SerializedName("name")
        val name: String,
        @SerializedName("newDetailPageRemixVideo")
        val newDetailPageRemixVideo: Any,
        @SerializedName("newImported")
        val newImported: Boolean,
        @SerializedName("officialPlaylistType")
        val officialPlaylistType: Any,
        @SerializedName("opRecommend")
        val opRecommend: Boolean,
        @SerializedName("ordered")
        val ordered: Boolean,
        @SerializedName("playCount")
        val playCount: Long,
        @SerializedName("playlistType")
        val playlistType: String,
        @SerializedName("privacy")
        val privacy: Int,
        @SerializedName("relateResType")
        val relateResType: Any,
        @SerializedName("remixVideo")
        val remixVideo: Any,
        @SerializedName("score")
        val score: Any,
        @SerializedName("shareCount")
        val shareCount: Int,
        @SerializedName("sharedUsers")
        val sharedUsers: Any,
        @SerializedName("specialType")
        val specialType: Int,
        @SerializedName("status")
        val status: Int,
        @SerializedName("subscribed")
        val subscribed: Boolean,
        @SerializedName("subscribedCount")
        val subscribedCount: Long,
        @SerializedName("subscribers")
        val subscribers: List<Subscriber>,
        @SerializedName("tags")
        val tags: List<String>,
        @SerializedName("titleImageUrl")
        val titleImageUrl: Any,
        @SerializedName("trackCount")
        val trackCount: Int,
        @SerializedName("trackIds")
        val trackIds: List<TrackId>,
        @SerializedName("trackNumberUpdateTime")
        val trackNumberUpdateTime: Long,
        @SerializedName("trackUpdateTime")
        val trackUpdateTime: Long,
        @SerializedName("tracks")
        val tracks: List<Track>,
        @SerializedName("trialMode")
        val trialMode: Int,
        @SerializedName("updateFrequency")
        val updateFrequency: Any,
        @SerializedName("updateTime")
        val updateTime: Long,
        @SerializedName("userId")
        val userId: Long,
        @SerializedName("videoIds")
        val videoIds: Any,
        @SerializedName("videos")
        val videos: Any
    ) {
        class BizExtInfo

        data class Creator(
            @SerializedName("accountStatus")
            val accountStatus: Int,
            @SerializedName("anchor")
            val anchor: Boolean,
            @SerializedName("authStatus")
            val authStatus: Int,
            @SerializedName("authenticationTypes")
            val authenticationTypes: Int,
            @SerializedName("authority")
            val authority: Int,
            @SerializedName("avatarDetail")
            val avatarDetail: Any,
            @SerializedName("avatarImgId")
            val avatarImgId: Long,
            @SerializedName("avatarImgIdStr")
            val avatarImgIdStr: String,
            @SerializedName("avatarUrl")
            val avatarUrl: String,
            @SerializedName("backgroundImgId")
            val backgroundImgId: Long,
            @SerializedName("backgroundImgIdStr")
            val backgroundImgIdStr: String,
            @SerializedName("backgroundUrl")
            val backgroundUrl: String,
            @SerializedName("birthday")
            val birthday: Int,
            @SerializedName("city")
            val city: Int,
            @SerializedName("defaultAvatar")
            val defaultAvatar: Boolean,
            @SerializedName("description")
            val description: String,
            @SerializedName("detailDescription")
            val detailDescription: String,
            @SerializedName("djStatus")
            val djStatus: Int,
            @SerializedName("expertTags")
            val expertTags: Any,
            @SerializedName("experts")
            val experts: Any,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("gender")
            val gender: Int,
            @SerializedName("mutual")
            val mutual: Boolean,
            @SerializedName("nickname")
            val nickname: String,
            @SerializedName("province")
            val province: Int,
            @SerializedName("remarkName")
            val remarkName: Any,
            @SerializedName("signature")
            val signature: String,
            @SerializedName("userId")
            val userId: Long,
            @SerializedName("userType")
            val userType: Int,
            @SerializedName("vipType")
            val vipType: Int
        )

        data class Subscriber(
            @SerializedName("accountStatus")
            val accountStatus: Int,
            @SerializedName("anchor")
            val anchor: Boolean,
            @SerializedName("authStatus")
            val authStatus: Int,
            @SerializedName("authenticationTypes")
            val authenticationTypes: Int,
            @SerializedName("authority")
            val authority: Int,
            @SerializedName("avatarDetail")
            val avatarDetail: Any,
            @SerializedName("avatarImgId")
            val avatarImgId: Long,
            @SerializedName("avatarImgIdStr")
            val avatarImgIdStr: String,
            @SerializedName("avatarUrl")
            val avatarUrl: String,
            @SerializedName("backgroundImgId")
            val backgroundImgId: Long,
            @SerializedName("backgroundImgIdStr")
            val backgroundImgIdStr: String,
            @SerializedName("backgroundUrl")
            val backgroundUrl: String,
            @SerializedName("birthday")
            val birthday: Int,
            @SerializedName("city")
            val city: Int,
            @SerializedName("defaultAvatar")
            val defaultAvatar: Boolean,
            @SerializedName("description")
            val description: String,
            @SerializedName("detailDescription")
            val detailDescription: String,
            @SerializedName("djStatus")
            val djStatus: Int,
            @SerializedName("expertTags")
            val expertTags: Any,
            @SerializedName("experts")
            val experts: Any,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("gender")
            val gender: Int,
            @SerializedName("mutual")
            val mutual: Boolean,
            @SerializedName("nickname")
            val nickname: String,
            @SerializedName("province")
            val province: Int,
            @SerializedName("remarkName")
            val remarkName: Any,
            @SerializedName("signature")
            val signature: String,
            @SerializedName("userId")
            val userId: Long,
            @SerializedName("userType")
            val userType: Int,
            @SerializedName("vipType")
            val vipType: Int
        )

        data class TrackId(
            @SerializedName("alg")
            val alg: Any,
            @SerializedName("at")
            val at: Long,
            @SerializedName("dpr")
            val dpr: Any,
            @SerializedName("f")
            val f: Any,
            @SerializedName("id")
            val id: Long,
            @SerializedName("rcmdReason")
            val rcmdReason: String,
            @SerializedName("rcmdReasonTitle")
            val rcmdReasonTitle: String,
            @SerializedName("sc")
            val sc: Any,
            @SerializedName("sr")
            val sr: Any,
            @SerializedName("t")
            val t: Int,
            @SerializedName("uid")
            val uId: Long,
            @SerializedName("v")
            val v: Int
        )

        data class Track(
            @SerializedName("a")
            val a: Any,
            @SerializedName("al")
            val al: Al,
            @SerializedName("alg")
            val alg: Any,
            @SerializedName("alia")
            val alia: List<Any>,
            @SerializedName("ar")
            val ar: List<Ar>,
            @SerializedName("awardTags")
            val awardTags: Any,
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
            @SerializedName("displayReason")
            val displayReason: Any,
            @SerializedName("djId")
            val djId: Long,
            @SerializedName("dt")
            val dt: Long,
            @SerializedName("entertainmentTags")
            val entertainmentTags: Any,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("ftype")
            val ftype: Int,
            @SerializedName("h")
            val h: H,
            @SerializedName("hr")
            val hr: Hr,
            @SerializedName("id")
            val id: Long,
            @SerializedName("l")
            val l: L,
            @SerializedName("m")
            val m: M,
            @SerializedName("mark")
            val mark: Long,
            @SerializedName("mst")
            val mst: Int,
            @SerializedName("mv")
            val mv: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("no")
            val no: Int,
            @SerializedName("noCopyrightRcmd")
            val noCopyrightRcmd: Any,
            @SerializedName("originCoverType")
            val originCoverType: Int,
            @SerializedName("originSongSimpleData")
            val originSongSimpleData: OriginSongSimpleData,
            @SerializedName("pop")
            val pop: Int,
            @SerializedName("pst")
            val pst: Int,
            @SerializedName("publishTime")
            val publishTime: Long,
            @SerializedName("resourceState")
            val resourceState: Boolean,
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
            @SerializedName("single")
            val single: Int,
            @SerializedName("songJumpInfo")
            val songJumpInfo: Any,
            @SerializedName("sq")
            val sq: Sq,
            @SerializedName("st")
            val st: Int,
            @SerializedName("t")
            val t: Int,
            @SerializedName("tagPicList")
            val tagPicList: Any,
            @SerializedName("tns")
            val tns: List<String>?,
            @SerializedName("v")
            val v: Int,
            @SerializedName("version")
            val version: Int
        ) {
            data class Al(
                @SerializedName("id")
                val Id: Long,
                @SerializedName("name")
                val name: String? = "",
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
                val alias: List<String>,
                @SerializedName("id")
                val Id: Long,
                @SerializedName("name")
                val name: String? = "",
                @SerializedName("tns")
                val tns: List<Any>
            )

            data class H(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Double
            )

            data class Hr(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Double
            )

            data class L(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Double
            )

            data class M(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Double
            )

            data class OriginSongSimpleData(
                @SerializedName("albumMeta")
                val albumMeta: AlbumMeta,
                @SerializedName("artists")
                val artists: List<Artist>,
                @SerializedName("name")
                val name: String,
                @SerializedName("songId")
                val songId: Long
            ) {
                data class AlbumMeta(
                    @SerializedName("id")
                    val Id: Long,
                    @SerializedName("name")
                    val name: String
                )

                data class Artist(
                    @SerializedName("id")
                    val Id: Long,
                    @SerializedName("name")
                    val name: String
                )
            }

            data class Sq(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("vd")
                val vd: Double
            )
        }
    }

    data class Privilege(
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
    ) {
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
    }
}

fun PlaylistDetail.toMiniPlaylistDetail():MiniPlaylistDetail{
    return MiniPlaylistDetail(
        cover = playlist.tracks.take(5).map { it.al.picUrl },
        name = playlist.name,
        description = playlist.description,
        id = playlist.Id,
        tracks = playlist.tracks.map { it.toMediaMetadata() },
        trackIds = playlist.trackIds.map { it.id },
        count = playlist.trackCount,
        creatorUserId = playlist.creator.userId,
        createUserName = playlist.creator.nickname,
        playCount = playlist.playCount,
        subscribedCount = playlist.subscribedCount,
        subscribed = playlist.subscribed
    )
}

data class MiniPlaylistDetail(

    val cover:List<String>,
    val name:String,
    val description:String?,
    val id:Long,
    val tracks:List<MediaMetadata>,
    val trackIds:List<Long>,
    val count:Int,
    val creatorUserId:Long,
    val createUserName:String,
    val playCount:Long,
    val subscribedCount:Long,
    val subscribed:Boolean

)