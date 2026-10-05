package com.ljyh.mei.audio.match

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WasmAudioFingerprinter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun generate(samples: FloatArray): String {
        require(samples.size == MatchSampleCount) { "指纹需要 $MatchSampleCount 个样本" }
        val encodedSamples = withContext(Dispatchers.Default) {
            val bytes = ByteBuffer.allocate(samples.size * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply { asFloatBuffer().put(samples) }
                .array()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        return withContext(Dispatchers.Main.immediate) {
            val webView = WebView(context)
            try {
                withTimeout(30_000) {
                    suspendCancellableCoroutine { continuation ->
                        val bridge = FingerprintBridge(
                            onResult = { fingerprint ->
                                if (continuation.isActive) continuation.resume(fingerprint)
                            },
                            onError = { message ->
                                if (continuation.isActive) {
                                    continuation.resumeWithException(IOException(message))
                                }
                            },
                        )
                        webView.settings.javaScriptEnabled = true
                        webView.settings.allowFileAccess = true
                        webView.settings.blockNetworkLoads = true
                        webView.addJavascriptInterface(bridge, "MeiFingerprint")
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                if (!continuation.isActive) return
                                view.evaluateJavascript(
                                    "window.generateMeiFingerprint(${JSONObject.quote(encodedSamples)})",
                                    null,
                                )
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                val asset = request.url.toString()
                                val requiredAsset = asset.endsWith("/afp.js") ||
                                    asset.endsWith("/afp.wasm.js")
                                if (continuation.isActive && (request.isForMainFrame || requiredAsset)) {
                                    continuation.resumeWithException(
                                        IOException("指纹资源加载失败：${error.description}"),
                                    )
                                }
                            }
                        }
                        webView.loadUrl("file:///android_asset/audio_match/index.html")
                    }
                }
            } finally {
                webView.stopLoading()
                webView.removeJavascriptInterface("MeiFingerprint")
                webView.destroy()
            }
        }
    }

    private class FingerprintBridge(
        private val onResult: (String) -> Unit,
        private val onError: (String) -> Unit,
    ) {
        @JavascriptInterface
        fun onResult(fingerprint: String) = onResult.invoke(fingerprint)

        @JavascriptInterface
        fun onError(message: String) = onError.invoke(message)
    }
}
