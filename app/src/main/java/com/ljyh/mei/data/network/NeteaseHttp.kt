package com.ljyh.mei.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Keeps wire requests behind one transport while service methods retain their existing signatures. */
class NeteaseHttp(
    val client: HttpClient,
    val json: Json,
) {
    suspend inline fun <reified Request : Any, reified Response : Any> post(
        baseUrl: String,
        path: String,
        body: Request,
        cryptoMode: String? = null,
    ): Response {
        val rawBody = json.encodeToString(body)
        val response = client.post(baseUrl.trimEnd('/') + path) {
            if (cryptoMode != null) header("X-Netease-Crypto", cryptoMode)
            setBody(TextContent(rawBody, ContentType.Application.Json))
        }
        return json.decodeFromString(response.bodyAsText())
    }
}
