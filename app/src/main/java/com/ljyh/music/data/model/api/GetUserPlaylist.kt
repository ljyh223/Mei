package com.ljyh.music.data.model.api

//mapOf(
//"uid" to uid.toString(),
//"limit" to limit.toString(),
//"includeVideo" to "false"
//)


data class GetUserPlaylist(
    val uid: String,
    val limit: String = "50",
    val offset: String = "0",
    val includeVideo: String = "false"
)