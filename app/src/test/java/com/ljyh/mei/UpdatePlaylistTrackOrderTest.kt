package com.ljyh.mei

import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.model.api.UpdatePlaylistTrackOrder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePlaylistTrackOrderTest {
    @Test
    fun updateRequestCarriesTheCompleteOrderedIdList() {
        val payload = Json.parseToJsonElement(
            Json.encodeToString(UpdatePlaylistTrackOrder("123", listOf(3L, 1L, 2L)))
        ).jsonObject

        assertEquals("123", payload.getValue("pid").jsonPrimitive.content)
        assertEquals("update", payload.getValue("op").jsonPrimitive.content)
        assertEquals("[\"3\",\"1\",\"2\"]", payload.getValue("trackIds").jsonPrimitive.content)
    }

    @Test
    fun acceptsCodeOnlyResponse() {
        assertEquals(200, Json.decodeFromString<BaseResponse>("{\"code\":200}").code)
    }
}
