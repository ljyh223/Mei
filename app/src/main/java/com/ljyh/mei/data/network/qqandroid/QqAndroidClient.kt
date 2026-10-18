package com.ljyh.mei.data.network.qqandroid

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * QQ 音乐 **Android 客户端协议**（`musics.fcg` + sign/mask）客户端。
 *
 * 和项目里其它两条链路刻意分开，互不影响：
 *  - [com.ljyh.mei.data.network.XeApiClient]            —— 网易云 XEAPI（当前歌词主链路）
 *  - [com.ljyh.unblockneteasemusic.qq.QqMusicProvider]  —— QQ 旧版 `musicu.fcg`（cookie，无 sign/mask）
 *
 * 设备档案首次使用时合成并落到 DataStore，之后固定复用。
 */
@Singleton
class QqAndroidClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val transport = QqAndroidTransport()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val deviceLock = Mutex()

    @Volatile
    private var deviceCache: QqAndroidDevice? = null

    // ------------------------------------------------------------------ 公开 API
    suspend fun search(query: String, pageSize: Int = 15): List<QqSong> = withContext(Dispatchers.IO) {
        val param = QqAndroidParams.search(query, pageSize)
        val root = transport.call(ensureDevice(), QqAndroidParser.MOD_SEARCH, QqAndroidParser.MET_SEARCH, param)
        QqAndroidParser.parseSearch(root)
    }

    /**
     * 取歌词原始字段（不解码）。`qrc=1` 时 `lyric/roma` 是 hex 的 QRC 密文，
     * `qrc=0` 时是 base64 明文 LRC——两种都交给 [com.ljyh.unblockneteasemusic.qq.QRCUtils] 统一处理。
     */
    suspend fun lyricRaw(song: QqSong, preferWordSynced: Boolean = true): MusicLyrics =
        withContext(Dispatchers.IO) {
            val param = QqAndroidParams.lyric(song, preferWordSynced)
            val root = transport.call(ensureDevice(), QqAndroidParser.MOD_LYRIC, QqAndroidParser.MET_LYRIC, param)
            QqAndroidParser.parseLyricRaw(root)
        }

    /** 取歌词并解码：original 是 QRC 文本（逐字时间），translation/romanization 可能为空。 */
    suspend fun lyric(song: QqSong): MusicLyrics = withContext(Dispatchers.IO) {
        val param = QqAndroidParams.lyric(song)
        val root = transport.call(ensureDevice(), QqAndroidParser.MOD_LYRIC, QqAndroidParser.MET_LYRIC, param)
        QqAndroidParser.parseLyric(root)
    }

    /** 搜索 → 取第一首 → 取歌词。 */
    suspend fun lyricForQuery(query: String): Pair<QqSong, MusicLyrics>? {
        val first = try {
            search(query).firstOrNull()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "QqAndroid: search failed")
            return null
        } ?: return null
        val lyrics = try {
            lyric(first)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "QqAndroid: lyric failed")
            return null
        }
        return first to lyrics
    }

    // ------------------------------------------------------------------ 设备档案
    private suspend fun ensureDevice(): QqAndroidDevice = deviceCache ?: deviceLock.withLock {
        deviceCache ?: run {
            val stored = context.dataStore.data.first()[DEVICE_KEY]
            val device = stored?.let {
                runCatching { json.decodeFromString<QqAndroidDevice>(it) }.getOrNull()
            } ?: QqAndroidDevice.synthetic().also { dev ->
                context.dataStore.edit { it[DEVICE_KEY] = json.encodeToString(dev) }
                Timber.i("QqAndroid: generated device profile (uid=%s)", dev.uid)
            }
            deviceCache = device
            device
        }
    }

    companion object {
        private val DEVICE_KEY = stringPreferencesKey("qq_android_device_profile")
    }
}
