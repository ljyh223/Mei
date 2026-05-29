package com.ljyh.mei.data.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_folder")
data class ScanFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val label: String? = null,
    val isDefault: Boolean = false,
    val enabled: Boolean = true,
    val lastScanAt: Long? = null,
    val songCount: Int = 0
)
