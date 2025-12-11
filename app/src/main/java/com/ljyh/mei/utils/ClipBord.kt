package com.ljyh.mei.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun setClipboard(context: Context, text: String, label: String) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            label,
            text
        )
    )
    Toast.makeText(context, "$label 已复制", Toast.LENGTH_SHORT).show()
}