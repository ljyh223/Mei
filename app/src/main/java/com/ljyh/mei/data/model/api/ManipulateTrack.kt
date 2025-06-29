package com.ljyh.mei.data.model.api

import com.google.gson.Gson

data class ManipulateTrack(
    val op:String,
    val pid:String,
    var trackIds:String,
    val imme:Boolean=true
){
    init {
        trackIds= Gson().toJson(trackIds.split(","))
    }
}


data class ManipulateTrackResult(
    val code:Int,
    val cloudCount:Int,
    val count:Int,
    val trackIds:String
)