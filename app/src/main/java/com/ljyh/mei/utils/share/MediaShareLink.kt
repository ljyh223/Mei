package com.ljyh.mei.utils.share

import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata

/** Public catalogue links only: never share playback URLs, local paths or account credentials. */
@ConsistentCopyVisibility
data class MediaShareLink private constructor(
    val kind: Kind,
    val id: Long,
    val title: String,
    val subtitle: String,
) {
    enum class Kind(val label: String, val path: String) {
        Song("歌曲", "song"), Playlist("歌单", "playlist"), Album("专辑", "album"),
    }

    val url: String get() = "https://music.163.com/${kind.path}?id=$id"
    val text: String get() = buildString {
        append("分享").append(kind.label).append("《").append(title).append("》")
        if (subtitle.isNotBlank()) append(" - ").append(subtitle)
        append('\n').append(url)
    }

    companion object {
        fun song(metadata: MediaMetadata): MediaShareLink? = create(
            kind = Kind.Song,
            id = metadata.neteaseId,
            title = metadata.title,
            subtitle = metadata.artists.map { it.name.trim() }.filter(String::isNotBlank)
                .distinct().joinToString(" / "),
        )

        fun playlist(id: Long, title: String, creator: String): MediaShareLink? =
            create(Kind.Playlist, id, title, creator)

        fun album(album: AlbumOverview): MediaShareLink? = create(
            Kind.Album, album.id, album.name,
            album.artistNames.map(String::trim).filter(String::isNotBlank).distinct().joinToString(" / "),
        )

        private fun create(kind: Kind, id: Long?, title: String, subtitle: String): MediaShareLink? {
            if (id == null || id <= 0) return null
            return MediaShareLink(kind, id, title.singleLine().ifBlank { kind.label }, subtitle.singleLine())
        }
    }
}

private fun String.singleLine() = replace(Regex("[\\r\\n\\t]+"), " ").trim()
