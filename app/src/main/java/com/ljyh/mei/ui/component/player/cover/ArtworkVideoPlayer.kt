package com.ljyh.mei.ui.component.player.cover

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.ljyh.mei.playback.source.CacheManager

/** Decorative video uses the existing media cache, with no audio track or audio focus. */
@OptIn(UnstableApi::class)
internal fun createArtworkVideoPlayer(
    context: Context,
    url: String,
    cacheKey: String,
    sourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(CacheManager.getCacheDataSourceFactory(context)),
): ExoPlayer = ExoPlayer.Builder(context.applicationContext)
    .setMediaSourceFactory(sourceFactory)
    .setAudioAttributes(AudioAttributes.DEFAULT, false)
    .build().apply {
        volume = 0f
        repeatMode = Player.REPEAT_MODE_ONE
        trackSelectionParameters = trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        setMediaItem(MediaItem.Builder().setUri(url).apply {
            if (url.contains(".m3u8", ignoreCase = true)) setMimeType(MimeTypes.APPLICATION_M3U8)
            else setCustomCacheKey(cacheKey)
        }.build())
    }
