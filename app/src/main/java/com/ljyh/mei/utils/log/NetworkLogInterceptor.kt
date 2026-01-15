package com.ljyh.mei.utils.log

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class NetworkLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        try {
            val response = chain.proceed(request)

            // 如果响应不成功 (比如 404, 500, 或者网易云特有的 301/403)
            if (!response.isSuccessful) {
                Timber.tag("API_ERROR").e("Request failed: $url | Code: ${response.code}")
            }
            return response
        } catch (e: Exception) {
            // 捕获网络异常（断网、超时、DNS解析失败）
            Timber.tag("API_FAIL").e(e, "Network Error: $url")
            throw e
        }
    }
}