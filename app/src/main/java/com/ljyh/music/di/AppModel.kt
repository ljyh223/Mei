package com.ljyh.music.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


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





}
