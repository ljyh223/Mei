package com.ljyh.mei.utils

import android.content.Context
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

    suspend fun scanAllMusic(
        rootPath: String,
        onProgress: (ScanProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            Timber.w("scanAllMusic: rootPath does not exist: $rootPath")
            return@withContext
        }

        Timber.d("scanAllMusic started: $rootPath")

        val subDirs = rootDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        val rootFiles = collectAudioFiles(rootDir, recursive = false)

        val totalDirs = subDirs.size + if (rootFiles.isNotEmpty()) 1 else 0
        var processedDirs = 0

        if (rootFiles.isNotEmpty()) {
            onProgress(ScanProgress(0, totalDirs, "扫描根目录...", true))
            val songIds = insertOrUpdateSongs(rootFiles, rootPath, onProgress)
            createPlaylistFromScan(rootPath, rootFiles.size, songIds)
            registerScanFolder(rootPath, "本地音乐", isDefault = false, songCount = rootFiles.size)
            processedDirs++
        }

        for (subDir in subDirs) {
            if (!subDir.exists() || !subDir.isDirectory) continue
            onProgress(ScanProgress(processedDirs, totalDirs, subDir.name, true))

            val files = collectAudioFiles(subDir, recursive = true)
            if (files.isEmpty()) {
                processedDirs++
                continue
            }

            val songIds = insertOrUpdateSongs(files, subDir.absolutePath, onProgress)
            createPlaylistFromScan(subDir.absolutePath, files.size, songIds)
            registerScanFolder(subDir.absolutePath, subDir.name, isDefault = false, songCount = files.size)
            processedDirs++
        }

        onProgress(ScanProgress(totalDirs, totalDirs, "扫描完成", false))
        Timber.d("scanAllMusic complete: $processedDirs directories processed")
    }

    suspend fun scanFilePaths(
        rootPath: String,
        label: String? = null,
        isDefault: Boolean = false,
        onProgress: (ScanProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext

        Timber.d("scanFilePaths started: $rootPath")

        val files = collectAudioFiles(rootDir, recursive = true)
        registerScanFolder(rootPath, label, isDefault, files.size)

        val insertedSongIds = insertOrUpdateSongs(files, rootPath, onProgress)
        createPlaylistFromScan(rootPath, files.size, insertedSongIds)

        Timber.d("scanFilePaths complete: ${files.size} files found")
    }

    private suspend fun registerScanFolder(
        path: String,
        label: String?,
        isDefault: Boolean,
        songCount: Int
    ) {
        val existing = scanFolderRepository.getByPath(path)
        if (existing != null) {
            scanFolderRepository.updateScanResult(existing.id, System.currentTimeMillis(), songCount)
        } else {
            val folder = ScanFolder(
                path = path,
                label = label ?: path.substringAfterLast('/').ifEmpty { path },
                isDefault = isDefault,
                enabled = true,
                lastScanAt = System.currentTimeMillis(),
                songCount = songCount
            )
            scanFolderRepository.insert(folder)
        }
    }

    private suspend fun insertOrUpdateSongs(
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
                val updatedSong = createSongFromFile(file, folderPath)
                if (updatedSong != null) {
                    val fileHasTags = try {
                        val tag = org.jaudiotagger.audio.AudioFileIO.read(file).tag
                        tag?.getFirst(FieldKey.TITLE)?.isNotBlank() == true
                    } catch (_: Exception) { false }
                    songRepository.updateMetadata(
                        id = existing.id,
                        title = if (fileHasTags) updatedSong.title else existing.title,
                        artist = if (fileHasTags) updatedSong.artist else existing.artist,
                        album = if (fileHasTags) updatedSong.album else existing.album,
                        cover = if (fileHasTags && updatedSong.cover.isNotEmpty()) updatedSong.cover else existing.cover,
                        duration = updatedSong.duration,
                        path = updatedSong.path,
                        fileHash = updatedSong.fileHash,
                        fileSize = updatedSong.fileSize,
                        fileFormat = updatedSong.fileFormat,
                        bitrate = updatedSong.bitrate,
                        sampleRate = updatedSong.sampleRate
                    )
                }
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

    private fun collectAudioFiles(dir: File, recursive: Boolean): List<File> {
        val results = mutableListOf<File>()
        val files = dir.listFiles() ?: return results
        for (file in files) {
            if (file.isDirectory && recursive) {
                results.addAll(collectAudioFiles(file, recursive = true))
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    results.add(file)
                }
            }
        }
        return results
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

    private suspend fun createPlaylistFromScan(folderPath: String, songCount: Int, songIds: List<String>) {
        val playlistId = "folder_${folderPath.hashCode().toUInt()}"
        val folderName = folderPath.substringAfterLast('/').ifEmpty { folderPath }

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
