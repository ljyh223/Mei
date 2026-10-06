package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Account summary; only the profile nickname and signature are shown by the app. */
@Serializable
data class UserAccount(
    @SerialName("account") val account: JsonElement? = null,
    @SerialName("code") val code: Int = 0,
    @SerialName("profile") val profile: Profile? = null,
) {
    @Serializable
    data class Profile(
        @SerialName("avatarUrl") val avatarUrl: String = "",
        @SerialName("nickname") val nickname: String = "",
        @SerialName("signature") val signature: String = "",
        @SerialName("userId") val userId: Long = 0,
    )
}
