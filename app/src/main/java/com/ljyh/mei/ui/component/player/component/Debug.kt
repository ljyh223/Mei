package com.ljyh.mei.ui.component.player.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun Debug(
    id: String,
    title: String,
    album: String,
    artist: String,
    duration: String,
    qid: String,
    color: String,
    modifier: Modifier
) {
    val context= LocalContext.current
    Card {
        Column(
            modifier=modifier
        ) {
            Text(text = "ID :$id")
            Text(text = "TITLE: $title")
            Text(text = "ALBUM: $album")
            Text(text = "ARTIST: $artist")
            Text(text = "DURATION: $duration")
            Text(text = "QQ_ID: $qid")
            Text(text = "COLOR: $color")
//            Button(
//                onClick = {
//                    val text = "ID :$id\nTITLE: $title\nALBUM: $album\nARTIST: $artist\nDURATION: $duration\nQQ_ID: $qid"
//                    val clipboard =
//                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                    val clip = ClipData.newPlainText("info", text)
//                    clipboard.setPrimaryClip(clip)
//
//                }
//            ) {
//                Text(text="Copy")
//            }
        }
    }

}