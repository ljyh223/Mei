package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class UserDetail(
    @SerialName("code") val code: Int,
    @SerialName("level") val level: Int,
    @SerialName("listenSongs") val listenSongs: Int,
    @SerialName("profile") val profile: Profile,
) {
    @Serializable
    data class Profile(
        @SerialName("follows") val follows: Int,
        @SerialName("followeds") val followeds: Int,
    )
}

@Serializable

data class UserVipInfo(
    @SerialName("message") val message: String,
    @SerialName("code") val code: Int,
    @SerialName("data") val data: Data?,
) {
    @Serializable
    data class Data(
        @SerialName("redVipLevelIcon") val redVipLevelIcon: String?,
        @SerialName("redVipLevel") val redVipLevel: Int,
        @SerialName("redVipAnnualCount") val redVipAnnualCount: Int,
        @SerialName("associator") val associator: Benefit?,
        @SerialName("musicPackage") val musicPackage: Benefit?,
        @SerialName("familyVip") val familyVip: Benefit?,
        @SerialName("redplus") val redplus: Benefit?,
        @SerialName("redVipDynamicIconUrl") val redVipDynamicIconUrl: String?,
        @SerialName("redVipDynamicIconUrl2") val redVipDynamicIconUrl2: String?,
    )

    @Serializable

    data class Benefit(
        @SerialName("vipCode") val vipCode: Int = 0,
        @SerialName("expireTime") val expireTime: Long = 0L,
        @SerialName("iconUrl") val iconUrl: String?,
        @SerialName("dynamicIconUrl") val dynamicIconUrl: String?,
        @SerialName("vipLevel") val vipLevel: Int = 0,
        @SerialName("isSign") val isSign: Boolean = false,
        @SerialName("isSignDeduct") val isSignDeduct: Boolean = false,
        @SerialName("isSignIap") val isSignIap: Boolean = false,
        @SerialName("isSignIapDeduct") val isSignIapDeduct: Boolean = false,
    )

}
