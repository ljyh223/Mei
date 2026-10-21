package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Audio URL duration is reported in milliseconds. */
@Serializable
data class SongUrl(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: List<Data> = emptyList(),
) {
    @Serializable
    data class FreeTrialInfo(val start: Long = 0, val end: Long = 0)

    @Serializable
    data class Data(
        @SerialName("id") val id: Long = 0,
        @SerialName("url") val url: String? = null,
        @SerialName("encodeType") val encodeType: String? = null,
        val time: Long? = null,
        val level: String? = null,
        val freeTrialInfo: FreeTrialInfo? = null,
    )
}
