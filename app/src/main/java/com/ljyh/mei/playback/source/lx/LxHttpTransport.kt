package com.ljyh.mei.playback.source.lx

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Deliberately independent of authenticated application clients and cookie stores. */
class LxHttpTransport(private val client: OkHttpClient = anonymousClient) : AutoCloseable {
    private val calls = ConcurrentHashMap<String, Call>()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()

    suspend fun request(raw: String): String {
        require(raw.length <= 2 * 1024 * 1024)
        val input = lxJson.parseToJsonElement(raw).jsonObject
        val id = input.string("id")
        val options = input["options"] as? JsonObject ?: JsonObject(emptyMap())
        val method = options.string("method", "GET").uppercase()
        val builder = Request.Builder().url(input.string("url"))
            .header("User-Agent", "Mozilla/5.0")
        (options["headers"] as? JsonObject)?.forEach { (k, v) -> builder.header(k, v.jsonPrimitive.content) }
        val contentType = (options["headers"] as? JsonObject)?.entries
            ?.firstOrNull { it.key.equals("content-type", true) }?.value?.jsonPrimitive?.content
        val form = options["form"] as? JsonObject
        val formData = options["formData"] as? JsonObject
        val body = when {
            form != null -> FormBody.Builder().apply { form.forEach { (k,v) -> add(k, v.jsonPrimitive.content) } }.build()
            formData != null -> MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                formData.forEach { (k,v) -> addFormDataPart(k, (v as? JsonPrimitive)?.content ?: v.toString()) }
            }.build()
            options["body"] != null && options["body"] != JsonNull -> {
                val value = options.getValue("body")
                val text = if (value is JsonPrimitive) value.content else value.toString()
                text.toRequestBody((contentType ?: "application/json").toMediaTypeOrNull())
            }
            method !in setOf("GET", "HEAD") -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        val call = client.newCall(builder.method(method, body).build())
        call.timeout().timeout(options.long("timeout", 8_000).coerceIn(1, 10_000), TimeUnit.MILLISECONDS)
        synchronized(calls) {
            require(calls.size < 16) { "Too many concurrent requests" }
            calls[id] = call
            if (cancelled.remove(id)) call.cancel()
        }
        return try {
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val result = response.use {
                                val source = it.body.source()
                                // Read at most MAX_BODY + 1; never buffer an unbounded response.
                                source.request(MAX_BODY + 1)
                                require(source.buffer.size <= MAX_BODY) { "Response too large" }
                                val text = source.readString(it.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
                                val parsed = if (it.header("Content-Type").orEmpty().contains("json", true)) {
                                    runCatching { lxJson.parseToJsonElement(text) }.getOrElse { JsonPrimitive(text) }
                                } else JsonPrimitive(text)
                                buildJsonObject {
                                    put("statusCode", it.code); put("statusMessage", it.message)
                                    put("headers", buildJsonObject { it.headers.names().forEach { k -> put(k.lowercase(), it.header(k)) } })
                                    put("body", parsed)
                                }.toString()
                            }
                            if (cont.isActive) cont.resume(result)
                        } catch (e: Exception) { if (cont.isActive) cont.resumeWithException(e) }
                    }
                })
            }
        } finally { calls.remove(id, call) }
    }

    fun cancel(id: String) {
        if (cancelled.size < 128) cancelled.add(id)
        calls.remove(id)?.cancel()
    }
    override fun close() { calls.values.forEach(Call::cancel); calls.clear(); cancelled.clear() }
    companion object {
        private const val MAX_BODY = 2L * 1024 * 1024
        private val anonymousClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build()
    }
}
