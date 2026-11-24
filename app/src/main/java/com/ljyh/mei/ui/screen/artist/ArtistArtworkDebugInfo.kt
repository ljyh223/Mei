package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork

internal enum class ArtistArtworkLoadState { Unavailable, Loading, Success, Error }

/** Rendering state, rather than resource availability, determines the displayed source. */
internal data class ArtistArtworkPresentation(
    val photo: ArtistArtworkLoadState = ArtistArtworkLoadState.Unavailable,
    val wordmark: ArtistArtworkLoadState = ArtistArtworkLoadState.Unavailable,
    val background: String? = null,
    val tabletPhotoKind: TabletArtistPhotoKind? = null,
) {
    val usesApplePhoto: Boolean get() = photo == ArtistArtworkLoadState.Success
}

/** Only mounted by the layouts when the existing Debug preference is enabled. */
@Composable
internal fun ArtistArtworkDebugInfo(
    artwork: AppleArtistArtwork?,
    presentation: ArtistArtworkPresentation,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("图片来源 · 调试", style = MaterialTheme.typography.labelMedium)
            Text(if (!enabled) "人像：网易云（Apple Music 歌手人像已关闭）" else when (presentation.photo) {
                ArtistArtworkLoadState.Unavailable -> "人像：网易云（AM 资源未就绪）"
                ArtistArtworkLoadState.Loading -> "人像：网易云（AM 图片加载中）"
                ArtistArtworkLoadState.Error -> "人像：网易云（AM 图片加载失败）"
                ArtistArtworkLoadState.Success -> "人像：Apple Music"
            }, style = MaterialTheme.typography.bodySmall)
            Text(if (!enabled || !presentation.usesApplePhoto) "名字：普通文字（网易云头部）" else when (presentation.wordmark) {
                ArtistArtworkLoadState.Unavailable -> "名字：普通文字（AM 未提供艺术字）"
                ArtistArtworkLoadState.Loading -> "名字：普通文字（AM 艺术字加载中）"
                ArtistArtworkLoadState.Error -> "名字：普通文字（AM 艺术字加载失败）"
                ArtistArtworkLoadState.Success -> "名字：AM 艺术字"
            }, style = MaterialTheme.typography.bodySmall)
            if (enabled && presentation.usesApplePhoto && presentation.tabletPhotoKind != null) Text(
                when (presentation.tabletPhotoKind) {
                    TabletArtistPhotoKind.Hero -> "背景：AM 宽幅图"
                    TabletArtistPhotoKind.MotionVideo -> "背景：AM 动态视频"
                    TabletArtistPhotoKind.MotionPreview -> "背景：AM 动态资源预览图"
                    TabletArtistPhotoKind.Portrait -> "背景：柔化人像；前景：AM 圆形头像"
                }, style = MaterialTheme.typography.bodySmall)
            if (enabled && artwork != null) Text("AM 匹配：${artwork.name} · ${artwork.id}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
