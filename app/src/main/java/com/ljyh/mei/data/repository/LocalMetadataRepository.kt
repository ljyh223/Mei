package com.ljyh.mei.data.repository

import android.content.Context
import android.net.Uri
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.di.dao.SongDao
import com.ljyh.mei.download.AudioTagWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LocalMetadataRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchRepository: SearchRepository,
    private val songDao: SongDao,
) {
    suspend fun findMatches(query: String): Resource<List<MediaMetadata>> {
        if (query.isBlank()) return Resource.Error("请输入歌曲名或歌手")
        return when (val result = searchRepository.search(query.trim(), type = 1, limit = 12)) {
            is Resource.Success -> Resource.Success(result.data.songs)
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }

    suspend fun saveDisplay(songId: String, candidate: MediaMetadata) = withContext(Dispatchers.IO) {
        val song = requireNotNull(songDao.getSongOnce(songId)) { "本地歌曲已不存在" }
        require(!song.path.isNullOrBlank()) { "找不到音频文件" }
        songDao.updateMatchedMetadata(
            id = songId,
            title = candidate.title,
            artist = candidate.artists.map { it.name },
            album = candidate.album.title,
            cover = candidate.coverUrl,
            matchedId = candidate.id,
        )
    }

    suspend fun writeToFile(songId: String, candidate: MediaMetadata) = withContext(Dispatchers.IO) {
        val song = requireNotNull(songDao.getSongOnce(songId)) { "本地歌曲已不存在" }
        val path = song.path ?: error("找不到音频文件")
        require(path.startsWith("content://")) { "该文件不支持通过系统音乐库写入" }
        val uri = Uri.parse(path)
        val extension = song.fileFormat?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,8}")) } ?: "mp3"
        val original = File.createTempFile("mei-original-", ".$extension", context.cacheDir)
        val edited = File.createTempFile("mei-edited-", ".$extension", context.cacheDir)
        var retainBackup = false
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                original.outputStream().use { output -> input.copyTo(output) }
            } ?: error("无法读取音频文件")
            original.copyTo(edited, overwrite = true)
            val written = AudioTagWriter.writeTags(
                title = candidate.title,
                artist = candidate.artists.joinToString(" / ") { it.name },
                album = candidate.album.title,
                coverUrl = candidate.coverUrl,
                filePath = edited.absolutePath,
            ) ?: error("当前音频格式不支持写入标签")
            check(written.hasTitle && written.hasArtist && written.hasAlbum) { "音频标签写入未完成" }
            check(candidate.coverUrl.isBlank() || written.hasCover) { "封面写入未完成" }
            var writeStarted = false
            try {
                context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                    writeStarted = true
                    edited.inputStream().use { input -> input.copyTo(output) }
                } ?: error("无法写入音频文件")
            } catch (error: Exception) {
                if (!writeStarted) throw error
                val restored = runCatching {
                    context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                        original.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("无法恢复原文件")
                }.isSuccess
                if (!restored) {
                    retainBackup = true
                    error("写入失败，原文件备份保留在 ${original.absolutePath}")
                }
                throw error
            }
            saveDisplay(songId, candidate)
            context.contentResolver.notifyChange(uri, null)
        } finally {
            edited.delete()
            if (!retainBackup) original.delete()
        }
    }
}
