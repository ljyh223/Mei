package com.ljyh.mei.ui.screen.artist.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.screen.artist.ArtistDetailActions

/** A non-scrolling row in the page's single feed. Song indices always refer to the full queue. */
@Composable
internal fun TabletArtistSongRow(
    songs: List<IndexedValue<MediaMetadata>>,
    columns: Int,
    actions: ArtistDetailActions,
    loading: Boolean = false,
) {
    Row {
        repeat(columns) { column ->
            val entry = songs.getOrNull(column)
            if (entry != null) key(entry.value.id) {
                Box(Modifier.weight(1f)) {
                    val song = entry.value
                    Track(song, subtitle = song.album.title.takeIf(String::isNotBlank)?.let(::AnnotatedString),
                        onClick = { actions.onTrackClick(song, entry.index) },
                        onMoreClick = { actions.onTrackMore(song, it) },
                        moreContentDescription = "${song.title}的更多操作")
                }
            } else if (loading) Box(Modifier.weight(1f)) { TrackPlaceholder(column) }
            else Spacer(Modifier.weight(1f))
        }
    }
}
