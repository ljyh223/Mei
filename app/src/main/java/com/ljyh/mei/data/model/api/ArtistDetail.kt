package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GetArtistDetail(val id: String)

/** Artist profile fields rendered by ArtistScreen. */
@Serializable
data class ArtistDetail(
    @SerialName("code") val code: Int = 0,
    @SerialName("data") val data: Data = Data(),
) {
    @Serializable
    data class Data(
        @SerialName("artist") val artist: Artist = Artist(),
        @SerialName("secondaryExpertIdentiy")
        val secondaryExpertIdentiy: List<SecondaryExpertIdentiy> = emptyList(),
    ) {
        @Serializable
        data class Artist(
            @SerialName("cover") val cover: String = "",
            @SerialName("avatar") val avatar: String = "",
            @SerialName("name") val name: String = "",
            @SerialName("transNames") val transNames: List<String> = emptyList(),
            @SerialName("alias") val alias: List<JsonElement> = emptyList(),
            @SerialName("identities") val identities: List<String> = emptyList(),
            @SerialName("identifyTag") val identifyTag: List<String>? = null,
            @SerialName("briefDesc") val briefDesc: String = "",
            @SerialName("albumSize") val albumSize: Int = 0,
            @SerialName("musicSize") val musicSize: Int = 0,
            @SerialName("mvSize") val mvSize: Int = 0,
        )

        @Serializable
        data class SecondaryExpertIdentiy(
            @SerialName("expertIdentiyName") val expertIdentiyName: String = "",
            @SerialName("expertIdentiyCount") val expertIdentiyCount: Int = 0,
        )
    }
}
