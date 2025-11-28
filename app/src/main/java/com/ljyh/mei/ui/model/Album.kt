package com.ljyh.mei.ui.model

data class Album(
    val id: Long,
    val title: String,
    val cover: String,
    val size: Int,
    var artist: List<Artist>
){
    data class Artist(
        val id: Long,
        val name: String
    )
}