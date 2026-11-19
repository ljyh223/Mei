package com.ljyh.mei.playback.source.musicfree

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import com.ljyh.mei.playback.source.lx.LxCrypto
import com.ljyh.mei.playback.source.lx.LxHttpTransport
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Each operation owns its JS context and cancellable requests. */
class MusicFreeRuntime(
    private val utilities: String,
    private val modules: String,
    private val preload: String,
    private val appVersion: String,
    private val httpFactory: () -> LxHttpTransport = { LxHttpTransport() },
) {
    suspend fun <T> use(code: String, variables: Map<String, String> = emptyMap(), block: suspend Session.() -> T): T =
        withContext(Dispatchers.Default) {
            withTimeout(20_000) {
                val http = httpFactory()
                val js = QuickJs.create(Dispatchers.IO)
                val timers = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
                try {
                    js.memoryLimit = 64L * 1024 * 1024
                    js.maxStackSize = 1024L * 1024
                    js.evaluationTimeoutMillis = 2_000
                    js.asyncFunction("__meiHttp") { args -> http.request(args[0] as String) }
                    js.function("__meiCancel") { args -> http.cancel(args[0] as String); null }
                    js.function("__meiCrypto") { args -> LxCrypto.invoke(args[0] as String) }
                    js.asyncFunction("__meiDelay") { args ->
                        val id = args[0] as String
                        val timer = timers.getOrPut(id) { CompletableDeferred() }
                        try {
                            withTimeoutOrNull((args[1] as Number).toLong().coerceIn(1, 20_000)) { timer.await() }
                            null
                        } finally { timers.remove(id) }
                    }
                    js.function("__meiClearTimer") { args ->
                        timers.getOrPut(args[0] as String) { CompletableDeferred() }.complete(Unit)
                        null
                    }
                    js.evaluate<Any?>(utilities, filename = "musicfree-utilities.js")
                    js.evaluate<Any?>("""
                        globalThis.crypto = { getRandomValues(value) {
                            const bytes = lx.utils.crypto.randomBytes(value.byteLength);
                            new Uint8Array(value.buffer, value.byteOffset, value.byteLength).set(bytes);
                            return value;
                        }};
                    """.trimIndent())
                    js.evaluate<Any?>(modules, filename = "musicfree-modules.js")
                    js.evaluate<Any?>(preload, filename = "musicfree-host.js")
                    val environment = buildJsonObject {
                        put("os", "android")
                        put("appVersion", appVersion)
                        put("variables", buildJsonObject { variables.forEach { (key, value) -> put(key, value) } })
                    }
                    js.evaluate<Any?>("__mfHost.setup($environment)")
                    js.evaluate<Any?>(code, filename = "musicfree-plugin.js")
                    Session(js).block()
                } finally {
                    http.close()
                    js.close()
                }
            }
        }

    class Session internal constructor(private val js: QuickJs) {
        internal suspend fun inspect(): JsonObject =
            musicFreeJson.parseToJsonElement(js.evaluate<String>("__mfHost.inspect()")).jsonObject

        internal suspend fun call(method: String, args: JsonArray): JsonElement {
            val key = JsonPrimitive(UUID.randomUUID().toString())
            js.evaluate<Any?>("__mfHost.call($key, ${JsonPrimitive(method)}, $args)")
            return musicFreeJson.parseToJsonElement(js.evaluate<String>("__mfHost.result($key)"))
        }
    }
}
