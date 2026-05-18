package com.ljyh.mei.data.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiLyricClient @Inject constructor(
    private val gson: Gson
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AiLyricResultRaw(val lrc: String?, val tlyric: String?)

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String,
        requireJson: Boolean = true
    ): String? {
        val url = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val endpoint = "$url/v1/chat/completions"

        val bodyMap = mutableMapOf<String, Any>(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userMessage)
            )
        )
        if (requireJson) {
            bodyMap["response_format"] = mapOf("type" to "json_object")
        }

        val body = gson.toJson(bodyMap).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null

            if (!response.isSuccessful) {
                Timber.w("AI API error: ${response.code} ${response.message}")
                return null
            }

            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            chatResponse.choices.firstOrNull()?.message?.content?.trim()
        } catch (e: Exception) {
            Timber.e(e, "AI chat failed")
            null
        }
    }

    private data class ChatCompletionResponse(
        val choices: List<Choice>
    ) {
        data class Choice(
            val message: Message
        ) {
            data class Message(
                val content: String
            )
        }
    }
}
