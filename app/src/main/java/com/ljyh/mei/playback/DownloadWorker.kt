package com.ljyh.mei.playback

import android.content.Context
import androidx.work.WorkerParameters

/** Keeps already queued WorkManager jobs from older app versions runnable. */
@Deprecated("Use com.ljyh.mei.download.DownloadWorker for new work")
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : com.ljyh.mei.download.DownloadWorker(context, params)
