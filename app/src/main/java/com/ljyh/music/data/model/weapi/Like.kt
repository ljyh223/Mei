package com.ljyh.music.data.model.weapi

//const data = {
//    alg: 'itembased',
//    trackId: query.id,
//    like: query.like,
//    time: '3',
//}
data class Like(
    val alg: String = "itembased",
    val trackId: String,
    val like: Boolean,
    val time: String = "3"
)