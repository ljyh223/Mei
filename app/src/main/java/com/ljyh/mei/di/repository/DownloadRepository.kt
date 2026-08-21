package com.ljyh.mei.di.repository

import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import kotlinx.coroutines.flow.firstOrNull
import com.ljyh.mei.di.dao.DownloadDao
import com.ljyh.mei.di.dao.SongDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val songDao: SongDao
) {
    fun getAll(): Flow<List<DownloadTask>> = downloadDao.getAll()
    fun activeCount(): Flow<Int> = downloadDao.activeCount()
    suspend fun getBySongId(songId: String): DownloadTask? = downloadDao.getBySongId(songId)
    suspend fun insert(task: DownloadTask) = downloadDao.insert(task)
    suspend fun insertAll(tasks: List<DownloadTask>) = downloadDao.insertAll(tasks)
    suspend fun updateProgress(songId: String, status: DownloadStatus, progress: Int) {
        downloadDao.updateProgress(songId, status, progress, time = System.currentTimeMillis())

        // 当下载完成时，自动同步更新 songDao 里的 path
        if (status == DownloadStatus.COMPLETED) {
            val task = downloadDao.getBySongId(songId)
            if (task != null) {
                val song = songDao.getSong(songId).firstOrNull()
                if (song != null) {
                    // 根据task里面保存的本地文件路径字段
                    // 拼接出文件的完整路径
                    val filePath = task.fileName
                    val folder = filePath.substringBeforeLast('/', "")

                    songDao.insertSong(
                        song.copy(
                            path = filePath,
                            folderPath = folder
                        )
                    )
                }
            }
        }
    }
    suspend fun updateFileInfo(songId: String, url: String, fileName: String, fileType: String) =
        downloadDao.updateFileInfo(songId, url, fileName, fileType)
    suspend fun pause(songId: String) = downloadDao.updateStatus(songId, DownloadStatus.PAUSED)
    suspend fun resume(songId: String) = downloadDao.updateStatus(songId, DownloadStatus.PENDING)
    suspend fun delete(songId: String) = downloadDao.delete(songId)
    suspend fun deleteAll() = downloadDao.deleteAll()
    suspend fun markAllDownloadingAsFailed() = downloadDao.markAllDownloadingAsFailed()
    suspend fun getDownloadedSongIds(): Set<String> {
        val tasks = downloadDao.getAll().first()
        return tasks.filter { it.status == DownloadStatus.COMPLETED }.map { it.songId }.toSet()
    }
}
