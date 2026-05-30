package com.ljyh.mei.utils


import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.playback.SongDownloadInfo
import com.ljyh.mei.utils.ImageUtils.downloadImageBytes
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.valuepair.ImageFormats
import org.jaudiotagger.tag.images.ArtworkFactory
import timber.log.Timber
import java.io.File

object SongMate {
    suspend fun writeFALC(song: SimplePlaylist.Song, filePath: String) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault as FlacTag
        tag.setField(FieldKey.TITLE, song.name)
        tag.setField(FieldKey.ARTIST, song.artist)
        tag.setField(FieldKey.ALBUM, song.album)
        tag.setField(FieldKey.ALBUM_ARTIST, song.artist)
        tag.setField(FieldKey.LYRICS, song.lyric)

        //cover

        tag.setField(
            tag.createArtworkField(
                downloadImageBytes(song.picUrl),
                6,
                ImageFormats.MIME_TYPE_JPEG,
                "Image",
                1400,
                1400,
                24,
                0
            )
        )
        audioFile.commit()
    }


    suspend fun writeMP3(song: SimplePlaylist.Song, filePath: String) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, song.name)
        tag.setField(FieldKey.ARTIST, song.artist)
        tag.setField(FieldKey.ALBUM, song.album)
        tag.setField(FieldKey.ALBUM_ARTIST, song.artist)
        tag.setField(FieldKey.LYRICS, song.lyric)
        val artwork = ArtworkFactory.getNew()
        artwork.mimeType = "image/jpeg"
        artwork.binaryData = downloadImageBytes(song.picUrl)
        artwork.pictureType = 6
        artwork.description = "Cover"
        tag.setField(artwork)
        audioFile.commit()


    }


    suspend fun writeFLACFromDownload(info: SongDownloadInfo, filePath: String) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault as FlacTag
        tag.setField(FieldKey.TITLE, info.songTitle)
        tag.setField(FieldKey.ARTIST, info.songArtist.joinToString(","))
        tag.setField(FieldKey.ALBUM, info.songAlbum)
        tag.setField(FieldKey.ALBUM_ARTIST, info.songArtist.joinToString(","))
        if (info.songCover.isNotBlank()) {
            tag.setField(
                tag.createArtworkField(
                    downloadImageBytes(info.songCover),
                    6,
                    ImageFormats.MIME_TYPE_JPEG,
                    "Image",
                    1400,
                    1400,
                    24,
                    0
                )
            )
        }
        audioFile.commit()
    }

    suspend fun writeMP3FromDownload(info: SongDownloadInfo, filePath: String) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, info.songTitle)
        tag.setField(FieldKey.ARTIST, info.songArtist.joinToString(","))
        tag.setField(FieldKey.ALBUM, info.songAlbum)
        tag.setField(FieldKey.ALBUM_ARTIST, info.songArtist.joinToString(","))
        if (info.songCover.isNotBlank()) {
            val artwork = ArtworkFactory.getNew()
            artwork.mimeType = "image/jpeg"
            artwork.binaryData = downloadImageBytes(info.songCover)
            artwork.pictureType = 6
            artwork.description = "Cover"
            tag.setField(artwork)
        }
        audioFile.commit()
    }

    suspend fun writeTags(
        title: String,
        artist: String,
        album: String,
        coverUrl: String,
        filePath: String
    ) {
        val suffix = filePath.substringAfterLast(".").lowercase()
        try {
            when (suffix) {
                "flac" -> writeFlacTags(title, artist, album, coverUrl, filePath)
                "mp3" -> writeMp3Tags(title, artist, album, coverUrl, filePath)
                else -> {
                    try {
                        val file = File(filePath)
                        val audioFile: AudioFile = AudioFileIO.read(file)
                        val tag = audioFile.tagOrCreateAndSetDefault
                        tag.setField(FieldKey.TITLE, title)
                        tag.setField(FieldKey.ARTIST, artist)
                        tag.setField(FieldKey.ALBUM, album)
                        tag.setField(FieldKey.ALBUM_ARTIST, artist)
                        val imageBytes = downloadImageBytes(coverUrl)
                        if (coverUrl.isNotBlank() && imageBytes != null) {
                            tag.deleteArtworkField()
                            val artwork = ArtworkFactory.getNew()
                            artwork.mimeType = "image/jpeg"
                            artwork.binaryData = imageBytes
                            artwork.pictureType = 6
                            artwork.description = "Cover"
                            tag.setField(artwork)
                        }
                        audioFile.commit()
                    } catch (e: Exception) {
                        Timber.e(e, "writeTags fallback failed for $filePath")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "writeTags error for $filePath")
        }
    }

    private suspend fun writeFlacTags(
        title: String, artist: String, album: String,
        coverUrl: String, filePath: String
    ) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault as FlacTag
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)
        tag.setField(FieldKey.ALBUM_ARTIST, artist)
        if (coverUrl.isNotBlank()) {
            val imageBytes = downloadImageBytes(coverUrl)
            if (imageBytes != null) {
                tag.setField(
                    tag.createArtworkField(
                        imageBytes,
                        6, ImageFormats.MIME_TYPE_JPEG, "Image",
                        1400, 1400, 24, 0
                    )
                )
            }
        }
        audioFile.commit()
    }

    private suspend fun writeMp3Tags(
        title: String, artist: String, album: String,
        coverUrl: String, filePath: String
    ) {
        val file = File(filePath)
        val audioFile: AudioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)
        tag.setField(FieldKey.ALBUM_ARTIST, artist)
        if (coverUrl.isNotBlank()) {
            val imageBytes = downloadImageBytes(coverUrl)
            if (imageBytes != null) {
                tag.deleteArtworkField()
                val artwork = ArtworkFactory.getNew()
                artwork.mimeType = "image/jpeg"
                artwork.binaryData = imageBytes
                artwork.pictureType = 6
                artwork.description = "Cover"
                tag.setField(artwork)
            }
        }
        audioFile.commit()
    }

    fun writeLyric(filePath: String, lyric: String): Boolean {
        val file = File(filePath)
        return when (filePath.substringAfterLast(".")) {

            "mp3" -> {
                println("mp3")
                val audioFile: AudioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault
                // utf-16

                tag.setField(FieldKey.LYRICS, lyric)
                audioFile.commit()
                true
            }

            "flac" -> {
                println("flac")
                val audioFile: AudioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault as FlacTag
                tag.setField(FieldKey.LYRICS, lyric)
                audioFile.commit()
                true
            }

            else -> false
        }


    }
}