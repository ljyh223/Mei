package com.ljyh.mei.data.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ManipulateTrack(
    val op:String,
    val pid:String,
    var trackIds:String,
    val imme:Boolean=true
){
    init {
        trackIds = Json.encodeToString(trackIds.split(","))
    }
}

@Serializable
data class ManipulateTrackResult(
    val code:Int,
    val message: String? = null,
    val cloudCount:Int,
    val count:Int,
    val trackIds:String
)
