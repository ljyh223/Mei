package com.ljyh.mei.di

import com.ljyh.mei.data.model.room.Color
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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


class PlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {
    suspend fun getPlaylist(id: String): Playlist? {
        return playlistDao.getPlaylist(id)
    }

    suspend fun getPlaylistByAuthor(author: String): List<Playlist> {
        return if (author == "") {
            emptyList()
        } else {
            playlistDao.getPlaylistByAuthor(author)
        }
    }

    fun getAllPlaylist(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylist()
    }

    suspend fun insertPlaylist(playlist: Playlist) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun insertPlaylists(playlists: List<Playlist>) {
        playlistDao.insertPlaylists(playlists)
    }
}