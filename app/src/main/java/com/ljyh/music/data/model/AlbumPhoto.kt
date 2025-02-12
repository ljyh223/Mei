package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class AlbumPhoto(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String
) {
    data class Data(
        @SerializedName("page")
        val page: Page,
        @SerializedName("records")
        val records: List<Record>,
        @SerializedName("totalSize")
        val totalSize: Int
    ) {
        data class Page(
            @SerializedName("cursor")
            val cursor: String,
            @SerializedName("more")
            val more: Boolean,
            @SerializedName("size")
            val size: Int
        )

        data class Record(
            @SerializedName("backgroundId")
            val backgroundId: String,
            @SerializedName("createTime")
            val createTime: Long,
            @SerializedName("docId")
            val docId: Any,
            @SerializedName("duration")
            val duration: Int,
            @SerializedName("imageUrl")
            val imageUrl: String,
            @SerializedName("materialId")
            val materialId: String,
            @SerializedName("nosBucket")
            val nosBucket: String,
            @SerializedName("nosKey")
            val nosKey: String,
            @SerializedName("praiseCount")
            val praiseCount: Int,
            @SerializedName("praised")
            val praised: Boolean,
            @SerializedName("resId")
            val resId: Any,
            @SerializedName("resType")
            val resType: String,
            @SerializedName("resourceId")
            val resourceId: String,
            @SerializedName("text")
            val text: Any,
            @SerializedName("threadId")
            val threadId: Any,
            @SerializedName("type")
            val type: String,
            @SerializedName("userId")
            val userId: Long,
            @SerializedName("videoUrl")
            val videoUrl: Any
        )
    }
}