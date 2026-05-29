package com.ljyh.mei.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistSongCrossRef
import com.ljyh.mei.data.model.room.PlaylistType
import com.ljyh.mei.data.model.room.ScanFolder
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import com.ljyh.mei.di.repository.PlaylistSongCrossRefRepository
import com.ljyh.mei.di.repository.ScanFolderRepository
import com.ljyh.mei.di.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jaudiotagger.tag.FieldKey
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
    val isScanning: Boolean
)

class LocalMusicScanner(
    private val context: Context,
    private val songRepository: SongRepository,
    private val scanFolderRepository: ScanFolderRepository,
    private val playlistRepository: LocalPlaylistRepository,
    private val crossRefRepository: PlaylistSongCrossRefRepository
) {
    companion object {
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "aac", "ogg", "wav", "m4a", "wma", "ape", "dsf", "dff", "opus"
        )
    }

    suspend fun scanFolder(
        uri: Uri,
        label: String? = null,
        isDefault: Boolean = false,
        onProgress: (ScanProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        Timber.d("scanFolder started: $uri")
        val decodedPath = uri.toString().let {
            try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
        }

        val folder = ScanFolder(
            path = decodedPath,
            label = label,
            isDefault = isDefault,
            enabled = true,
            lastScanAt = System.currentTimeMillis()
        )
        scanFolderRepository.insert(folder)

        val documentFile = DocumentFile.fromTreeUri(context, uri)
        if (documentFile == null) {
            Timber.e("DocumentFile.fromTreeUri returned null for $uri")
            return@withContext
        }

        if (!documentFile.exists()) {
            Timber.e("DocumentFile does not exist: $uri")
            return@withContext
        }

        val allFiles = mutableListOf<AudioFileInfo>()
        scanDirectory(documentFile, allFiles)

        Timber.d("scanFolder found ${allFiles.size} files")

        onProgress(ScanProgress(current = 0, total = allFiles.size, currentFile = "正在处理...", isScanning = true))

        val insertedSongIds = insertSongs(allFiles, folder.path, onProgress)
        createPlaylistFromScan(folder.path, allFiles.size, insertedSongIds)

        val updatedFolder = scanFolderRepository.getAll().firstOrNull()
            ?.find { it.path == decodedPath }
        if (updatedFolder != null) {
            scanFolderRepository.updateScanResult(
                updatedFolder.id,
                System.currentTimeMillis(),
                allFiles.size
            )
        }

        Timber.d("Scan complete: ${allFiles.size} files found")
    }

    suspend fun scanFilePaths(
        rootPath: String,
        label: String? = null,
        isDefault: Boolean = false,
        onProgress: (ScanProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext

        val folder = ScanFolder(
            path = rootPath,
            label = label,
            isDefault = isDefault,
            enabled = true,
            lastScanAt = System.currentTimeMillis()
        )
        scanFolderRepository.insert(folder)

        val allFiles = mutableListOf<File>()
        collectFiles(rootDir, allFiles)

        val insertedSongIds = insertFileSongs(allFiles, folder.path, onProgress)
        createPlaylistFromScan(rootPath, allFiles.size, insertedSongIds)

        val updatedFolder = scanFolderRepository.getAll().firstOrNull()
            ?.find { it.path == rootPath }
        if (updatedFolder != null) {
            scanFolderRepository.updateScanResult(
                updatedFolder.id,
                System.currentTimeMillis(),
                allFiles.size
            )
        }

        Timber.d("Scan complete: ${allFiles.size} files found")
    }

    private suspend fun insertSongs(
        files: List<AudioFileInfo>,
        folderPath: String,
        onProgress: (ScanProgress) -> Unit = {}
    ): List<String> {
        val songsToInsert = mutableListOf<Song>()
        val songIds = mutableListOf<String>()
        val existingSongs = try { songRepository.getLocalSongs().firstOrNull() } catch (_: Exception) { null }
            ?.associateBy { it.path } ?: emptyMap()

        files.forEachIndexed { index, info ->
            onProgress(
                ScanProgress(
                    current = index + 1, total = files.size,
                    currentFile = info.path.substringAfterLast('/'),
                    isScanning = true
                )
            )
            val existing = existingSongs[info.path]
            if (existing != null) {
                songRepository.updatePath(existing.id, existing.path)
                songIds.add(existing.id)
            } else {
                val song = try {
                    createSongFromUri(info, folderPath)
                } catch (e: Exception) {
                    Timber.e(e, "createSongFromUri failed: ${info.path}")
                    null
                }
                if (song != null) {
                    songsToInsert.add(song)
                    songIds.add(song.id)
                }
            }
        }

        if (songsToInsert.isNotEmpty()) {
            songRepository.insertSongs(songsToInsert)
        }

        return songIds
    }

    private suspend fun insertFileSongs(
        files: List<File>,
        folderPath: String,
        onProgress: (ScanProgress) -> Unit
    ): List<String> {
        val songsToInsert = mutableListOf<Song>()
        val songIds = mutableListOf<String>()
        val existingSongs = songRepository.getLocalSongs().firstOrNull()
            ?.associateBy { it.path } ?: emptyMap()

        files.forEachIndexed { index, file ->
            onProgress(
                ScanProgress(
                    current = index + 1,
                    total = files.size,
                    currentFile = file.name,
                    isScanning = true
                )
            )

            val existing = existingSongs[file.absolutePath]
            if (existing != null) {
                songRepository.updatePath(existing.id, existing.path)
                songIds.add(existing.id)
            } else {
                val song = createSongFromFile(file, folderPath)
                if (song != null) {
                    songsToInsert.add(song)
                    songIds.add(song.id)
                }
            }
        }

        if (songsToInsert.isNotEmpty()) {
            songRepository.insertSongs(songsToInsert)
        }

        return songIds
    }

    private fun scanDirectory(
        dir: DocumentFile,
        results: MutableList<AudioFileInfo>
    ) {
        val files = dir.listFiles()
        for (doc in files) {
            if (doc.isDirectory) {
                scanDirectory(doc, results)
            } else if (doc.isFile) {
                val name = doc.name ?: ""
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    results.add(
                        AudioFileInfo(
                            path = doc.uri.toString().let {
                                try { java.net.URLDecoder.decode(it, "UTF-8") }
                                catch (_: Exception) { it }
                            },
                            size = doc.length(),
                            uri = doc.uri
                        )
                    )
                }
            }
        }
    }

    private fun collectFiles(dir: File, results: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                collectFiles(file, results)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    results.add(file)
                }
            }
        }
    }

    private fun copyToTemp(uri: android.net.Uri): File? {
        val tempFile = File(context.cacheDir, "scan_${System.nanoTime()}")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) {
            Timber.e(e, "copyToTemp failed for $uri")
            tempFile.delete()
            null
        }
    }

    private fun createSongFromUri(info: AudioFileInfo, folderPath: String): Song? {
        val uri = info.uri ?: return null
        val (coverPath, tagData) = if (info.path.startsWith("content://")) {
            val tempFile = copyToTemp(uri)
            if (tempFile == null) {
                Timber.w("copyToTemp returned null for ${info.path}")
                "" to TagResult("", "", "", 0, null, null)
            } else {
                val audioFile = try { org.jaudiotagger.audio.AudioFileIO.read(tempFile) } catch (e: Exception) {
                    Timber.e(e, "AudioFileIO.read failed for temp file")
                    null
                }
                val result = try { readTagsFromAudioFile(audioFile) } catch (e: Exception) {
                    Timber.e(e, "readTagsFromAudioFile failed")
                    TagResult("", "", "", 0, null, null)
                }
                val cover = try { extractCoverArt(audioFile, computeHash(info)) } catch (e: Exception) {
                    Timber.e(e, "extractCoverArt failed")
                    ""
                }
                tempFile.delete()
                cover to result
            }
        } else {
            val file = File(info.path)
            val audioFile = try { org.jaudiotagger.audio.AudioFileIO.read(file) } catch (e: Exception) {
                Timber.e(e, "AudioFileIO.read failed for ${info.path}")
                null
            }
            val result = try { readTagsFromAudioFile(audioFile) } catch (e: Exception) {
                Timber.e(e, "readTagsFromAudioFile failed")
                TagResult("", "", "", 0, null, null)
            }
            val cover = try { extractCoverArt(audioFile, computeHash(info)) } catch (e: Exception) {
                Timber.e(e, "extractCoverArt failed")
                ""
            }
            cover to result
        }
        val fileName = info.path.substringAfterLast('/')
        val ext = info.path.substringAfterLast('.').lowercase()
        val hash = computeHash(info)

        return Song(
            id = generateSongId(info.path),
            title = tagData.title.ifBlank { fileName.substringBeforeLast('.') },
            artist = tagData.artist.ifBlank { "未知艺术家" },
            album = tagData.album.ifBlank { "未知专辑" },
            cover = coverPath,
            duration = tagData.duration,
            path = info.path,
            sourceType = SourceType.LOCAL,
            fileHash = hash,
            fileSize = info.size,
            fileFormat = ext,
            bitrate = tagData.bitrate,
            sampleRate = tagData.sampleRate,
            folderPath = folderPath,
            addedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createSongFromFile(file: File, folderPath: String): Song? {
        val audioFile = try { org.jaudiotagger.audio.AudioFileIO.read(file) } catch (_: Exception) { null }
        val tags = try {
            readTagsFromAudioFile(audioFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to read tags for ${file.name}")
            TagResult("", "", "", 0, null, null)
        }

        val fileName = file.name
        val nameWithoutExt = fileName.substringBeforeLast('.')
        val ext = file.extension.lowercase()
        val hash = computeHashForFile(file)
        val coverPath = extractCoverArt(audioFile, hash)

        return Song(
            id = generateSongId(file.absolutePath),
            title = tags.title.ifBlank { nameWithoutExt },
            artist = tags.artist.ifBlank { "未知艺术家" },
            album = tags.album.ifBlank { "未知专辑" },
            cover = coverPath,
            duration = tags.duration,
            path = file.absolutePath,
            sourceType = SourceType.LOCAL,
            fileHash = hash,
            fileSize = file.length(),
            fileFormat = ext,
            bitrate = tags.bitrate,
            sampleRate = tags.sampleRate,
            folderPath = folderPath,
            addedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun computeHash(info: AudioFileInfo): String {
        return try {
            val input = "${info.path}_${info.size}"
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(input.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun computeHashForFile(file: File): String {
        return try {
            val input = "${file.absolutePath}_${file.length()}"
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(input.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun generateSongId(path: String): String {
        return "local_${path.hashCode().toUInt()}"
    }

    data class AudioFileInfo(
        val path: String,
        val size: Long,
        val uri: Uri? = null
    )

    private suspend fun createPlaylistFromScan(folderPath: String, songCount: Int, songIds: List<String>) {
        val playlistId = "folder_${folderPath.hashCode().toUInt()}"

        val folderName = if (folderPath.startsWith("content://")) {
            "扫描文件夹"
        } else {
            folderPath.substringAfterLast('/').ifEmpty { folderPath }
        }

        val firstSong = songIds.firstOrNull()?.let { songRepository.getSong(it).firstOrNull() }

        val playlist = Playlist(
            id = playlistId,
            title = folderName,
            cover = firstSong?.cover ?: "",
            author = "local",
            authorName = "本地音乐",
            authorAvatar = "",
            count = songCount,
            type = PlaylistType.FOLDER,
            description = folderPath,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistRepository.insertPlaylist(playlist)

        crossRefRepository.deleteByPlaylist(playlistId)

        val crossRefs = songIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                sortOrder = index,
                addedAt = System.currentTimeMillis()
            )
        }
        crossRefRepository.insertAll(crossRefs)
    }

    private fun readTagsFromAudioFile(audioFile: org.jaudiotagger.audio.AudioFile?): TagResult {
        if (audioFile == null) return TagResult("", "", "", 0, null, null)
        val tag = audioFile.tag
        val header = audioFile.audioHeader
        return TagResult(
            title = tag?.getFirst(FieldKey.TITLE) ?: "",
            artist = tag?.getFirst(FieldKey.ARTIST) ?: "",
            album = tag?.getFirst(FieldKey.ALBUM) ?: "",
            duration = header.trackLength.toLong(),
            bitrate = header.bitRate?.toInt(),
            sampleRate = header.sampleRate?.toInt()
        )
    }

    private fun extractCoverArt(audioFile: org.jaudiotagger.audio.AudioFile?, songHash: String): String {
        try {
            val artwork = audioFile?.tag?.firstArtwork ?: return ""
            val imageData = artwork.binaryData ?: return ""
            val coverDir = File(context.cacheDir, "covers")
            if (!coverDir.exists()) coverDir.mkdirs()
            val ext = when (artwork.mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                else -> "jpg"
            }
            val coverFile = File(coverDir, "${songHash}.$ext")
            coverFile.writeBytes(imageData)
            return coverFile.absolutePath
        } catch (_: Exception) {
            return ""
        }
    }

    private data class TagResult(
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long,
        val bitrate: Int?,
        val sampleRate: Int?
    )
}
