package com.ljyh.mei.di.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.data.model.room.AlbumEntity
import com.ljyh.mei.data.model.room.AlbumWithArtists
import com.ljyh.mei.data.model.room.ArtistEntity
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.CachedLyric
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.PlaybackHistory
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.di.dao.AlbumsDao
import com.ljyh.mei.di.dao.CachedLyricDao
import com.ljyh.mei.di.dao.ColorDao
import com.ljyh.mei.di.dao.HistoryDao
import com.ljyh.mei.di.dao.LikeDao
import com.ljyh.mei.di.dao.SongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorRepository @Inject constructor(private val colorDao: ColorDao) {
    private val memoryCache = ConcurrentHashMap<String, Color>()

    fun getFromMemory(url: String): Color? {
        if (url.isEmpty()) return null
        return memoryCache[url]
    }

    suspend fun getColorOrExtract(context: Context, url: String): Color = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext Color.Black
        memoryCache[url]?.let { return@withContext it }
        val dbEntity = colorDao.getColor(url)
        if (dbEntity != null) {
            val color = Color(dbEntity.color)
            memoryCache[url] = color
            return@withContext color
        }
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url).allowHardware(false).size(128).build()
        val result = loader.execute(request)
        val finalColor = if (result is SuccessResult) {
            extractAndBoostColor(result.image.toBitmap())
        } else Color.Black
        memoryCache[url] = finalColor
        colorDao.insertColor(CacheColor(url = url, color = finalColor.toArgb()))
        finalColor
    }

    private fun extractAndBoostColor(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap).generate()
        val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
        val seed = vibrant ?: dominant ?: Color.Black
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed.toArgb(), hsl)
        val isMonotone = hsl[1] < 0.1f || hsl[2] < 0.05f || hsl[2] > 0.95f
        return if (isMonotone) Color(0xFF2E3192) else boostSaturation(seed, 1.4f)
    }

    private fun boostSaturation(color: Color, multiplier: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = (hsl[1] * multiplier).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    fun getDbColor(url: String): Color? = colorDao.getColor(url)?.let { Color(it.color) }
    suspend fun insertColor(color: CacheColor) = colorDao.insertColor(color)
}

class QQSongRepository @Inject constructor(private val qqSongDao: com.ljyh.mei.di.dao.QQSongDao) {
    fun getQQSong(id: String): Flow<com.ljyh.mei.data.model.room.QQSong?> = qqSongDao.getSong(id)
    suspend fun insertSong(song: com.ljyh.mei.data.model.room.QQSong) = qqSongDao.insertSong(song)
    suspend fun deleteSongById(id: String) = qqSongDao.deleteSongById(id)
    suspend fun deleteAll() = qqSongDao.deleteAll()
}

class LikeRepository @Inject constructor(private val likeDao: LikeDao) {
    suspend fun getLike(id: String): Like? = likeDao.getLike(id)
    suspend fun getAllLike(): List<Like> = likeDao.getALlLike()
    suspend fun insertLike(like: Like) = likeDao.insertLike(like)
    suspend fun updateAllLike(likes: List<Like>) = likeDao.updateALlLike(likes)
    suspend fun deleteLike(id: String) = likeDao.deleteLike(id)
}

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao: SongDao
) {
    suspend fun addToHistory(song: Song) {
        try {
            historyDao.addSongToHistory(song)
        } catch (e: Exception) {
            if (e is SQLiteConstraintException) {
                songDao.insertSongs(listOf(song))
                historyDao.insertHistory(PlaybackHistory(songId = song.id, playedAt = System.currentTimeMillis()))
            } else throw e
        }
    }

    fun getHistoryStream(): Flow<List<HistoryItem>> = historyDao.getHistory()
    suspend fun getHistory(): List<HistoryItem> = historyDao.getHistory().first()
    suspend fun clearHistory() = historyDao.clearHistory()
    suspend fun removeFromHistory(songId: String) = historyDao.deleteHistoryBySongId(songId)
    suspend fun addMultipleToHistory(songs: List<Song>) { songs.forEach { addToHistory(it) } }
    suspend fun getRecentSongs(limit: Int = 20): List<HistoryItem> = historyDao.getHistory().first().take(limit)
    suspend fun isSongInHistory(songId: String): Boolean = historyDao.getHistory().first().any { it.song.id == songId }
    suspend fun getHistoryCount(): Int = historyDao.getHistory().first().size
}

class AlbumsRepository @Inject constructor(private val dao: AlbumsDao) {
    suspend fun getAlbumWithArtists(id: Long): AlbumWithArtists = dao.getAlbumWithArtists(id)
    suspend fun getAlbumsByArtist(id: Long): List<AlbumEntity> = dao.getAlbumsByArtist(id)
    suspend fun insertAlbum(album: AlbumEntity, artists: List<ArtistEntity>) = dao.insertAlbumWithArtists(album, artists)
    suspend fun existsAlbum(albumId: Long): Boolean = dao.existsAlbum(albumId)
    suspend fun deleteAlbum(albumId: Long) { dao.deleteAlbumById(albumId); dao.deleteOrphanedArtists() }
    suspend fun deleteAlbums(albumIds: List<Long>) { dao.deleteAlbumsByIds(albumIds); dao.deleteOrphanedArtists() }
    suspend fun deleteArtistWithCleanup(artistId: Long) {
        dao.deleteArtistFromAllAlbums(artistId)
        if (!dao.isArtistUsed(artistId)) dao.deleteArtist(artistId)
    }
}

class CachedLyricRepository @Inject constructor(private val dao: CachedLyricDao) {
    fun get(songId: String): Flow<CachedLyric?> = dao.get(songId)
    suspend fun insert(lyric: CachedLyric) = dao.insert(lyric)
    suspend fun delete(songId: String) = dao.delete(songId)
    suspend fun deleteOld(before: Long) = dao.deleteOld(before)
    suspend fun deleteAll() = dao.deleteAll()
}
