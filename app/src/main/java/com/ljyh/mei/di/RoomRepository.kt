package com.ljyh.mei.di

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.paging.LOG_TAG
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.PlaybackHistory
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
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
    // 内存缓存 (L1)
    private val memoryCache = ConcurrentHashMap<String, Color>()

    /**
     * 1. 【同步方法】只查内存
     * 专门给 UI 层的 Composable 使用，绝不阻塞主线程。
     * 如果返回 null，说明内存里没有，UI 应该显示默认色，并触发后台加载。
     */
    fun getFromMemory(url: String): Color? {
        Timber.tag("ColorRepository").d("开始查询: $url")
        if (url.isEmpty()) return null
        Timber.tag("ColorRepository").d("返回: ${memoryCache[url]}")
        Timber.tag("ColorRepository").d("memoryCache: ${memoryCache}")
        return memoryCache[url]
    }

    suspend fun getColorOrExtract(
        context: Context,
        url: String
    ): Color = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext Color.Black

        // 1. 查内存
        memoryCache[url]?.let { return@withContext it }

        // 2. 查数据库
        val dbEntity = colorDao.getColor(url)
        if (dbEntity != null) {
            val color = Color(dbEntity.color)
            memoryCache[url] = color
            return@withContext color
        }

        // 3. 网络提取 (核心修改部分)
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // 必须关闭硬件加速
            .size(128)            // 极速模式
            .build()

        val result = loader.execute(request)

        val finalColor = if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            // 使用统一的增强算法提取颜色
            extractAndBoostColor(bitmap)
        } else {
            Color.Black
        }

        // 4. 存缓存
        memoryCache[url] = finalColor
        colorDao.insertColor(CacheColor(url = url, color = finalColor.toArgb()))

        finalColor
    }

    /**
     * 【新增】核心色彩提取与增强逻辑
     * 保持与 AppleMusicFluidBackground 的视觉风格一致
     */
    private fun extractAndBoostColor(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap).generate()

        // 1. 优先取 Vibrant (鲜艳)，其次 Dominant (主色)
        val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }

        // 2. 确定基准色
        val seed = vibrant ?: dominant ?: Color.Black

        // 3. 判断是否为“无聊颜色” (黑/白/灰)
        // 如果是黑白图，我们不希望主题色变成死黑，而是给一个深邃的蓝色或紫色(与背景一致)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed.toArgb(), hsl)
        val isMonotone = hsl[1] < 0.1f || hsl[2] < 0.05f || hsl[2] > 0.95f

        return if (isMonotone) {
            // 这里返回一个通用的“高级暗色”，比如深蓝灰，
            // 这样生成的 MaterialTheme 会比较好看，而不是纯黑白
            Color(0xFF2E3192)
        } else {
            // 4. 【关键】色彩增强：提升 40% 饱和度
            // 这样 Repository 返回的颜色就是鲜艳的，生成的按钮和背景色调就统一了
            seed.boostSaturation(1.4f)
        }
    }
    private fun Color.boostSaturation(multiplier: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(this.toArgb(), hsl)
        hsl[1] = (hsl[1] * multiplier).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    fun getDbColor(url:String): Color? {
        return colorDao.getColor(url)?.let {
            Color(it.color)
        }
    }

    suspend fun insertColor(color: CacheColor) {
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

    suspend fun deleteSongById(id: String){
        qqSongDao.deleteSongById(id)
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
    suspend fun deletePlaylistById(id:String){
        playlistDao.deletePlaylistById(id)
    }
}


@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao: SongDao
) {

    // 添加歌曲到播放历史
    suspend fun addToHistory(song: Song) {
        try {
            historyDao.addSongToHistory(song)
        } catch (e: Exception) {
            // 如果外键约束失败，先插入歌曲再插入历史记录
            if (e is SQLiteConstraintException) {
                songDao.insertSongs(listOf(song))
                historyDao.insertHistory(
                    PlaybackHistory(
                        songId = song.id,
                        playedAt = System.currentTimeMillis()
                    )
                )
            } else {
                throw e
            }
        }
    }

    // 获取播放历史（Flow 版本，用于观察数据变化）
    fun getHistoryStream(): Flow<List<HistoryItem>> {
        return historyDao.getHistory()
    }

    // 获取播放历史（一次性获取）
    suspend fun getHistory(): List<HistoryItem> {
        return historyDao.getHistory().first()
    }

    // 清空播放历史
    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // 删除指定歌曲的播放历史
    suspend fun removeFromHistory(songId: String) {
        historyDao.deleteHistoryBySongId(songId)
    }

    // 批量添加歌曲到历史
    suspend fun addMultipleToHistory(songs: List<Song>) {
        songs.forEach { song ->
            addToHistory(song)
        }
    }

    // 获取最近播放的N首歌曲
    suspend fun getRecentSongs(limit: Int = 20): List<HistoryItem> {
        return historyDao.getHistory().first().take(limit)
    }

    // 检查歌曲是否在历史记录中
    suspend fun isSongInHistory(songId: String): Boolean {
        return historyDao.getHistory().first().any { it.song.id == songId }
    }

    // 获取历史记录数量
    suspend fun getHistoryCount(): Int {
        return historyDao.getHistory().first().size
    }
}