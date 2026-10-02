package com.ljyh.mei.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import timber.log.Timber

@OptIn(UnstableApi::class)
class MusicLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception

        // 明确是资源找不到 / 断网 / 404，立即失败，不要重试
        if (exception is SourceNotFoundException ||
            exception is java.net.UnknownHostException ||
            exception is java.net.ConnectException ||
            exception is java.net.SocketTimeoutException ||
            (exception is HttpDataSource.InvalidResponseCodeException && exception.responseCode == 404)
        ) {
            return C.TIME_UNSET // 不重试
        }

        Timber.tag("MusicLoadErrorHandlingPolicy").d(loadErrorInfo.toString())
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}