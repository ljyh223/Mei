package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName


/**
 * 创建歌单请求参数
 */
data class CreatePlaylist(
    val name: String,
    val privacy: String = "0", // 0 普通歌单, 10 隐私歌单
    val type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
)

/**
 * 收藏/取消收藏歌单请求参数
 */
data class SubscribePlaylist(
    val id: String,
    val checkToken: String? = null
)

/**
 * 删除歌单请求参数
 */
data class DeletePlaylist(
    val ids: String // 歌单ID，格式: "[12345]"
)

/**
 * 收藏/取消收藏歌单响应结果
 */
data class SubscribePlaylistResult(
    val code: Int,
    val message: String
)


class CreatePlaylistResult(
    @SerializedName("code")
    val code: Int,
    @SerializedName("playlist")
    val playlist: Playlist,
    @SerializedName("id")
    val id: Long
) {
    data class Playlist(
        @SerializedName("subscribers")
        val subscribers: List<Any>,
        @SerializedName("subscribed")
        val subscribed: Any,
        @SerializedName("creator")
        val creator: Any,
        @SerializedName("artists")
        val artists: Any,
        @SerializedName("tracks")
        val tracks: Any,
        @SerializedName("top")
        val top: Boolean,
        @SerializedName("updateFrequency")
        val updateFrequency: Any,
        @SerializedName("backgroundCoverId")
        val backgroundCoverId: Int,
        @SerializedName("backgroundCoverUrl")
        val backgroundCoverUrl: Any,
        @SerializedName("titleImage")
        val titleImage: Int,
        @SerializedName("titleImageUrl")
        val titleImageUrl: Any,
        @SerializedName("englishTitle")
        val englishTitle: Any,
        @SerializedName("opRecommend")
        val opRecommend: Boolean,
        @SerializedName("recommendInfo")
        val recommendInfo: Any,
        @SerializedName("subscribedCount")
        val subscribedCount: Int,
        @SerializedName("cloudTrackCount")
        val cloudTrackCount: Int,
        @SerializedName("userId")
        val userId: Long,
        @SerializedName("totalDuration")
        val totalDuration: Int,
        @SerializedName("coverImgId")
        val coverImgId: Long,
        @SerializedName("privacy")
        val privacy: Int,
        @SerializedName("trackUpdateTime")
        val trackUpdateTime: Int,
        @SerializedName("trackCount")
        val trackCount: Int,
        @SerializedName("updateTime")
        val updateTime: Long,
        @SerializedName("commentThreadId")
        val commentThreadId: String,
        @SerializedName("coverImgUrl")
        val coverImgUrl: String,
        @SerializedName("specialType")
        val specialType: Int,
        @SerializedName("anonimous")
        val anonimous: Boolean,
        @SerializedName("createTime")
        val createTime: Long,
        @SerializedName("highQuality")
        val highQuality: Boolean,
        @SerializedName("newImported")
        val newImported: Boolean,
        @SerializedName("trackNumberUpdateTime")
        val trackNumberUpdateTime: Int,
        @SerializedName("playCount")
        val playCount: Int,
        @SerializedName("adType")
        val adType: Int,
        @SerializedName("description")
        val description: Any,
        @SerializedName("tags")
        val tags: List<Any>,
        @SerializedName("ordered")
        val ordered: Boolean,
        @SerializedName("status")
        val status: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("coverImgId_str")
        val coverImgIdStr: String,
        @SerializedName("sharedUsers")
        val sharedUsers: Any,
        @SerializedName("shareStatus")
        val shareStatus: Any,
        @SerializedName("copied")
        val copied: Boolean,
        @SerializedName("containsTracks")
        val containsTracks: Boolean
    )
}