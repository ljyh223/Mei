package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ljyh.mei.data.model.room.ScanFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanFolderDao {
    @Query("SELECT * FROM scan_folder WHERE enabled = 1")
    fun getEnabled(): Flow<List<ScanFolder>>

    @Query("SELECT * FROM scan_folder")
    fun getAll(): Flow<List<ScanFolder>>

    @Query("SELECT * FROM scan_folder WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): ScanFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: ScanFolder)

    @Query("UPDATE scan_folder SET lastScanAt = :time, songCount = :count WHERE id = :id")
    suspend fun updateScanResult(id: Long, time: Long, count: Int)

    @Query("UPDATE scan_folder SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM scan_folder WHERE id = :id")
    suspend fun delete(id: Long)
}
