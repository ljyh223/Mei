package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 创建歌单请求参数
 */
@Serializable
data class CreatePlaylist(
    val name: String,
    val privacy: String = "0", // 0 普通歌单, 10 隐私歌单
    val type: String = "NORMAL" // 默认 NORMAL, VIDEO 视频歌单, SHARED 共享歌单
)

/**
 * 收藏/取消收藏歌单请求参数
 */
@Serializable
data class SubscribePlaylist(
    val id: String,
    val checkToken: String? = null
)

/**
 * 删除歌单请求参数
 */
@Serializable
data class DeletePlaylist(
    val ids: String // 歌单ID，格式: "[12345]"
)

/**
 * 收藏/取消收藏歌单响应结果
 */
@Serializable
data class SubscribePlaylistResult(
    val code: Int,
    val message: String
)

@Serializable
class CreatePlaylistResult(
    @SerialName("code")
    val code: Int,
    @SerialName("playlist")
    val playlist: Playlist,
    @SerialName("id")
    val id: Long
) {
    @Serializable
    data class Playlist(
        @SerialName("subscribers")
        val subscribers: List<JsonElement>,
        @SerialName("subscribed")
        val subscribed: JsonElement,
        @SerialName("creator")
        val creator: JsonElement,
        @SerialName("artists")
        val artists: JsonElement,
        @SerialName("tracks")
        val tracks: JsonElement,
        @SerialName("top")
        val top: Boolean,
        @SerialName("updateFrequency")
        val updateFrequency: JsonElement,
        @SerialName("backgroundCoverId")
        val backgroundCoverId: Int,
        @SerialName("backgroundCoverUrl")
        val backgroundCoverUrl: JsonElement,
        @SerialName("titleImage")
        val titleImage: Int,
        @SerialName("titleImageUrl")
        val titleImageUrl: JsonElement,
        @SerialName("englishTitle")
        val englishTitle: JsonElement,
        @SerialName("opRecommend")
        val opRecommend: Boolean,
        @SerialName("recommendInfo")
        val recommendInfo: JsonElement,
        @SerialName("subscribedCount")
        val subscribedCount: Int,
        @SerialName("cloudTrackCount")
        val cloudTrackCount: Int,
        @SerialName("userId")
        val userId: Long,
        @SerialName("totalDuration")
        val totalDuration: Int,
        @SerialName("coverImgId")
        val coverImgId: Long,
        @SerialName("privacy")
        val privacy: Int,
        @SerialName("trackUpdateTime")
        val trackUpdateTime: Int,
        @SerialName("trackCount")
        val trackCount: Int,
        @SerialName("updateTime")
        val updateTime: Long,
        @SerialName("commentThreadId")
        val commentThreadId: String,
        @SerialName("coverImgUrl")
        val coverImgUrl: String,
        @SerialName("specialType")
        val specialType: Int,
        @SerialName("anonimous")
        val anonimous: Boolean,
        @SerialName("createTime")
        val createTime: Long,
        @SerialName("highQuality")
        val highQuality: Boolean,
        @SerialName("newImported")
        val newImported: Boolean,
        @SerialName("trackNumberUpdateTime")
        val trackNumberUpdateTime: Int,
        @SerialName("playCount")
        val playCount: Int,
        @SerialName("adType")
        val adType: Int,
        @SerialName("description")
        val description: JsonElement,
        @SerialName("tags")
        val tags: List<JsonElement>,
        @SerialName("ordered")
        val ordered: Boolean,
        @SerialName("status")
        val status: Int,
        @SerialName("name")
        val name: String,
        @SerialName("id")
        val id: Long,
        @SerialName("coverImgId_str")
        val coverImgIdStr: String,
        @SerialName("sharedUsers")
        val sharedUsers: JsonElement,
        @SerialName("shareStatus")
        val shareStatus: JsonElement,
        @SerialName("copied")
        val copied: Boolean,
        @SerialName("containsTracks")
        val containsTracks: Boolean
    )
}
