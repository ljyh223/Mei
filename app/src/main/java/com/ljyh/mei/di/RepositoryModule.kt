package com.ljyh.mei.di

import com.ljyh.mei.data.model.room.Color
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.QQMusicCApiService
import com.ljyh.mei.data.network.QQMusicUApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.HomeRepository
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.ShareRepository
import com.ljyh.mei.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideHomeRepository(eApiService: EApiService): HomeRepository {
        return HomeRepository(eApiService)
    }


    @Singleton
    @Provides
    fun providePlaylistRepository(apiService: ApiService,weApiService: WeApiService): PlaylistRepository {
        return PlaylistRepository(apiService,weApiService)
    }

    @Singleton
    @Provides
    fun provideUserRepository(apiService: ApiService,eApiService: EApiService): UserRepository {
        return UserRepository(apiService,eApiService)
    }


    @Singleton
    @Provides
    fun provideShareRepository(apiService: ApiService, qqMusicApiCService: QQMusicCApiService, qqMusicUApiService: QQMusicUApiService ): ShareRepository {
        return ShareRepository(apiService,qqMusicApiCService,qqMusicUApiService)
    }


    @Singleton
    @Provides
    fun providePlayerRepository(qqMusicUApiService: QQMusicUApiService,apiService: ApiService,weApiService: WeApiService): PlayerRepository {
        return PlayerRepository(qqMusicUApiService,apiService,weApiService)
    }
}

