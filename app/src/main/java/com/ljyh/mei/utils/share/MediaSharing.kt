package com.ljyh.mei.utils.share

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

internal fun Context.mediaShareChooser(content: MediaShareLink): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, content.title)
        putExtra(Intent.EXTRA_SUBJECT, content.title)
        putExtra(Intent.EXTRA_TEXT, content.text)
    }
    return Intent.createChooser(send, "分享${content.kind.label}").apply {
        var owner = this@mediaShareChooser
        while (owner is ContextWrapper && owner !is Activity && owner.baseContext !== owner) {
            owner = owner.baseContext
        }
        if (owner !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** Opening the chooser never sends anything until the user chooses a destination. */
fun Context.shareMediaLink(content: MediaShareLink?): Boolean {
    if (content == null) {
        Toast.makeText(this, "此内容暂无可分享的公开链接", Toast.LENGTH_SHORT).show()
        return false
    }
    return try {
        startActivity(mediaShareChooser(content))
        true
    } catch (error: RuntimeException) {
        Timber.w(error, "Unable to open media share chooser")
        Toast.makeText(this, "无法打开系统分享，请稍后重试", Toast.LENGTH_SHORT).show()
        false
    }
}
