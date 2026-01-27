package com.ljyh.mei.data.model.weapi

import com.google.gson.annotations.SerializedName

data class HighQualityPlaylist(
    @SerializedName("cat")
    val category: String = "全部", // 全部,华语,欧美,韩语,日语,粤语,小语种,运动,ACG,影视原声,流行,摇滚,后摇,古风,民谣,轻音乐,电子,器乐,说唱,古典,爵士

    @SerializedName("limit")
    val limit: Int = 50,

    @SerializedName("lasttime")
    val lastTime: Long = 0L, // 歌单updateTime

    @SerializedName("total")
    val total: Boolean = true
)



data class HighQualityPlaylistResult(
    @SerializedName("code")
    val code: Int,
    @SerializedName("lasttime")
    val lasttime: Long,
    @SerializedName("more")
    val more: Boolean,
    @SerializedName("playlists")
    val playlists: List<Playlists>,
    @SerializedName("total")
    val total: Int
)

data class Playlists(
    @SerializedName("adType")
    val adType: Int,
    @SerializedName("anonimous")
    val anonimous: Boolean,
    @SerializedName("cloudTrackCount")
    val cloudTrackCount: Int,
    @SerializedName("commentCount")
    val commentCount: Int,
    @SerializedName("commentThreadId")
    val commentThreadId: String,
    @SerializedName("copywriter")
    val copywriter: String,
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
    @SerializedName("highQuality")
    val highQuality: Boolean,
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("newImported")
    val newImported: Boolean,
    @SerializedName("ordered")
    val ordered: Boolean,
    @SerializedName("playCount")
    val playCount: Int,
    @SerializedName("privacy")
    val privacy: Int,
    @SerializedName("recommendInfo")
    val recommendInfo: Any,
    @SerializedName("recommendText")
    val recommendText: Any,
    @SerializedName("shareCount")
    val shareCount: Int,
    @SerializedName("socialPlaylistCover")
    val socialPlaylistCover: Any,
    @SerializedName("specialType")
    val specialType: Int,
    @SerializedName("status")
    val status: Int,
    @SerializedName("subscribed")
    val subscribed: Boolean,
    @SerializedName("subscribedCount")
    val subscribedCount: Int,
    @SerializedName("subscribers")
    val subscribers: List<Subscriber>,
    @SerializedName("tag")
    val tag: String,
    @SerializedName("tags")
    val tags: List<String>,
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
    @SerializedName("updateTime")
    val updateTime: Long,
    @SerializedName("userId")
    val userId: Int
)

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
    val avatarDetail: AvatarDetail,
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
    val birthday: Long,
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
    val experts: Experts,
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
    val userId: Int,
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
    val birthday: Long,
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

data class AvatarDetail(
    @SerializedName("identityIconUrl")
    val identityIconUrl: String,
    @SerializedName("identityLevel")
    val identityLevel: Int,
    @SerializedName("userType")
    val userType: Int
)

data class Experts(
    @SerializedName("1")
    val x1: String,
    @SerializedName("2")
    val x2: String
)