package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ljyh.mei.data.model.room.AlbumSubscription
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumSubscriptionDao {
    @Query("SELECT * FROM album_subscription WHERE userId = :userId AND albumId = :albumId")
    suspend fun get(userId: String, albumId: Long): AlbumSubscription?

    @Query("SELECT * FROM album_subscription WHERE userId = :userId AND albumId = :albumId")
    fun observe(userId: String, albumId: Long): Flow<AlbumSubscription?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: AlbumSubscription)
}
