package com.ljyh.mei.data.model

import com.google.gson.annotations.SerializedName

data class AlbumDetail(
    @SerializedName("resourceState")
    val resourceState: Boolean,
    @SerializedName("songs")
    val songs: List<Song>,
    @SerializedName("code")
    val code: Int,
    @SerializedName("album")
    val album: Album
) {
    data class Song(
        @SerializedName("rtUrls")
        val rtUrls: List<Any>,
        @SerializedName("ar")
        val ar: List<Ar>,
        @SerializedName("al")
        val al: Al,
        @SerializedName("st")
        val st: Int,
        @SerializedName("noCopyrightRcmd")
        val noCopyrightRcmd: Any,
        @SerializedName("songJumpInfo")
        val songJumpInfo: Any,
        @SerializedName("djId")
        val djId: Int,
        @SerializedName("no")
        val no: Int,
        @SerializedName("fee")
        val fee: Int,
        @SerializedName("mv")
        val mv: Int,
        @SerializedName("t")
        val t: Int,
        @SerializedName("cd")
        val cd: String,
        @SerializedName("v")
        val v: Int,
        @SerializedName("rtype")
        val rtype: Int,
        @SerializedName("rurl")
        val rurl: Any,
        @SerializedName("pst")
        val pst: Int,
        @SerializedName("alia")
        val alia: List<String>,
        @SerializedName("pop")
        val pop: Int,
        @SerializedName("rt")
        val rt: Any,
        @SerializedName("mst")
        val mst: Int,
        @SerializedName("cp")
        val cp: Int,
        @SerializedName("crbt")
        val crbt: Any,
        @SerializedName("cf")
        val cf: String,
        @SerializedName("dt")
        val dt: Long,
        @SerializedName("h")
        val h: H,
        @SerializedName("sq")
        val sq: Sq,
        @SerializedName("hr")
        val hr: Any,
        @SerializedName("l")
        val l: L,
        @SerializedName("rtUrl")
        val rtUrl: Any,
        @SerializedName("ftype")
        val ftype: Int,
        @SerializedName("a")
        val a: Any,
        @SerializedName("m")
        val m: M,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("videoInfo")
        val videoInfo: VideoInfo,
        @SerializedName("privilege")
        val privilege: Privilege
    ) {
        data class Ar(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String
        )

        data class Al(
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
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

        data class VideoInfo(
            @SerializedName("moreThanOne")
            val moreThanOne: Boolean,
            @SerializedName("video")
            val video: Video
        ) {
            data class Video(
                @SerializedName("vid")
                val vid: String,
                @SerializedName("type")
                val type: Int,
                @SerializedName("title")
                val title: String,
                @SerializedName("playTime")
                val playTime: Int,
                @SerializedName("coverUrl")
                val coverUrl: String,
                @SerializedName("publishTime")
                val publishTime: Long,
                @SerializedName("artists")
                val artists: Any
            )
        }

        data class Privilege(
            @SerializedName("id")
            val id: Long,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("payed")
            val payed: Int,
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
            @SerializedName("toast")
            val toast: Boolean,
            @SerializedName("flag")
            val flag: Int,
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
            val message: Any,
            @SerializedName("plLevels")
            val plLevels: Any,
            @SerializedName("dlLevels")
            val dlLevels: Any,
            @SerializedName("ignoreCache")
            val ignoreCache: Any,
            @SerializedName("bd")
            val bd: Any
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

    data class Album(
        @SerializedName("songs")
        val songs: List<Any>,
        @SerializedName("paid")
        val paid: Boolean,
        @SerializedName("onSale")
        val onSale: Boolean,
        @SerializedName("mark")
        val mark: Int,
        @SerializedName("awardTags")
        val awardTags: Any,
        @SerializedName("displayTags")
        val displayTags: Any,
        @SerializedName("artists")
        val artists: List<Artist>,
        @SerializedName("copyrightId")
        val copyrightId: Int,
        @SerializedName("picId")
        val picId: Long,
        @SerializedName("artist")
        val artist: Artist,
        @SerializedName("briefDesc")
        val briefDesc: Any,
        @SerializedName("publishTime")
        val publishTime: Long,
        @SerializedName("company")
        val company: String,
        @SerializedName("picUrl")
        val picUrl: String,
        @SerializedName("commentThreadId")
        val commentThreadId: String,
        @SerializedName("blurPicUrl")
        val blurPicUrl: String,
        @SerializedName("companyId")
        val companyId: Int,
        @SerializedName("pic")
        val pic: Long,
        @SerializedName("status")
        val status: Int,
        @SerializedName("subType")
        val subType: String,
        @SerializedName("description")
        val description: String,
        @SerializedName("tags")
        val tags: String,
        @SerializedName("alias")
        val alias: List<Any>,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("type")
        val type: String,
        @SerializedName("size")
        val size: Int,
        @SerializedName("picId_str")
        val picIdStr: String,
        @SerializedName("info")
        val info: Info
    ) {
        data class Artist(
            @SerializedName("img1v1Id")
            val img1v1Id: Long,
            @SerializedName("topicPerson")
            val topicPerson: Int,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("musicSize")
            val musicSize: Int,
            @SerializedName("albumSize")
            val albumSize: Int,
            @SerializedName("briefDesc")
            val briefDesc: String,
            @SerializedName("picUrl")
            val picUrl: String,
            @SerializedName("img1v1Url")
            val img1v1Url: String,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("trans")
            val trans: String,
            @SerializedName("alias")
            val alias: List<Any>,
            @SerializedName("name")
            val name: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("img1v1Id_str")
            val img1v1IdStr: String
        )



        data class Info(
            @SerializedName("commentThread")
            val commentThread: CommentThread,
            @SerializedName("latestLikedUsers")
            val latestLikedUsers: Any,
            @SerializedName("liked")
            val liked: Boolean,
            @SerializedName("comments")
            val comments: Any,
            @SerializedName("resourceType")
            val resourceType: Int,
            @SerializedName("resourceId")
            val resourceId: Int,
            @SerializedName("commentCount")
            val commentCount: Int,
            @SerializedName("likedCount")
            val likedCount: Int,
            @SerializedName("shareCount")
            val shareCount: Int,
            @SerializedName("threadId")
            val threadId: String
        ) {
            data class CommentThread(
                @SerializedName("id")
                val id: String,
                @SerializedName("resourceInfo")
                val resourceInfo: ResourceInfo,
                @SerializedName("resourceType")
                val resourceType: Int,
                @SerializedName("commentCount")
                val commentCount: Int,
                @SerializedName("likedCount")
                val likedCount: Int,
                @SerializedName("shareCount")
                val shareCount: Int,
                @SerializedName("hotCount")
                val hotCount: Int,
                @SerializedName("latestLikedUsers")
                val latestLikedUsers: Any,
                @SerializedName("resourceId")
                val resourceId: Int,
                @SerializedName("resourceOwnerId")
                val resourceOwnerId: Int,
                @SerializedName("resourceTitle")
                val resourceTitle: String
            ) {
                data class ResourceInfo(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("userId")
                    val userId: Int,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("imgUrl")
                    val imgUrl: String,
                    @SerializedName("creator")
                    val creator: Any,
                    @SerializedName("encodedId")
                    val encodedId: Any,
                    @SerializedName("subTitle")
                    val subTitle: Any,
                    @SerializedName("webUrl")
                    val webUrl: Any
                )
            }
        }
    }
}

data class MiniAlbumDetail(
    val name: String,
    val id: Long,
    val picUrl: String,
    val description: String,
    val artist: List<Artist>,
    val publishTime: Long,
    val collected: Boolean = false
){
    data class Artist(
        val name: String,
        val id: Int
    )
}

fun AlbumDetail.toMiniAlbumDetail(): MiniAlbumDetail {
    return MiniAlbumDetail(
        name = album.name,
        id = album.id,
        picUrl = album.picUrl,
        description = album.description,
        publishTime = album.publishTime,
        artist = album.artists.map {
            MiniAlbumDetail.Artist(
                name = it.name,
                id = it.id
            )
        }
    )
}