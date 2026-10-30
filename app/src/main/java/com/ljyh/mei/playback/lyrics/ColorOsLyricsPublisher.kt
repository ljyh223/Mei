package com.ljyh.mei.playback.lyrics

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import timber.log.Timber

internal const val COLOR_OS_LYRIC_INFO = "lyricInfo"

/** Appends lyrics to the same session that owns the media card, leaving PlaybackState alone. */
internal class ColorOsLyricsPublisher(
    private val session: LyricMetadataSession,
    private val currentMediaId: () -> String?,
) {
    constructor(session: MediaSession, currentMediaId: () -> String?) :
        this(PlatformLyricMetadataSession(session), currentMediaId)

    private var mediaId: String? = null
    private var payload: String? = null
    private var lastPublished: String? = null
    private var closed = false
    private val stopObserving = session.observe { publish() }

    fun setLyrics(mediaId: String?, payload: String?) {
        if (closed) return
        this.mediaId = mediaId
        this.payload = payload
        publish()
    }

    private fun publish() {
        if (closed) return
        try {
            val original = session.metadata ?: return
            val desired = payload.takeIf {
                mediaId != null && currentMediaId() == mediaId &&
                    original.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) == mediaId
            }
            val existing = original.getString(COLOR_OS_LYRIC_INFO)
            if (existing == desired) return
            // Remove only our own previous payload when switching tracks or losing lyrics.
            if (desired == null && (lastPublished == null || existing != lastPublished)) return

            val candidate = copyWithoutLyrics(original).apply {
                if (desired != null) putString(COLOR_OS_LYRIC_INFO, desired)
            }.build()
            if (desired != null && candidate.parcelSize() > 512 * 1024) return
            session.setMetadata(candidate)
            lastPublished = desired
        } catch (error: RuntimeException) {
            // Never log the payload or private media paths, including via exception messages.
            Timber.w("System lyric publication failed (%s)", error.javaClass.simpleName)
        }
    }

    fun close() {
        if (closed) return
        stopObserving()
        setLyrics(null, null)
        closed = true
    }
}

/** The narrow boundary also allows publication/track-race tests without a device media service. */
internal interface LyricMetadataSession {
    val metadata: MediaMetadata?
    fun setMetadata(value: MediaMetadata)
    fun observe(onChanged: () -> Unit): () -> Unit
}

private class PlatformLyricMetadataSession(private val session: MediaSession) : LyricMetadataSession {
    private val controller = session.controller
    override val metadata: MediaMetadata? get() = controller.metadata
    override fun setMetadata(value: MediaMetadata) = session.setMetadata(value)

    override fun observe(onChanged: () -> Unit): () -> Unit {
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = onChanged()
        }
        controller.registerCallback(callback, Handler(Looper.getMainLooper()))
        return { controller.unregisterCallback(callback) }
    }
}

/** Avoid vendor bugs in Builder(existing) while retaining bitmaps, ratings and unknown keys. */
@Suppress("DEPRECATION")
private fun copyWithoutLyrics(metadata: MediaMetadata): MediaMetadata.Builder {
    val parcel = Parcel.obtain()
    val values = try {
        metadata.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        requireNotNull(parcel.readBundle(MediaMetadata::class.java.classLoader))
    } finally {
        parcel.recycle()
    }
    return MediaMetadata.Builder().apply {
        values.keySet().forEach { key ->
            if (key == COLOR_OS_LYRIC_INFO) return@forEach
            when (val value = values.get(key)) {
                is Bitmap -> putBitmap(key, value)
                is Rating -> putRating(key, value)
                is Long -> putLong(key, value)
                is CharSequence -> putText(key, value)
                null -> when (key) {
                    MediaMetadata.METADATA_KEY_ART,
                    MediaMetadata.METADATA_KEY_ALBUM_ART,
                    MediaMetadata.METADATA_KEY_DISPLAY_ICON -> putBitmap(key, null)
                    MediaMetadata.METADATA_KEY_RATING,
                    MediaMetadata.METADATA_KEY_USER_RATING -> putRating(key, null)
                    else -> putText(key, null)
                }
                else -> error("Unsupported metadata type")
            }
        }
    }
}

private fun MediaMetadata.parcelSize(): Int {
    val parcel = Parcel.obtain()
    return try {
        writeToParcel(parcel, 0)
        parcel.dataSize()
    } finally {
        parcel.recycle()
    }
}
