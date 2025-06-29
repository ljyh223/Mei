package com.ljyh.mei.data.model
import com.google.gson.annotations.SerializedName


data class UserPlaylist(
    @SerializedName("code")
    val code: Int,
    @SerializedName("more")
    val more: Boolean,
    @SerializedName("playlist")
    val playlist: List<Playlist>
) {
    data class Playlist(
        @SerializedName("adType")
        val adType: Int,
        @SerializedName("anonimous")
        val anonimous: Boolean,
        @SerializedName("artists")
        val artists: Any,
        @SerializedName("backgroundCoverId")
        val backgroundCoverId: Long,
        @SerializedName("backgroundCoverUrl")
        val backgroundCoverUrl: Any,
        @SerializedName("cloudTrackCount")
        val cloudTrackCount: Int,
        @SerializedName("commentThreadId")
        val commentThreadId: String,
        @SerializedName("containsTracks")
        val containsTracks: Boolean,
        @SerializedName("copied")
        val copied: Boolean,
        @SerializedName("coverImgId")
        val coverImgId: Long,
        @SerializedName("coverImgId_str")
        val coverImgIdStr: String,
        @SerializedName("coverImgUrl")
        val coverImgUrl: String,
        @SerializedName("createTime")
        val createTime: Long,
        @SerializedName("creator")
        val creator: Creator,
        @SerializedName("description")
        val description: String,
        @SerializedName("englishTitle")
        val englishTitle: Any,
        @SerializedName("highQuality")
        val highQuality: Boolean,
        @SerializedName("id")
        val id: Long,
        @SerializedName("name")
        val name: String,
        @SerializedName("newImported")
        val newImported: Boolean,
        @SerializedName("opRecommend")
        val opRecommend: Boolean,
        @SerializedName("ordered")
        val ordered: Boolean,
        @SerializedName("playCount")
        val playCount: Long,
        @SerializedName("privacy")
        val privacy: Int,
        @SerializedName("recommendInfo")
        val recommendInfo: Any,
        @SerializedName("shareStatus")
        val shareStatus: Any,
        @SerializedName("sharedUsers")
        val sharedUsers: Any,
        @SerializedName("specialType")
        val specialType: Int,
        @SerializedName("status")
        val status: Int,
        @SerializedName("subscribed")
        val subscribed: Boolean,
        @SerializedName("subscribedCount")
        val subscribedCount: Int,
        @SerializedName("subscribers")
        val subscribers: List<Any>,
        @SerializedName("tags")
        val tags: List<String>,
        @SerializedName("titleImage")
        val titleImage: Long,
        @SerializedName("titleImageUrl")
        val titleImageUrl: Any,
        @SerializedName("top")
        val top: Boolean,
        @SerializedName("totalDuration")
        val totalDuration: Int,
        @SerializedName("trackCount")
        val trackCount: Int,
        @SerializedName("trackNumberUpdateTime")
        val trackNumberUpdateTime: Long,
        @SerializedName("trackUpdateTime")
        val trackUpdateTime: Long,
        @SerializedName("tracks")
        val tracks: Any,
        @SerializedName("updateFrequency")
        val updateFrequency: Any,
        @SerializedName("updateTime")
        val updateTime: Long,
        @SerializedName("userId")
        val userId: Long
    ) {
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
            val expertTags: List<String>,
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
    }
}