package com.ljyh.mei.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

@OptIn(UnstableApi::class)
class MusicLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception

        // 如果是我们自定义的 SourceNotFoundException，或者 404 等错误，直接不重试，立即报错
        if (exception is SourceNotFoundException ||
            (exception is HttpDataSource.InvalidResponseCodeException && exception.responseCode == 404)) {
            return C.TIME_UNSET // 不重试
        }

        // 其他网络错误，使用默认的指数退避重试
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}