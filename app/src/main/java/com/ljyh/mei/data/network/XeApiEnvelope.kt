package com.ljyh.mei.data.network

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Serializes an original request into the plaintext expected by the XEAPI cipher. */
internal fun XeApiRequest.encryptionPlaintext(): ByteArray {
    val query = url.encodedQuery.orEmpty()
    return buildJsonObject {
        if (originalBody is XeApiOriginalBody.JsonBody) put("contentType", "application/json")
        // The native interceptor appends e_r after the original encoded query.
        if (query.isNotEmpty()) put("queryString", "$query&e_r=true")
        when (val body = originalBody) {
            is XeApiOriginalBody.Form -> put("content", body.encodedContent)
            is XeApiOriginalBody.JsonBody -> put(
                "body",
                Base64.getEncoder().encodeToString(body.text.toByteArray(StandardCharsets.UTF_8)),
            )
        }
        if (query.isEmpty()) put("queryString", "e_r=true")
    }.toString().toByteArray(StandardCharsets.UTF_8)
}
