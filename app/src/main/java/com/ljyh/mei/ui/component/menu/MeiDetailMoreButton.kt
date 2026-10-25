package com.ljyh.mei.ui.component.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.share.MediaShareLink
import com.ljyh.mei.utils.share.shareMediaLink
import com.ljyh.mei.utils.system.setClipboard

/** One floating toolbar action for collection search and public link sharing. */
@Composable
fun MeiDetailMoreButton(
    shareContent: MediaShareLink?,
    modifier: Modifier = Modifier,
    searchActive: Boolean = false,
    searchLabel: String = "页内搜索",
    onSearchActiveChange: ((Boolean) -> Unit)? = null,
    playlistCoverUrl: String? = null,
) {
    val context = LocalContext.current
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    var expanded by remember(shareContent?.kind, shareContent?.id, searchActive) { mutableStateOf(false) }
    if (searchActive && onSearchActiveChange != null) {
        MeiGlassIconButton(modifier = modifier, onClick = { onSearchActiveChange(false) }) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭搜索")
        }
        return
    }
    if (shareContent == null && onSearchActiveChange == null) return
    Box(modifier) {
        MeiGlassIconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 184.dp, max = 280.dp),
        ) {
            if (onSearchActiveChange != null) {
                MeiMenuItem(searchLabel, Icons.Rounded.Search) {
                    expanded = false
                    onSearchActiveChange(true)
                }
            }
            if (shareContent != null) {
                if (onSearchActiveChange != null) HorizontalDivider()
                MeiMenuItem("分享链接", Icons.Rounded.Share) {
                    expanded = false
                    context.shareMediaLink(shareContent)
                }
                if (shareContent.kind != MediaShareLink.Kind.Playlist) {
                    MeiMenuItem("复制链接", Icons.Rounded.ContentCopy) {
                        expanded = false
                        setClipboard(context, shareContent.url, "${shareContent.kind.label}链接")
                    }
                } else if (debugEnabled) {
                    HorizontalDivider()
                    MeiMenuItem("复制歌单 ID", Icons.Rounded.ContentCopy) {
                        expanded = false
                        setClipboard(context, shareContent.id.toString(), "歌单 ID")
                    }
                    playlistCoverUrl?.takeIf(String::isNotBlank)?.let { coverUrl ->
                        MeiMenuItem("复制歌单封面 URL", Icons.Rounded.ContentCopy) {
                            expanded = false
                            setClipboard(context, coverUrl, "歌单封面 URL")
                        }
                    }
                }
            }
        }
    }
}
