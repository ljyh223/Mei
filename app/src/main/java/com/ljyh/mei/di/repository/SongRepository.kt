package com.ljyh.mei.di.repository

import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import com.ljyh.mei.di.dao.SongDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(private val songDao: SongDao) {
    fun getSong(id: String): Flow<Song?> = songDao.getSong(id)
    fun getAllSong(): Flow<List<Song>> = songDao.getAllSong()
    fun getLocalSongs(): Flow<List<Song>> = songDao.getLocalSongs()
    fun getSongsBySource(sourceType: SourceType): Flow<List<Song>> = songDao.getSongsBySource(sourceType)
    fun getSongsByFolder(folderPath: String): Flow<List<Song>> = songDao.getSongsByFolder(folderPath)
    fun getLocalAlbums(): Flow<List<String>> = songDao.getLocalAlbums()
    fun getLocalArtists(): Flow<List<String>> = songDao.getLocalArtists()
    fun getLocalSongsByArtist(artist: String): Flow<List<Song>> = songDao.getLocalSongsByArtist(artist)
    fun getLocalSongsByAlbum(album: String): Flow<List<Song>> = songDao.getLocalSongsByAlbum(album)
    fun getLosslessSongs(): Flow<List<Song>> = songDao.getLosslessSongs()
    suspend fun updatePath(id: String, path: String?) = songDao.updatePath(id, path)

    suspend fun updateMetadata(
        id: String,
        title: String,
        artist: String,
        album: String,
        cover: String,
        duration: Long,
        path: String?,
        fileHash: String?,
        fileSize: Long,
        fileFormat: String?,
        bitrate: Int?,
        sampleRate: Int?
    ) = songDao.updateMetadata(
        id, title, artist, album, cover, duration, path, fileHash, fileSize, fileFormat, bitrate, sampleRate
    )
    suspend fun insertSong(song: Song) = songDao.insertSong(song)
    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)
    suspend fun deleteById(id: String) = songDao.deleteById(id)
}
