package com.ljyh.mei.utils

import com.ljyh.mei.utils.ImageUtils.downloadImageBytes
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.valuepair.ImageFormats
import org.jaudiotagger.tag.images.ArtworkFactory
import timber.log.Timber
import java.io.File

object SongMate {

    data class TagStatus(
        val hasTitle: Boolean,
        val hasArtist: Boolean,
        val hasAlbum: Boolean,
        val hasCover: Boolean,
        val hasLyric: Boolean
    ) {
        val isComplete get() = hasTitle && hasArtist && hasAlbum && hasCover && hasLyric
        val isBasicComplete get() = hasTitle && hasArtist && hasAlbum && hasCover
    }

    fun checkTags(filePath: String): TagStatus? {
        return try {
            val file = File(filePath)
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            TagStatus(
                hasTitle = !tag.getFirst(FieldKey.TITLE).isNullOrBlank(),
                hasArtist = !tag.getFirst(FieldKey.ARTIST).isNullOrBlank(),
                hasAlbum = !tag.getFirst(FieldKey.ALBUM).isNullOrBlank(),
                hasCover = tag.firstArtwork != null,
                hasLyric = !tag.getFirst(FieldKey.LYRICS).isNullOrBlank()
            )
        } catch (e: Exception) {
            Timber.tag("SongMate").w(e, "checkTags failed for $filePath")
            null
        }
    }

    suspend fun writeTags(
        title: String,
        artist: String,
        album: String,
        coverUrl: String,
        filePath: String,
        lyric: String? = null
    ) {
        val coverBytes = if (coverUrl.isNotBlank()) downloadImageBytes(coverUrl) else null
        writeTagsWithCoverBytes(title, artist, album, coverBytes, filePath, lyric)
    }

    suspend fun writeTagsWithCoverBytes(
        title: String,
        artist: String,
        album: String,
        coverBytes: ByteArray?,
        filePath: String,
        lyric: String? = null
    ) {
        try {
            val file = File(filePath)
            val audioFile = AudioFileIO.read(file)
            var tag = audioFile.tagOrCreateAndSetDefault
            val isFlac = tag is FlacTag

            if (!isFlac && tag !is ID3v24Tag) {
                val oldArtwork = if (tag is AbstractID3v2Tag) {
                    tag.firstArtwork
                } else null
                val v24Tag = ID3v24Tag()
                oldArtwork?.let { v24Tag.setField(it) }
                audioFile.tag = v24Tag
                tag = v24Tag
            }

            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.ALBUM_ARTIST, artist)

            if (!lyric.isNullOrBlank()) {
                try {
                    tag.setField(FieldKey.LYRICS, lyric)
                } catch (e: Exception) {
                    Timber.tag("SongMate").w(e, "Failed to write lyric for $title")
                }
            }

            if (coverBytes != null) {
                try {
                    tag.deleteArtworkField()
                    if (isFlac) {
                        tag.setField(
                            (tag as FlacTag).createArtworkField(
                                coverBytes, 6,
                                ImageFormats.MIME_TYPE_JPEG, "Image",
                                1400, 1400, 24, 0
                            )
                        )
                    } else {
                        val artwork = ArtworkFactory.getNew()
                        artwork.mimeType = "image/jpeg"
                        artwork.binaryData = coverBytes
                        artwork.pictureType = 6
                        artwork.description = "Cover"
                        tag.setField(artwork)
                    }
                } catch (e: Exception) {
                    Timber.tag("SongMate").w(e, "Failed to write cover for $title, skipping")
                }
            }

            audioFile.commit()
        } catch (e: Exception) {
            Timber.tag("SongMate").e(e, "writeTags failed for $filePath")
        }
    }
}
