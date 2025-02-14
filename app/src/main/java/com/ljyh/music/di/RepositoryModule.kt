package com.ljyh.music.di

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ljyh.music.data.model.room.Color
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.model.room.Song
import com.ljyh.music.data.network.ApiService
import com.ljyh.music.data.repository.HomeRepository
import com.ljyh.music.data.repository.PlaylistRepository
import com.ljyh.music.data.repository.ShareRepository
import com.ljyh.music.data.repository.UserRepository
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
    fun provideHomeRepository(apiService: ApiService): HomeRepository {
        return HomeRepository(apiService)
    }


    @Singleton
    @Provides
    fun providePlaylistRepository(apiService: ApiService): PlaylistRepository {
        return PlaylistRepository(apiService)
    }

    @Singleton
    @Provides
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepository(apiService)
    }


    @Singleton
    @Provides
    fun provideShareRepository(apiService: ApiService): ShareRepository {
        return ShareRepository(apiService)
    }
}


class ColorRepository @Inject constructor(private val colorDao: ColorDao) {

    fun getColor(url:String): Color? {
        return colorDao.getColor(url)
    }
    suspend fun insertColor(color: Color) {
        colorDao.insertColor(color)
    }
}


class SongRepository @Inject constructor(private val songDao: SongDao) {
    fun getSong(id:String): Flow<Song?> {
        return songDao.getSong(id)
    }

    fun getAllSong(): Flow<List<Song>> {
        return songDao.getAllSong()
    }

    suspend fun insertSong(song: Song) {
        songDao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }

}

class LikeRepository @Inject constructor(private val likeDao: LikeDao) {
    fun getLike(id:String): Like? {
        return likeDao.getLike(id)
    }

    fun getAllLike(): List<Like> {
        return likeDao.getALlLike()
    }

    suspend fun insertLike(like:Like) {
        likeDao.insertLike(like)
    }
    suspend fun updateAllLike(likes: List<Like>) {
        likeDao.updateALlLike(likes)
    }

    suspend fun deleteLike(id: String) {
        likeDao.deleteLike(id)
    }
}