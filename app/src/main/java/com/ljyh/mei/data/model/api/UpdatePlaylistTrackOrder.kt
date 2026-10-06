package com.ljyh.mei.data.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UpdatePlaylistTrackOrder(
    val pid: String,
    val trackIds: String,
    val op: String,
) {
    constructor(pid: String, ids: List<Long>) : this(
        pid = pid,
        trackIds = Json.encodeToString(ids.map(Long::toString)),
        op = "update",
    )
}
