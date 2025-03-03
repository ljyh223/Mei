package com.ljyh.music.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ljyh.music.MainActivity
import com.ljyh.music.R
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.data.model.api.GetSongDetails
import com.ljyh.music.data.model.api.GetSongUrlV1
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.model.toMediaItem
import com.ljyh.music.data.network.api.ApiService
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.di.AppDatabase
import com.ljyh.music.extensions.SilentHandler
import com.ljyh.music.extensions.currentMetadata
import com.ljyh.music.playback.queue.EmptyQueue
import com.ljyh.music.playback.queue.Queue
import com.ljyh.music.utils.CoilBitmapLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import kotlin.collections.List
import kotlin.collections.drop
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.take
import kotlin.collections.toMutableList


@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {

    @Inject
    lateinit var database: AppDatabase
    lateinit var player: ExoPlayer
    private lateinit var audioPlayer: AudioPlayer
    val context = this
    private lateinit var mediaSession: MediaLibrarySession

    private lateinit var sleepTimer: SleepTimer
    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private lateinit var connectivityManager: ConnectivityManager
    val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaMetadata.flatMapLatest { mediaMetadata: MediaMetadata? ->
        database.songDao().getSong((mediaMetadata?.id ?: 0).toString())
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val binder = MusicBinder()

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null
    private var isAudioEffectSessionOpened = false

    @Inject
    lateinit var weApiService: WeApiService // 自动注入

    @Inject
    lateinit var apiService: ApiService

    private val songUrlCache = mutableMapOf<String, SongCache>()

    override fun onCreate() {
        super.onCreate()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
        )

        player = ExoPlayer.Builder(this)
            //媒体源工厂
            .setMediaSourceFactory(createMediaSourceFactory())
            //渲染器工厂
            .setRenderersFactory(createRenderersFactory())
            //处理音频焦点变化和音频播放行为
            .setHandleAudioBecomingNoisy(true)
            //设置音频的唤醒模式，保证设备在网络连接上不进入休眠状态。
            //它并不会阻止屏幕变暗或关闭；它主要是为了防止CPU进入睡眠状态以及确保网络连接的活跃，从而避免播放中断。
            .setWakeMode(C.WAKE_MODE_NETWORK)
            //音频属性
            .setAudioAttributes(
                AudioAttributes.Builder()
                    //音频焦点变化 比如来电话了
                    .setUsage(C.USAGE_MEDIA)
                    //AUDIO_CONTENT_TYPE_MUSIC 表明内容类型是音乐。这有助于系统选择合适的音频处理方式和路由（比如通过扬声器还是耳机输出）。
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            //快进快退时间
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                //添加监听器
                addListener(this@MusicService)
                //睡眠定时
                sleepTimer = SleepTimer(scope, this)
//                addListener(sleepTimer)
                //播放统计
                addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
            }



        audioPlayer = AudioPlayer(player)

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(
                //当用户在媒体通知或锁屏上点击时，可以启动指定的 MainActivity
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            //设置位图加载器
            .setBitmapLoader(CoilBitmapLoader(this))
            .build()

        player.repeatMode = REPEAT_MODE_OFF

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())


        connectivityManager = getSystemService(ConnectivityManager::class.java)


    }



    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                openAudioEffectSession()
                audioPlayer.startSmooth()
            } else {
                closeAudioEffectSession()
                audioPlayer.pauseSmooth()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }


    class LibrarySessionCallback : MediaLibrarySession.Callback {


    }

    fun playNext(items: List<MediaItem>) {
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            items
        )
        player.prepare()
    }

    fun toggleLike(id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val like = database.likeDao().getLike(id) == null
                Log.d("like", "toggleLike: $like")
                if (!like) database.likeDao().deleteLike(id) else database.likeDao()
                    .insertLike(Like(id))
                weApiService.like(
                    com.ljyh.music.data.model.weapi.Like(
                        trackId = id,
                        like = like
                    )
                )
            }
        }
    }

    suspend fun isLike(id: String) = withContext(Dispatchers.IO) {
        database.likeDao().getLike(id) != null
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }



    fun playQueue(queue: Queue, playWhenReady: Boolean = true, batchSize: Int = 10) {
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main) + Job()
        }
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false

        scope.launch(SilentHandler) {
            val initialStatus = queue.getInitialStatus()
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }


            val firstBatch = initialStatus.ids.take(batchSize)
            val remainingItems = initialStatus.ids.drop(batchSize)

            val firstItems =
                apiService.getSongDetail(
                    GetSongDetails(
                        c = firstBatch.joinToString(",")
                    )
                ).songs.map {
                    it.toMediaItem()
                }
            // 设置第一批歌曲
            player.setMediaItems(firstItems, 0, initialStatus.position)
            player.prepare()
            player.playWhenReady = playWhenReady

            // 监听播放进度，动态加载后续歌曲
            monitorPlayback(remainingItems, batchSize)
        }
    }

    private fun monitorPlayback(remainingItems: List<String>, batchSize: Int) {
        var remaining = remainingItems.toMutableList()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val lastIndex = player.currentMediaItemIndex
                val totalItems = player.mediaItemCount

                // 当播放到倒数第 2 首时，加载下一批
                if (lastIndex >= totalItems - 2 && remaining.isNotEmpty()) {
                    val nextBatch = remaining.take(batchSize)
                    scope.launch {
                        val nextItem =
                            apiService.getSongDetail(GetSongDetails(c = nextBatch.joinToString(","))).songs.map {
                                it.toMediaItem()
                            }
                        remaining = remaining.drop(batchSize).toMutableList()
                        player.addMediaItems(nextItem)
                    }

                }
            }
        })
    }

    override fun onDestroy() {
        CacheManager.release()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {

    }

    private fun createCacheDataSource(): CacheDataSource.Factory {
        Log.d("SimpleCache", "Creating CacheDataSource instance")
        val simpleCache = CacheManager.getSimpleCache(context)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this,
                    OkHttpDataSource.Factory(OkHttpClient.Builder().build())
                )
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    private fun createDataSourceFactory(): DataSource.Factory {

        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            val localFilePath = runBlocking {
                database.songDao().getSong(mediaId).firstOrNull()?.path
            }
            if (localFilePath != null) {
                val file = File(localFilePath)
                if (file.exists()) {
                    Log.d(
                        "ResolvingDataSource",
                        "Using local file for mediaId: $mediaId, filePath: ${file.path}"
                    )
                    return@Factory dataSpec.withUri(Uri.fromFile(file))
                }
            }


            // 检查缓存
            songUrlCache[mediaId]?.takeIf { it.expiryTime > System.currentTimeMillis() }?.let {
                Log.d("ResolvingDataSource", "Using cached URL for mediaId: $mediaId")
                return@Factory dataSpec.withUri(it.url.toUri())
            }

            val deferredUrl = CoroutineScope(Dispatchers.IO).async {
                try {
                    val response = apiService.getSongUrlV1(GetSongUrlV1(
                        ids = "[$mediaId]",
                    ))
                    response.data.getOrNull(0)?.url
                } catch (e: Exception) {
                    Log.e("ResolvingDataSource", "Failed to fetch media URL", e)
                    null
                }
            }
            val url = runBlocking { deferredUrl.await() } ?: run {
                Log.w("ResolvingDataSource", "No valid URL for mediaId: $mediaId")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "暂无音源", Toast.LENGTH_SHORT).show()
                }
                return@Factory dataSpec
            }
            // 存储到缓存
            val expiryTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24小时有效
            songUrlCache[mediaId] = SongCache(url, expiryTime)
            Log.d("ResolvingDataSource", "Resolved media URL for mediaId: $mediaId, URL: $url")

            dataSpec.withUri(Uri.parse(url))
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory()

        )


    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink.Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 0.01f, 2_000_000, 0, 256),
                        SonicAudioProcessor()
                    )
                )
                .build()
        }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    data class SongCache(val url: String, val expiryTime: Long)

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
    }
}