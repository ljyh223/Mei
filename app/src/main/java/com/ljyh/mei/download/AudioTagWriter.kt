package com.ljyh.mei.download

import com.ljyh.mei.utils.image.CoverImageDownloader.downloadImageBytes
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.ArtworkFactory
import timber.log.Timber
import java.io.File

object AudioTagWriter {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
    )

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
            Timber.tag("AudioTagWriter").w(e, "checkTags failed for $filePath")
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
    ): TagStatus? {
        val coverBytes = if (coverUrl.isNotBlank()) downloadImageBytes(coverUrl) else null
        return writeTagsWithCoverBytes(title, artist, album, coverBytes, filePath, lyric)
    }

    suspend fun writeTagsWithCoverBytes(
        title: String,
        artist: String,
        album: String,
        coverBytes: ByteArray?,
        filePath: String,
        lyric: String? = null
    ): TagStatus? {
        return try {
            val file = File(filePath)
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            val isFlac = tag is FlacTag

            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.ALBUM_ARTIST, artist)

            if (!lyric.isNullOrBlank()) {
                try {
                    tag.setField(FieldKey.LYRICS, lyric)
                } catch (e: Exception) {
                    Timber.tag("AudioTagWriter").w(e, "Failed to write lyric for $title")
                }
            }

            if (coverBytes != null) {
                try {
                    val coverMimeType = when {
                        coverBytes.size >= pngSignature.size &&
                            coverBytes.copyOfRange(0, pngSignature.size).contentEquals(pngSignature) -> "image/png"
                        coverBytes.size >= 3 &&
                            coverBytes[0] == 0xff.toByte() &&
                            coverBytes[1] == 0xd8.toByte() &&
                            coverBytes[2] == 0xff.toByte() -> "image/jpeg"
                        else -> error("Unsupported cover image format")
                    }
                    val dimensions = if (isFlac) {
                        imageDimensions(coverBytes, coverMimeType)
                            ?: error("Cannot read cover image dimensions")
                    } else null
                    tag.deleteArtworkField()
                    if (isFlac) {
                        val (width, height) = requireNotNull(dimensions)
                        tag.setField(
                            tag.createArtworkField(
                                coverBytes, 3,
                                coverMimeType, "Cover",
                                width, height, 24, 0
                            )
                        )
                    } else {
                        val artwork = ArtworkFactory.getNew()
                        artwork.mimeType = coverMimeType
                        artwork.binaryData = coverBytes
                        artwork.pictureType = 3
                        artwork.description = "Cover"
                        tag.setField(artwork)
                    }
                } catch (e: Exception) {
                    Timber.tag("AudioTagWriter").w(e, "Failed to write cover for $title, skipping")
                }
            }

            audioFile.commit()
            checkTags(filePath)
        } catch (e: Exception) {
            Timber.tag("AudioTagWriter").e(e, "writeTags failed for $filePath")
            null
        }
    }

    private fun imageDimensions(bytes: ByteArray, mimeType: String): Pair<Int, Int>? {
        fun uint16(offset: Int): Int =
            ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

        if (mimeType == "image/png") {
            if (bytes.size < 24) return null
            fun uint32(offset: Int): Int =
                (uint16(offset) shl 16) or uint16(offset + 2)
            return (uint32(16) to uint32(20)).takeIf { it.first > 0 && it.second > 0 }
        }

        // JPEG dimensions are stored in the first start-of-frame marker.
        var offset = 2
        while (offset + 9 < bytes.size) {
            if (bytes[offset] != 0xff.toByte()) {
                offset++
                continue
            }
            val marker = bytes[offset + 1].toInt() and 0xff
            if (marker == 0xff) {
                offset++
                continue
            }
            if (marker == 0xd9 || marker == 0xda) break
            if (marker == 0xd8 || marker == 0x01 || marker in 0xd0..0xd7) {
                offset += 2
                continue
            }
            val length = uint16(offset + 2)
            if (length < 2 || offset + 2 + length > bytes.size) break
            if (marker in listOf(
                    0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7,
                    0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf,
                ) && length >= 7
            ) {
                val height = uint16(offset + 5)
                val width = uint16(offset + 7)
                return (width to height).takeIf { it.first > 0 && it.second > 0 }
            }
            offset += 2 + length
        }
        return null
    }
}
