package com.ljyh.mei.di

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.paging.LOG_TAG
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.data.model.room.CacheColor
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.model.room.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
        Log.d("ColorRepository", "开始查询: $url")
        if (url.isEmpty()) return null
        Log.d("ColorRepository", "返回: ${memoryCache[url]}")
        Log.d("ColorRepository", "memoryCache: ${memoryCache}")
        return memoryCache[url]
    }

    /**
     * 2. 【挂起方法】完整流程：内存 -> 数据库 -> 网络提取
     * 专门给 ViewModel 或 LaunchedEffect 在后台使用。
     * 自动处理线程切换 (Dispatchers.IO)。
     */
    suspend fun getColorOrExtract(
        context: Context,
        url: String
    ): Color = withContext(Dispatchers.IO) { // 强制切换到 IO 线程
        if (url.isEmpty()) return@withContext Color.Black

        // A. 再次检查内存 (防止并发时别人已经加载好了)
        memoryCache[url]?.let {
            Log.d("ColorRepository", "内存中已经有了: $url")
            return@withContext it }

        // B. 查数据库 (L2)
        val dbEntity = colorDao.getColor(url)
        if (dbEntity != null) {
            Log.d("ColorRepository", "数据库中已经有了: $url")
            val color = Color(dbEntity.color)
            memoryCache[url] = color // 回填内存
            return@withContext color
        }
        Log.d("ColorRepository", "都没有，开始网络提取: $url")

        // C. 数据库没有 -> 网络下载并提取
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // 关硬件加速以读取像素
            .size(128)            // 读小图，提速
            .build()

        val result = loader.execute(request)
        val extractedColor = if (result is coil3.request.SuccessResult) {
            val bitmap = result.image.toBitmap()
            val palette = Palette.from(bitmap).generate()
            // 提取逻辑：鲜艳色 -> 主色 -> 黑色兜底
            val targetInt = palette.getVibrantColor(
                palette.getDominantColor(android.graphics.Color.BLACK)
            )
            Log.d("ColorRepository", "提取到颜色: $url -> $targetInt")
            Color(targetInt)
        } else {
            Log.d("ColorRepository", "网络请求失败: $url")
            Color.Black
        }

        // D. 存入 L1 内存
        memoryCache[url] = extractedColor

        // E. 存入 L2 数据库
        colorDao.insertColor(
            CacheColor(url = url, color = extractedColor.toArgb())
        )

        extractedColor
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