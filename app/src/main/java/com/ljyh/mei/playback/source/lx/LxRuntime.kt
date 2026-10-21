package com.ljyh.mei.playback.source.lx

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.util.UUID

/** One runtime per script attempt. A failed or cancelled script cannot poison another script. */
class LxRuntime(
    private val preload: String,
    private val httpFactory: () -> LxHttpTransport = { LxHttpTransport() },
) {
    suspend fun <T> use(script: LxScript, code: String, block: suspend Session.() -> T): T = withContext(Dispatchers.Default) {
        withTimeout(15_000) {
            val http = httpFactory()
            val js = QuickJs.create(Dispatchers.IO)
            val timers = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
            try {
                js.memoryLimit = 32L * 1024 * 1024
                js.maxStackSize = 512L * 1024
                js.evaluationTimeoutMillis = 2_000
                js.asyncFunction("__meiHttp") { args -> http.request(args[0] as String) }
                js.function("__meiCancel") { args -> http.cancel(args[0] as String); null }
                js.function("__meiCrypto") { args -> LxCrypto.invoke(args[0] as String) }
                js.asyncFunction("__meiDelay") { args ->
                    val id = args[0] as String
                    val timer = timers.getOrPut(id) { CompletableDeferred() }
                    try { withTimeoutOrNull((args[1] as Number).toLong().coerceIn(1, 15_000)) { timer.await() }; null }
                    finally { timers.remove(id) }
                }
                js.function("__meiClearTimer") { args -> timers.getOrPut(args[0] as String) { CompletableDeferred() }.complete(Unit); null }
                js.evaluate<Any?>(preload, filename = "lx-host.js")
                val info = buildJsonObject {
                    put("name", script.name); put("version", script.version); put("author", script.author)
                    put("description", script.description); put("homepage", script.homepage); put("rawScript", code)
                }
                js.evaluate<Any?>("__meiHost.setup($info)")
                js.evaluate<Any?>(code, filename = "user-source.js")
                val raw = js.evaluate<String>("__meiHost.capabilities()")
                val sources = lxJson.decodeFromString<Map<String, LxCapability>>(raw)
                    .filter { (id, c) -> id in setOf("wy", "tx", "kw", "kg", "mg", "local") && c.type == "music" && "musicUrl" in c.actions }
                require(sources.isNotEmpty()) { "Script has no musicUrl capability" }
                Session(js, sources).block()
            } finally { http.close(); js.close() }
        }
    }

    class Session internal constructor(private val js: QuickJs, val sources: Map<String, LxCapability>) {
        suspend fun musicUrl(candidate: LxCandidate, quality: String): String {
            val key = "result_" + UUID.randomUUID().toString().replace("-", "")
            val request = buildJsonObject {
                put("source", candidate.source); put("action", "musicUrl")
                put("info", buildJsonObject {
                    put("type", if (candidate.source == "local") JsonNull else JsonPrimitive(quality))
                    put("musicInfo", candidate.musicInfo)
                })
            }
            // evaluate() drains native async jobs. The host retains the resolved JS value by key.
            js.evaluate<Any?>("__meiHost.request(${JsonPrimitive(key)}, $request)")
            return js.evaluate<String>("__meiHost.result(${JsonPrimitive(key)})")
        }
    }
}
