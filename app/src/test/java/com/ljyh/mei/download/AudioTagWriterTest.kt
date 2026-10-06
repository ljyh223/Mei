package com.ljyh.mei.download

import com.ljyh.mei.utils.preferences.get
import java.io.File
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudioTagWriterTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val title = "测试 / Song. 你好"
    private val artist = "歌手 A, Artist B"
    private val album = "专辑 · Album"
    private val lyrics = "[00:01.00]第一句\n[00:02.50]Second line"

    @Test
    fun writesAndReadsBackDownloadTagsForSupportedContainers() = runBlocking {
        val cover = resourceBytes("cover.jpg")
        for (extension in listOf("mp3", "flac", "m4a")) {
            val file = sample(extension)
            val status = AudioTagWriter.writeTagsWithCoverBytes(
                title, artist, album, cover, file.absolutePath, lyrics,
            )
            assertNotNull("$extension write failed", status)
            assertTrue("$extension tag status incomplete: $status", status!!.isComplete)

            // Inspect the persisted file, not just the status returned by the writer.
            val audio = AudioFileIO.read(file)
            val tag = audio.tag
            assertEquals("$extension title", title, tag.getFirst(FieldKey.TITLE))
            assertEquals("$extension artist", artist, tag.getFirst(FieldKey.ARTIST))
            assertEquals("$extension album", album, tag.getFirst(FieldKey.ALBUM))
            assertEquals("$extension album artist", artist, tag.getFirst(FieldKey.ALBUM_ARTIST))
            assertEquals("$extension lyrics", lyrics, tag.getFirst(FieldKey.LYRICS))
            assertArrayEquals("$extension cover", cover, tag.firstArtwork?.binaryData)
            if (extension != "m4a") {
                // MP4's covr atom does not expose an ID3/FLAC picture type.
                assertEquals("$extension front cover type", 3, tag.firstArtwork?.pictureType)
            }
            assertEquals("$extension cover MIME", "image/jpeg", tag.firstArtwork?.mimeType)
            if (extension == "flac") {
                assertEquals("FLAC cover width", 16, tag.firstArtwork?.width)
                assertEquals("FLAC cover height", 16, tag.firstArtwork?.height)
            }
        }
    }

    @Test
    fun lyricRepairRetainsCoverAndOtherMetadata() = runBlocking {
        val file = sample("mp3")
        val cover = resourceBytes("cover.jpg")
        val initial = AudioTagWriter.writeTagsWithCoverBytes(
            title, artist, album, cover, file.absolutePath, "[00:01.00]old",
        )
        assertTrue(initial?.isComplete == true)
        val audio = AudioFileIO.read(file)
        audio.tag.setField(FieldKey.GENRE, "Electronic")
        audio.tag.setField(FieldKey.TRACK, "7")
        audio.commit()

        val repaired = AudioTagWriter.writeTagsWithCoverBytes(
            title, artist, album, null, file.absolutePath, lyrics,
        )
        assertTrue(repaired?.isComplete == true)
        val saved = AudioFileIO.read(file).tag
        assertEquals(lyrics, saved.getFirst(FieldKey.LYRICS))
        assertArrayEquals(cover, saved.firstArtwork?.binaryData)
        assertEquals("Electronic", saved.getFirst(FieldKey.GENRE))
        assertEquals("7", saved.getFirst(FieldKey.TRACK))
    }

    @Test
    fun upgradingOldMp3TagKeepsUnrelatedFields() = runBlocking {
        val file = sample("mp3")
        val audio = AudioFileIO.read(file)
        audio.tag = ID3v23Tag().apply {
            setField(FieldKey.GENRE, "Electronic")
            setField(FieldKey.TRACK, "7")
        }
        audio.commit()

        val status = AudioTagWriter.writeTagsWithCoverBytes(
            title, artist, album, resourceBytes("cover.jpg"), file.absolutePath, lyrics,
        )
        assertTrue(status?.isComplete == true)
        val saved = AudioFileIO.read(file).tag
        assertEquals("Electronic", saved.getFirst(FieldKey.GENRE))
        assertEquals("7", saved.getFirst(FieldKey.TRACK))
    }

    @Test
    fun pngCoverKeepsItsActualMimeType() = runBlocking {
        val cover = resourceBytes("cover.png")
        for (extension in listOf("mp3", "flac", "m4a")) {
            val file = sample(extension)
            val status = AudioTagWriter.writeTagsWithCoverBytes(
                title, artist, album, cover, file.absolutePath, lyrics,
            )
            assertTrue("$extension status: $status", status?.isComplete == true)
            val artwork = AudioFileIO.read(file).tag.firstArtwork
            assertArrayEquals("$extension cover", cover, artwork?.binaryData)
            assertEquals("$extension MIME", "image/png", artwork?.mimeType)
            if (extension != "m4a") assertEquals("$extension picture type", 3, artwork?.pictureType)
            if (extension == "flac") {
                assertEquals(16, artwork?.width)
                assertEquals(16, artwork?.height)
            }
        }
    }

    @Test
    fun corruptAudioReportsTaggingFailure() = runBlocking {
        val file = temporaryFolder.newFile("corrupt.mp3").apply { writeText("not an audio file") }
        assertNull(AudioTagWriter.writeTagsWithCoverBytes(
            title, artist, album, resourceBytes("cover.jpg"), file.absolutePath, lyrics,
        ))
        assertNull(AudioTagWriter.checkTags(file.absolutePath))
    }

    private fun sample(extension: String): File = temporaryFolder.newFile("sample.$extension").also {
        it.writeBytes(resourceBytes("blank.$extension"))
    }

    private fun resourceBytes(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/audio-tags/$name")) { "Missing fixture: $name" }
            .use { it.readBytes() }
}
