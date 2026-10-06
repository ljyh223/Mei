package com.ljyh.mei.data.model.weapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EveryDaySongs(
    @SerialName("code") val code: Int,
    @SerialName("data") val data: Data
) {
    @Serializable
    data class Data(
        @SerialName("dailySongs") val dailySongs: List<DailySong> = emptyList()
    ) {
        @Serializable
        data class DailySong(
            @SerialName("name") val name: String,
            @SerialName("id") val id: Long,
            @SerialName("ar") val ar: List<Artist>,
            @SerialName("al") val al: Album,
            @SerialName("dt") val dt: Long,
            @SerialName("tns") val tns: List<String>? = null
        ) {
            @Serializable
            data class Artist(
                @SerialName("id") val id: Long,
                @SerialName("name") val name: String,
                @SerialName("alias") val alias: List<String> = emptyList()
            )

            @Serializable
            data class Album(
                @SerialName("id") val id: Long,
                @SerialName("name") val name: String,
                @SerialName("picUrl") val picUrl: String
            )
        }
    }
}
