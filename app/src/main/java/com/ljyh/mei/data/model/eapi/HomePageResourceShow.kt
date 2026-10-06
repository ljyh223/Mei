package com.ljyh.mei.data.model.eapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The home feed is polymorphic. Keep only its block discriminator and raw card payload. */
@Serializable
data class HomePageResourceShow(
    val code: Int = 0,
    val data: Data = Data(),
) {
    @Serializable
    data class Data(val blocks: List<Block> = emptyList()) {
        @Serializable
        data class Block(
            val positionCode: String = "",
            val dslData: JsonObject = JsonObject(emptyMap()),
        ) {
            /** Card payloads are decoded separately because each position code has its own shape. */
            object DslData {
                object BlockResource {
                    @Serializable
                    data class Resource(
                        val resourceId: String = "",
                        val resourceType: String = "",
                        val coverImg: String = "",
                        val title: String = "",
                        val singleLineTitle: String = "",
                        val subTitle: String? = null,
                        val iconDesc: IconDesc? = null,
                        val extInfo: ExtInfo = ExtInfo(),
                        val resourceExtInfo: ResourceExtInfo? = null,
                        val resourceInteractInfo: ResourceInteractInfo? = null,
                    ) {
                        @Serializable
                        data class IconDesc(val image: String = "")

                        @Serializable
                        data class ExtInfo(val songId: JsonElement? = null) {
                            val songIdText: String? get() = (songId as? JsonPrimitive)?.content
                        }

                        @Serializable
                        data class ResourceExtInfo(val coverText: List<String>? = null)

                        @Serializable
                        data class ResourceInteractInfo(val playCount: String? = null)
                    }
                }

                object HomeCommon {
                    object Content {
                        @Serializable
                        data class Item(val items: List<Item> = emptyList()) {
                            @Serializable
                            data class Item(
                                val resourceId: String = "",
                                val coverUrl: String = "",
                                val title: String = "",
                                val artistName: String = "",
                            )
                        }
                    }
                }
            }
        }
    }
}
