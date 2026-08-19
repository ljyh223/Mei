package com.ljyh.mei.data.model

import com.google.gson.annotations.SerializedName

data class UserDetail(
    @SerializedName("code") val code: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("listenSongs") val listenSongs: Int,
    @SerializedName("profile") val profile: Profile,
) {
    data class Profile(
        @SerializedName("follows") val follows: Int,
        @SerializedName("followeds") val followeds: Int,
    )
}

data class UserVipInfo(
    @SerializedName("message") val message: String,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: Data?,
) {
    data class Data(
        @SerializedName("redVipLevelIcon") val redVipLevelIcon: String?,
        @SerializedName("redVipLevel") val redVipLevel: Int,
        @SerializedName("redVipAnnualCount") val redVipAnnualCount: Int,
        @SerializedName("associator") val associator: Benefit?,
        @SerializedName("musicPackage") val musicPackage: Benefit?,
        @SerializedName("familyVip") val familyVip: Benefit?,
        @SerializedName("redplus") val redplus: Benefit?,
        @SerializedName("redVipDynamicIconUrl") val redVipDynamicIconUrl: String?,
        @SerializedName("redVipDynamicIconUrl2") val redVipDynamicIconUrl2: String?,
    )

    data class Benefit(
        @SerializedName("vipCode") val vipCode: Int = 0,
        @SerializedName("expireTime") val expireTime: Long = 0L,
        @SerializedName("iconUrl") val iconUrl: String?,
        @SerializedName("dynamicIconUrl") val dynamicIconUrl: String?,
        @SerializedName("vipLevel") val vipLevel: Int = 0,
        @SerializedName("isSign") val isSign: Boolean = false,
        @SerializedName("isSignDeduct") val isSignDeduct: Boolean = false,
        @SerializedName("isSignIap") val isSignIap: Boolean = false,
        @SerializedName("isSignIapDeduct") val isSignIapDeduct: Boolean = false,
    )

}
