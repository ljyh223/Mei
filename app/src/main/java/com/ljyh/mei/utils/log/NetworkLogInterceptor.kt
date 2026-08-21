package com.ljyh.mei.utils.log

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class NetworkLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        return try {
            val response = chain.proceed(request)

            // 如果响应不成功（比如 404, 500 等）
            if (!response.isSuccessful) {
                Timber.tag("API_ERROR").e("Request failed: $url | Code: ${response.code}")
            }

            response
        } catch (e: Exception) {
            // 捕获网络异常（断网、超时、DNS解析失败、SSL握手失败等）
            Timber.tag("API_FAIL").e(e, "Network Error: $url")

            // 将所有非 IOException 包装为 IOException 重新抛出
            // 确保 OkHttp 规范捕获，并能顺利传递给上层的 Repository/ViewModel 进行 try-catch 处理
            if (e is IOException) {
                throw e
            } else {
                throw IOException("Network request failed due to: ${e.message}", e)
            }
        }
    }
}
