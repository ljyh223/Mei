package com.ljyh.music.utils


import com.ljyh.music.data.model.SimplePlaylist
import com.ljyh.music.utils.ImageUtils.downloadImageBytes
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.valuepair.ImageFormats
import org.jaudiotagger.tag.images.ArtworkFactory
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