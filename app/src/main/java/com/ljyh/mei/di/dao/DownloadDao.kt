package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_task WHERE songId = :songId")
    suspend fun getBySongId(songId: String): DownloadTask?

    @Query("SELECT * FROM download_task ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DownloadTask>>

    @Query("SELECT COUNT(*) FROM download_task WHERE status = 'DOWNLOADING' OR status = 'PENDING'")
    fun activeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DownloadTask>)

    @Query("UPDATE download_task SET status = :status, progress = :progress, updatedAt = :time WHERE songId = :songId")
    suspend fun updateProgress(songId: String, status: DownloadStatus, progress: Int, time: Long)

    @Query("UPDATE download_task SET url = :url, fileName = :fileName, fileType = :fileType WHERE songId = :songId")
    suspend fun updateFileInfo(songId: String, url: String, fileName: String, fileType: String)

    @Query("UPDATE download_task SET status = :status, updatedAt = :time WHERE songId = :songId")
    suspend fun updateStatus(songId: String, status: DownloadStatus, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM download_task WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM download_task")
    suspend fun deleteAll()

    @Query("UPDATE download_task SET status = 'FAILED' WHERE status = 'DOWNLOADING'")
    suspend fun markAllDownloadingAsFailed()
}
