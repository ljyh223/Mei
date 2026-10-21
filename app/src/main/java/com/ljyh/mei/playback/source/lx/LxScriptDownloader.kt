package com.ljyh.mei.playback.source.lx

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LxScriptImportException(message: String) : IOException(message)
internal data class DownloadedLxScript(val bytes: ByteArray, val filename: String)

/** Imports a snapshot. No account cookies, URL persistence, or automatic script updates. */
@Singleton
class LxScriptDownloader @Inject constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS).callTimeout(12, TimeUnit.SECONDS).build()

    internal suspend fun download(input: String): DownloadedLxScript {
        val text = input.trim()
        val url = text.takeIf { it.length <= 8192 }?.toHttpUrlOrNull()
            ?: throw LxScriptImportException("请输入有效的 http:// 或 https:// 链接")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw LxScriptImportException("请使用不含用户名和密码的脚本链接")
        }
        val call = client.newCall(Request.Builder().url(url)
            .header("Accept", "application/javascript, text/javascript, text/plain, */*").build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(
                        LxScriptImportException("下载失败，请检查网络或稍后重试")
                    )
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val downloaded = response.use {
                            if (!it.isSuccessful) throw LxScriptImportException("下载失败（HTTP ${it.code}），请检查链接")
                            val max = LxScriptStore.MAX_SCRIPT_BYTES
                            if (it.body.contentLength() > max) throw LxScriptImportException("脚本不能超过 1 MB")
                            val bytes = it.body.byteStream().readLimited(max + 1)
                            if (bytes.size > max) throw LxScriptImportException("脚本不能超过 1 MB")
                            if (bytes.isEmpty()) throw LxScriptImportException("链接返回了空文件")
                            val type = it.body.contentType()?.toString().orEmpty()
                            val prefix = bytes.take(512).toByteArray().toString(Charsets.UTF_8).removePrefix("\uFEFF").trimStart()
                            if (type.contains("html", true) || type.contains("json", true) ||
                                Regex("^<(?:!doctype\\s+html|html\\b|head\\b|body\\b)", RegexOption.IGNORE_CASE).containsMatchIn(prefix)) {
                                throw LxScriptImportException("链接返回了网页或接口数据，请使用 JS 文件的直链")
                            }
                            val filename = it.request.url.pathSegments.lastOrNull()?.takeIf { name -> name.endsWith(".js", true) }
                                ?: "网络音源.js"
                            DownloadedLxScript(bytes, filename)
                        }
                        if (continuation.isActive) continuation.resume(downloaded)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(
                            if (e is LxScriptImportException) e else LxScriptImportException("下载未完成，请检查网络后重试")
                        )
                    }
                }
            })
        }
    }
}
