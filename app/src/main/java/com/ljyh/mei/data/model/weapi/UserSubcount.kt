package com.ljyh.mei.data.model.weapi
import com.google.gson.annotations.SerializedName

data class UserSubcount(
    @SerializedName("artistCount")
    val artistCount: Int,
    @SerializedName("code")
    val code: Int,
    @SerializedName("createDjRadioCount")
    val createDjRadioCount: Int,
    @SerializedName("createdPlaylistCount")
    val createdPlaylistCount: Int,
    @SerializedName("djRadioCount")
    val djRadioCount: Int,
    @SerializedName("mvCount")
    val mvCount: Int,
    @SerializedName("newProgramCount")
    val newProgramCount: Int,
    @SerializedName("programCount")
    val programCount: Int,
    @SerializedName("subPlaylistCount")
    val subPlaylistCount: Int
)


