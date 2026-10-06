package com.ljyh.mei.data.model.response

import com.ljyh.mei.data.model.domain.metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Playback only needs the resolved URL; the server's quality metadata is ignored. */
@Serializable
data class SongUrl(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: List<Data> = emptyList(),
) {
    @Serializable
    data class Data(
        @SerialName("id") val id: Long = 0,
        @SerialName("url") val url: String? = null,
        @SerialName("encodeType") val encodeType: String? = null,
    )
}
