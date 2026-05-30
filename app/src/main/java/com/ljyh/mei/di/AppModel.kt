package com.ljyh.mei.di

import android.content.Context
import com.google.gson.Gson
import com.ljyh.mei.di.repository.AlbumsRepository
import com.ljyh.mei.di.repository.CachedLyricRepository
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.di.repository.DownloadRepository
import com.ljyh.mei.di.repository.HistoryRepository
import com.ljyh.mei.di.repository.LikeRepository
import com.ljyh.mei.di.repository.LocalPlaylistRepository
import com.ljyh.mei.di.repository.PlaylistSongCrossRefRepository
import com.ljyh.mei.di.repository.QQSongRepository
import com.ljyh.mei.di.repository.SongRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase =
        AppDatabase.getDatabase(appContext)

    @Provides @Singleton
    fun provideColorDao(db: AppDatabase): ColorRepository =
        ColorRepository(db.colorDao())

    @Provides
    fun provideLikeDao(db: AppDatabase): LikeRepository =
        LikeRepository(db.likeDao())

    @Provides
    fun provideSongDao(db: AppDatabase): SongRepository =
        SongRepository(db.songDao())

    @Provides
    fun provideQQSongDao(db: AppDatabase): QQSongRepository =
        QQSongRepository(db.qqSongDao())

    @Provides
    fun providePlaylistDao(db: AppDatabase): LocalPlaylistRepository =
        LocalPlaylistRepository(db.playlistDao())

    @Provides @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryRepository =
        HistoryRepository(database.historyDao(), database.songDao())

    @Provides
    fun provideAlbumsDao(db: AppDatabase): AlbumsRepository =
        AlbumsRepository(db.AlbumsDao())

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadRepository =
        DownloadRepository(db.downloadDao(), db.songDao())

    @Provides
    fun providePlaylistSongCrossRefDao(db: AppDatabase): PlaylistSongCrossRefRepository =
        PlaylistSongCrossRefRepository(db.playlistSongCrossRefDao())

    @Provides
    fun provideCachedLyricDao(db: AppDatabase): CachedLyricRepository =
        CachedLyricRepository(db.cachedLyricDao())

    @Provides @Singleton
    fun provideGson(): Gson = Gson()
}
