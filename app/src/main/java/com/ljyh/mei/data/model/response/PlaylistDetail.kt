package com.ljyh.mei.data.model.response

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal response shape shared by playlist detail and song detail endpoints. */
@Serializable
data class PlaylistDetail(
    @SerialName("code") val code: Int,
    @SerialName("playlist") val playlist: Playlist
) {
    @Serializable
    data class Playlist(
        @SerialName("coverImgUrl") val coverImgUrl: String,
        @SerialName("creator") val creator: Creator,
    @SerialName("description") val description: String? = null,
        @SerialName("id") val Id: Long,
        @SerialName("name") val name: String,
        @SerialName("playCount") val playCount: Long,
        @SerialName("subscribed") val subscribed: Boolean,
        @SerialName("subscribedCount") val subscribedCount: Long,
        @SerialName("trackCount") val trackCount: Int,
        @SerialName("trackIds") val trackIds: List<TrackId>,
        @SerialName("tracks") val tracks: List<Track>
    ) {
        @Serializable
        data class Creator(
            @SerialName("nickname") val nickname: String,
            @SerialName("userId") val userId: Long
        )

        @Serializable
        data class TrackId(@SerialName("id") val id: Long)

        @Serializable
        data class Track(
            @SerialName("al") val al: Album,
            @SerialName("ar") val ar: List<Artist>,
            @SerialName("dt") val dt: Long,
            @SerialName("id") val id: Long,
            @SerialName("name") val name: String,
            @SerialName("tns") val tns: List<String>? = null
        ) {
            @Serializable
            data class Album(
                @SerialName("id") val Id: Long,
                @SerialName("name") val name: String? = "",
                @SerialName("picUrl") val picUrl: String
            )

            @Serializable
            data class Artist(
                @SerialName("alias") val alias: List<String> = emptyList(),
                @SerialName("id") val Id: Long,
                @SerialName("name") val name: String? = ""
            )
        }
    }
}

/** Stable playlist fields needed by playback, playlist screens, and download flows. */
data class MiniPlaylistDetail(
    val cover: List<String>,
    val name: String,
    val description: String?,
    val id: Long,
    val tracks: List<MediaMetadata>,
    val trackIds: List<Long>,
    val count: Int,
    val creatorUserId: Long,
    val createUserName: String,
    val playCount: Long,
    val subscribedCount: Long,
    val subscribed: Boolean
)

internal fun PlaylistDetail.toDomain(): MiniPlaylistDetail {
    val playlist = playlist
    return MiniPlaylistDetail(
        cover = playlist.tracks.take(5).map { it.al.picUrl },
        name = playlist.name,
        description = playlist.description,
        id = playlist.Id,
        tracks = playlist.tracks.map { it.toMediaMetadata() },
        trackIds = playlist.trackIds.map { it.id },
        count = playlist.trackCount,
        creatorUserId = playlist.creator.userId,
        createUserName = playlist.creator.nickname,
        playCount = playlist.playCount,
        subscribedCount = playlist.subscribedCount,
        subscribed = playlist.subscribed
    )
}
