package com.ljyh.mei.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database").build()

    @Provides
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
    fun providePlaylistDao(db: AppDatabase): PlaylistRepository =
        PlaylistRepository(db.playlistDao())


}
