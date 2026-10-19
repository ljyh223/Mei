package com.ljyh.mei.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Fetches the Watchman v2 token used by playlist subscription requests. */
internal class WatchmanTokenProvider(private val context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun getToken(): String = withContext(Dispatchers.Main.immediate) {
        val webView = WebView(context)
        try {
            withTimeout(25.seconds) {
                suspendCancellableCoroutine { continuation ->
                    val bridge = TokenBridge(
                        onToken = { token ->
                            if (!continuation.isActive) return@TokenBridge
                            if (token.length < 100) {
                                continuation.resumeWithException(IOException("Watchman v2 返回无效 Token"))
                            } else {
                                continuation.resume(token)
                            }
                        },
                        onError = { message ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("Watchman v2 获取失败：$message"))
                            }
                        },
                    )
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.addJavascriptInterface(bridge, "MeiWatchman")
                    webView.webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            val required = request.isForMainFrame ||
                                request.url.toString().contains("/tool.min.js")
                            if (required && continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException("Watchman v2 资源加载失败：${error.description}"),
                                )
                            }
                        }
                    }
                    webView.loadDataWithBaseURL(
                        "https://music.163.com/",
                        WATCHMAN_PAGE,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            }
        } finally {
            webView.stopLoading()
            webView.removeJavascriptInterface("MeiWatchman")
            webView.destroy()
        }
    }

    private class TokenBridge(
        private val onToken: (String) -> Unit,
        private val onError: (String) -> Unit,
    ) {
        @JavascriptInterface
        fun onToken(token: String) = onToken.invoke(token)

        @JavascriptInterface
        fun onError(message: String) = onError.invoke(message)
    }
}

private val WATCHMAN_PAGE = """
    <!doctype html><html><head><meta charset="utf-8"></head><body>
    <script src="https://acstatic-dun.126.net/tool.min.js"></script>
    <script>
    (function () {
      var finished = false;
      var timer = setTimeout(function () { fail('初始化超时'); }, 20000);
      function fail(message) {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        MeiWatchman.onError(String(message));
      }
      function done(token) {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        MeiWatchman.onToken(typeof token === 'string' ? token : '');
      }
      try {
        initWatchman({
          auto: true,
          productNumber: 'YD00000558929251',
          onload: function (watchman) {
            try {
              var requestToken = function () {
                watchman.getToken('bd5d2f973ef74cd2a61325a412ae54d9', done);
              };
              var instance = watchman.getInstance();
              if (instance && typeof instance.I === 'function') instance.I(requestToken);
              else requestToken();
            } catch (error) { fail(error); }
          },
          onerror: function () { fail('SDK 初始化失败'); }
        });
      } catch (error) { fail(error); }
    })();
    </script></body></html>
""".trimIndent()
