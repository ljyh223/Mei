package com.ljyh.mei.data.model.weapi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class UserSubcount(
    @SerialName("artistCount")
    val artistCount: Int,
    @SerialName("code")
    val code: Int,
    @SerialName("createDjRadioCount")
    val createDjRadioCount: Int,
    @SerialName("createdPlaylistCount")
    val createdPlaylistCount: Int,
    @SerialName("djRadioCount")
    val djRadioCount: Int,
    @SerialName("mvCount")
    val mvCount: Int,
    @SerialName("newProgramCount")
    val newProgramCount: Int,
    @SerialName("programCount")
    val programCount: Int,
    @SerialName("subPlaylistCount")
    val subPlaylistCount: Int
)

