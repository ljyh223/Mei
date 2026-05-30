package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM song where id=:id")
    fun getSong(id: String): Flow<Song?>

    @Query("SELECT * FROM song")
    fun getAllSong(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE path IS NOT NULL AND path != ''")
    fun getLocalSongs(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE sourceType = :sourceType")
    fun getSongsBySource(sourceType: SourceType): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE folderPath = :folderPath")
    fun getSongsByFolder(folderPath: String): Flow<List<Song>>

    @Query("SELECT DISTINCT album FROM song WHERE album != '' AND path IS NOT NULL AND path != ''")
    fun getLocalAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM song WHERE artist != '[]' AND path IS NOT NULL AND path != ''")
    fun getLocalArtists(): Flow<List<String>>

    @Query("SELECT * FROM song WHERE artist LIKE '%\"' || :artist || '\"%' AND path IS NOT NULL AND path != ''")
    fun getLocalSongsByArtist(artist: String): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE artist LIKE '%' || :artist || '%' AND path IS NOT NULL AND path != ''")
    fun getLocalSongsByArtistContains(artist: String): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE album = :album AND path IS NOT NULL AND path != ''")
    fun getLocalSongsByAlbum(album: String): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE fileFormat IN ('flac','wav','ape','dsf','dff') AND path IS NOT NULL AND path != ''")
    fun getLosslessSongs(): Flow<List<Song>>

    @Query("UPDATE song SET path = :path, updatedAt = :time WHERE id = :id")
    suspend fun updatePath(id: String, path: String?, time: Long = System.currentTimeMillis())

    @Query("""
        UPDATE song SET 
            title = :title, 
            artist = :artist, 
            album = :album, 
            cover = :cover, 
            duration = :duration, 
            path = :path, 
            fileHash = :fileHash, 
            fileSize = :fileSize, 
            fileFormat = :fileFormat, 
            bitrate = :bitrate, 
            sampleRate = :sampleRate, 
            updatedAt = :time 
        WHERE id = :id
    """)
    suspend fun updateMetadata(
        id: String,
        title: String,
        artist: List<String>,
        album: String,
        cover: String,
        duration: Long,
        path: String?,
        fileHash: String?,
        fileSize: Long,
        fileFormat: String?,
        bitrate: Int?,
        sampleRate: Int?,
        time: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("DELETE FROM song WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface QQSongDao {
    @Query("SELECT * FROM qqSong where id=:id")
    fun getSong(id: String): Flow<QQSong?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(qqSong: QQSong)

    @Query("DELETE FROM qqSong WHERE id = :id")
    suspend fun deleteSongById(id: String)

    @Query("DELETE FROM qqSong")
    suspend fun deleteAll()
}
