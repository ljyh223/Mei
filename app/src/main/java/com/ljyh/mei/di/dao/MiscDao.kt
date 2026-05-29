package com.ljyh.mei.di.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ljyh.mei.data.model.room.AlbumArtistCrossRef
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.AlbumWithArtists
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.CachedLyric
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.PlaybackHistory
import com.ljyh.mei.data.model.room.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface ColorDao {
    @Query("SELECT * FROM color where url=:url")
    fun getColor(url: String): CacheColor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: CacheColor)
}

@Dao
interface LikeDao {
    @Query("SELECT * FROM `like` where id=:id")
    suspend fun getLike(id: String): Like?

    @Query("SELECT * FROM `like`")
    suspend fun getALlLike(): List<Like>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: Like)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateALlLike(likes: List<Like>)

    @Query("DELETE FROM `like` where id=:id")
    suspend fun deleteLike(id: String)
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistory)

    @Transaction
    suspend fun addSongToHistory(song: Song) {
        insertOrUpdateSong(song)
        insertHistory(PlaybackHistory(songId = song.id, playedAt = System.currentTimeMillis()))
    }

    @Query("""
        SELECT song.*, history.playedAt 
        FROM playback_history AS history
        INNER JOIN song ON history.songId = song.id
        ORDER BY history.playedAt DESC
    """)
    fun getHistory(): Flow<List<HistoryItem>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE songId = :songId")
    suspend fun deleteHistoryBySongId(songId: String)
}

@Dao
interface AlbumsDao {
    @Transaction
    @Query("SELECT * FROM albums WHERE albumId = :id")
    suspend fun getAlbumWithArtists(id: Long): AlbumWithArtists

    @Transaction
    @Query("""
        SELECT * FROM albums
        INNER JOIN album_artist_cross_ref
        ON albums.albumId = album_artist_cross_ref.albumId
        WHERE album_artist_cross_ref.artistId = :artistId""")
    suspend fun getAlbumsByArtist(artistId: Long): List<AlbumEntity>

    @Transaction
    suspend fun insertAlbumWithArtists(album: AlbumEntity, artists: List<ArtistEntity>) {
        insertAlbum(album)
        insertArtists(artists)
        val refs = artists.map { AlbumArtistCrossRef(albumId = album.albumId, artistId = it.artistId) }
        insertAlbumArtistRefs(refs)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAlbum(album: AlbumEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertArtists(artists: List<ArtistEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAlbumArtistRefs(refs: List<AlbumArtistCrossRef>)

    @Query("SELECT EXISTS(SELECT 1 FROM albums WHERE albumId = :albumId)")
    suspend fun existsAlbum(albumId: Long): Boolean

    @Transaction
    suspend fun deleteAlbumById(albumId: Long) {
        deleteAlbumArtistRefsByAlbumId(albumId)
        deleteAlbum(albumId)
    }

    @Transaction
    suspend fun deleteAlbumsByIds(albumIds: List<Long>) {
        if (albumIds.isEmpty()) return
        deleteAlbumArtistRefsByAlbumIds(albumIds)
        deleteAlbums(albumIds)
    }

    suspend fun deleteArtistFromAllAlbums(artistId: Long) {
        deleteAlbumArtistRefsByArtistId(artistId)
    }

    @Transaction
    suspend fun deleteOrphanedArtists() {
        val orphanedArtistIds = getOrphanedArtistIds()
        if (orphanedArtistIds.isNotEmpty()) deleteArtistsByIds(orphanedArtistIds)
    }

    @Delete suspend fun deleteAlbum(album: AlbumEntity)
    @Query("DELETE FROM albums WHERE albumId = :albumId") suspend fun deleteAlbum(albumId: Long)
    @Query("DELETE FROM albums WHERE albumId IN (:albumIds)") suspend fun deleteAlbums(albumIds: List<Long>)
    @Delete suspend fun deleteArtist(artist: ArtistEntity)
    @Query("DELETE FROM artists WHERE artistId = :artistId") suspend fun deleteArtist(artistId: Long)
    @Query("DELETE FROM artists WHERE artistId IN (:artistIds)") suspend fun deleteArtistsByIds(artistIds: List<Long>)
    @Delete suspend fun deleteAlbumArtistRef(ref: AlbumArtistCrossRef)
    @Query("DELETE FROM album_artist_cross_ref WHERE albumId = :albumId") suspend fun deleteAlbumArtistRefsByAlbumId(albumId: Long)
    @Query("DELETE FROM album_artist_cross_ref WHERE albumId IN (:albumIds)") suspend fun deleteAlbumArtistRefsByAlbumIds(albumIds: List<Long>)
    @Query("DELETE FROM album_artist_cross_ref WHERE artistId = :artistId") suspend fun deleteAlbumArtistRefsByArtistId(artistId: Long)

    @Query("SELECT artistId FROM artists WHERE artistId NOT IN (SELECT DISTINCT artistId FROM album_artist_cross_ref)")
    suspend fun getOrphanedArtistIds(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM album_artist_cross_ref WHERE artistId = :artistId)")
    suspend fun isArtistUsed(artistId: Long): Boolean
}

@Dao
interface CachedLyricDao {
    @Query("SELECT * FROM cached_lyric WHERE songId = :songId")
    fun get(songId: String): Flow<CachedLyric?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lyric: CachedLyric)

    @Query("DELETE FROM cached_lyric WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM cached_lyric WHERE updatedAt < :before")
    suspend fun deleteOld(before: Long)

    @Query("DELETE FROM cached_lyric")
    suspend fun deleteAll()
}
