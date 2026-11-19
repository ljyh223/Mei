package com.ljyh.mei.playback.source.musicfree

import com.ljyh.mei.playback.source.lx.LxScriptImportException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class MusicFreeImportException(message: String) : IOException(message)

data class MusicFreeSubscriptionEntry(val name: String, val url: String, val version: String = "")
data class MusicFreeSubscription(val description: String, val plugins: List<MusicFreeSubscriptionEntry>)
sealed interface MusicFreeImportDocument {
    data class Script(val bytes: ByteArray) : MusicFreeImportDocument
    data class Subscription(val manifest: MusicFreeSubscription) : MusicFreeImportDocument
}
data class MusicFreeImportFailure(val entry: MusicFreeSubscriptionEntry, val reason: String)
data class MusicFreeBatchImportResult(
    val imported: List<MusicFreeSubscriptionEntry> = emptyList(),
    val failures: List<MusicFreeImportFailure> = emptyList(),
)

internal fun musicFreeImportDocument(bytes: ByteArray, baseUrl: String): MusicFreeImportDocument {
    val text = try {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF").trimStart()
    } catch (_: Exception) { throw MusicFreeImportException("文件不是有效的 UTF-8 文本") }
    if (!text.startsWith("{") && !text.startsWith("[")) return MusicFreeImportDocument.Script(bytes)
    val root = try { musicFreeJson.parseToJsonElement(text) as? JsonObject }
        catch (_: Exception) { null }
        ?: throw MusicFreeImportException("订阅格式无效，请使用 MusicFree 订阅链接")
    val entries = root["plugins"] as? JsonArray
        ?: throw MusicFreeImportException("订阅中缺少插件列表，请检查链接")
    if (entries.isEmpty() || entries.size > 100) throw MusicFreeImportException("订阅需包含 1 至 100 个插件")
    val base = baseUrl.toHttpUrlOrNull() ?: throw MusicFreeImportException("订阅地址无效")
    val plugins = entries.map { element ->
        val entry = element as? JsonObject ?: throw MusicFreeImportException("订阅中的插件信息无效")
        val rawUrl = entry.text("url").trim()
        val url = rawUrl.takeIf { it.isNotEmpty() && it.length <= 8192 }?.let(base::resolve)
        if (url == null || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw MusicFreeImportException("订阅包含无效的插件下载地址")
        }
        MusicFreeSubscriptionEntry(
            name = entry.text("name").trim().take(200).ifBlank { "未命名插件" },
            url = url.toString(), version = entry.text("version").take(80),
        )
    }.distinctBy { it.url }
    return MusicFreeImportDocument.Subscription(MusicFreeSubscription(root.text("desc").take(2000), plugins))
}

/** Display only our own actionable import errors; script exceptions may contain private URLs. */
internal fun musicFreeImportError(error: Exception): String = when (error) {
    is TimeoutCancellationException -> "下载或初始化超时，请重试"
    is MusicFreeImportException, is LxScriptImportException -> error.message ?: "导入失败，请重试"
    else -> "插件初始化失败，请检查插件兼容性、配置与网络"
}
