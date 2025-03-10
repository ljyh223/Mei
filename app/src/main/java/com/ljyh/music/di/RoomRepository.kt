package com.ljyh.music.di

import com.ljyh.music.data.model.room.Color
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.model.room.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class ColorRepository @Inject constructor(private val colorDao: ColorDao) {

    fun getColor(url:String): Color? {
        return colorDao.getColor(url)
    }
    suspend fun insertColor(color: Color) {
        colorDao.insertColor(color)
    }
}

class QQSongRepository @Inject constructor(private val qqSongDao: QQSongDao) {
    fun getQQSong(id: String): Flow<QQSong?> {
        return qqSongDao.getSong(id)
    }

    suspend fun insertSong(song: QQSong) {
        qqSongDao.insertSong(song)
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
    suspend fun getLike(id:String): Like? {
        return likeDao.getLike(id)
    }

    suspend fun getAllLike(): List<Like> {
        return likeDao.getALlLike()
    }

    suspend fun insertLike(like: Like) {
        likeDao.insertLike(like)
    }
    suspend fun updateAllLike(likes: List<Like>) {
        likeDao.updateALlLike(likes)
    }

    suspend fun deleteLike(id: String) {
        likeDao.deleteLike(id)
    }
}