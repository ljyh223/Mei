package com.ljyh.mei.di

import android.content.Context
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.unblockneteasemusic.qq.QqMusicProvider
import com.ljyh.mei.data.network.QrLoginClient
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.repository.HomeRepository
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.data.repository.SearchRepository
import com.ljyh.mei.data.repository.ShareRepository
import com.ljyh.mei.data.repository.UserRepository
import com.ljyh.mei.data.repository.ArtistRepository
import com.ljyh.mei.data.repository.CommentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideHomeRepository(eApiService: EApiService, apiService: ApiService): HomeRepository {
        return HomeRepository(eApiService, apiService)
    }


    @Singleton
    @Provides
    fun providePlaylistRepository(apiService: ApiService,weApiService: WeApiService, eApiService: EApiService): PlaylistRepository {
        return PlaylistRepository(apiService,weApiService, eApiService)
    }

    @Singleton
    @Provides
    fun provideUserRepository(
        apiService: ApiService,
        eApiService: EApiService,
        weApiService: WeApiService,
        qrLoginClient: QrLoginClient,
    ): UserRepository {
        return UserRepository(apiService, eApiService, weApiService, qrLoginClient)
    }


    @Singleton
    @Provides
    fun provideShareRepository(apiService: ApiService): ShareRepository {
        return ShareRepository(apiService)
    }


    @Singleton
    @Provides
    fun providePlayerRepository(
        @ApplicationContext context: Context,
        qqMusicProvider: QqMusicProvider,
        apiService: ApiService,
        weApiService: WeApiService,
    ): PlayerRepository {
        return PlayerRepository(qqMusicProvider, apiService, weApiService, context)
    }

    @Singleton
    @Provides
    fun provideSearchRepository(apiService: ApiService): SearchRepository {
        return SearchRepository(apiService)
    }

    @Singleton
    @Provides
    fun provideArtistRepository(apiService: ApiService): ArtistRepository {
        return ArtistRepository(apiService)
    }

    @Singleton
    @Provides
    fun provideCommentRepository(apiService: ApiService, weApiService: WeApiService): CommentRepository {
        return CommentRepository(apiService, weApiService)
    }
}
