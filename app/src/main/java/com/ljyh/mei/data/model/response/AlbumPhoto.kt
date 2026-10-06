package com.ljyh.mei.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class AlbumPhoto(val data: Data = Data()) {
    @Serializable
    data class Data(val records: List<Record> = emptyList()) {
        @Serializable
        data class Record(val imageUrl: String = "")
    }
}
