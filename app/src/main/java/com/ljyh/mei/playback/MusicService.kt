package com.ljyh.mei.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.ContentMetadata
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
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import coil3.ImageLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ljyh.mei.MainActivity
import com.ljyh.mei.R
import com.ljyh.mei.constants.IsShuffleModeKey
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.LastPlaybackQueueKey
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.constants.RepeatModeKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.SongEntity
import com.ljyh.mei.data.model.createPlaceholder
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.di.repository.HistoryRepository
import com.ljyh.mei.di.repository.SongRepository
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.CacheManager.getCacheDataSourceFactory
import com.ljyh.mei.playback.CacheManager.isContentFullyCached
import com.ljyh.mei.playback.equalizer.ParametricEqualizerProcessor
import com.ljyh.mei.playback.equalizer.EqualizerProfile
import com.ljyh.mei.utils.CoilBitmapLoader
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.lyric.LyricManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Locale.getDefault
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {

    lateinit var player: TransitionDeckPlayer
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var audioEffectsController: AudioEffectsController
    private lateinit var standbyAudioEffectsController: AudioEffectsController
    private lateinit var transitionController: SmartTransitionController
    private lateinit var desktopLyricsController: DesktopLyricsController
    private val parametricEqualizerProcessor = ParametricEqualizerProcessor()
    private val standbyEqualizerProcessor = ParametricEqualizerProcessor()
    private val firstTransitionProbe = TransitionAudioProbe()
    private val secondTransitionProbe = TransitionAudioProbe()
    val context = this
    private lateinit var mediaSession: MediaLibrarySession
    private val desktopLyricsToggleCommand = SessionCommand(DESKTOP_LYRICS_TOGGLE_ACTION, Bundle.EMPTY)
    @Volatile private var desktopLyricsEnabled = false

    lateinit var sleepTimer: SleepTimer
    private val serviceJob = SupervisorJob()
    var scope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var historyJob: Job? = null
    private var queueSaveJob: Job? = null
    private var restoringQueue = true
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var baseMediaSourceFactory: DefaultMediaSourceFactory
    val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)

    @Inject
    lateinit var mediaUriProvider: MediaUriProvider

    private var consecutiveFailedItems = 0
    private var retriedMediaId: String? = null

    private val binder = MusicBinder()

    lateinit var queueManager: PlaybackQueueManager
    var queueTitle: String? = null

    @Inject
    lateinit var weApiService: WeApiService
    @Inject
    lateinit var apiService: ApiService


    @Inject
    lateinit var historyRepository: HistoryRepository

    @Inject
    lateinit var songRepository: SongRepository
    @Inject
    lateinit var lyricManager: LyricManager
    override fun onCreate() {
        super.onCreate()
        baseMediaSourceFactory = DefaultMediaSourceFactory(createDataSourceFactory())
            .setLoadErrorHandlingPolicy(MusicLoadErrorHandlingPolicy()) // 应用自定义错误策略

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.app_name_en
            )
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        fun buildDeck(processor: ParametricEqualizerProcessor, probe: TransitionAudioProbe, focused: Boolean) =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(baseMediaSourceFactory)
                .setRenderersFactory(createRenderersFactory(processor, probe))
                .setHandleAudioBecomingNoisy(focused)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(audioAttributes, focused)
                .setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build().apply {
                    setPreloadConfiguration(ExoPlayer.PreloadConfiguration(PLAYLIST_PRELOAD_DURATION_US))
                }
        val firstDeck = buildDeck(parametricEqualizerProcessor, firstTransitionProbe, true)
        val secondDeck = buildDeck(standbyEqualizerProcessor, secondTransitionProbe, false)
        player = TransitionDeckPlayer(firstDeck, secondDeck, audioAttributes).apply {
                addListener(this@MusicService)
                sleepTimer = SleepTimer(scope, this)
                addListener(sleepTimer)
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        historyJob?.cancel()
                        if (mediaItem != null) {
                            historyJob = scope.launch {
                                delay(5000L.milliseconds)
                                try {
                                    recordHistory(mediaItem)
                                } catch (e: Exception) {
                                    Timber.tag("MusicService").e("add history record error $e")
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        scope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[IsShuffleModeKey] = shuffleModeEnabled
                            }
                        }
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        scope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[RepeatModeKey] = repeatMode
                            }
                        }
                    }
                })
            }




        desktopLyricsController = DesktopLyricsController(this, player, lyricManager, scope)
        desktopLyricsController.start()
        audioEffectsController = AudioEffectsController(this, scope, parametricEqualizerProcessor)
        standbyAudioEffectsController = AudioEffectsController(this, scope, standbyEqualizerProcessor)

        audioPlayer = AudioPlayer(player)
        transitionController = SmartTransitionController(
            this, player,
            mapOf(firstDeck to firstTransitionProbe, secondDeck to secondTransitionProbe),
            scope, audioPlayer::cancelFade,
            { !sleepTimer.pauseWhenSongEnd },
            { mediaId ->
                val quality = dataStore[MusicQualityKey]?.lowercase(getDefault()) ?: MusicQuality.EXHIGH.text
                mediaUriProvider.resolveMediaUri(mediaId, quality)
            },
        )
        val singletonImageLoader = ImageLoader(this)
        val sessionPlayer = object : ForwardingSimpleBasePlayer(player) {
            override fun handleSeek(
                mediaItemIndex: Int,
                positionMs: Long,
                seekCommand: Int
            ): ListenableFuture<*> {
                // System media controls map "previous" to seekToPrevious(), whose default
                // behavior restarts the current item after the seek threshold.
                if (seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS) {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    } else {
                        player.seekTo(0)
                    }
                    return Futures.immediateVoidFuture()
                }
                return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
            }
        }
        mediaSession = MediaLibrarySession.Builder(this, sessionPlayer, LibrarySessionCallback())
            .setMediaButtonPreferences(listOf(desktopLyricsButton(enabled = false)))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(CoilBitmapLoader(this, singletonImageLoader))
            .build()

        scope.launch {
            dataStore.data
                .map { it[DesktopLyricsEnabledKey] ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    desktopLyricsEnabled = enabled
                    updateDesktopLyricsNotificationButton()
                }
        }


        // The manager must exist before restored items publish timeline events.
        queueManager = PlaybackQueueManager(player, apiService, weApiService, scope)
        restorePlayerState()
        scope.launch {
            while (isActive) {
                delay(10_000)
                if (!restoringQueue && player.mediaItemCount > 0) savePlaybackQueue()
            }
        }
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService(ConnectivityManager::class.java)

    }

    private fun restorePlayerState() {
        scope.launch {
            try {
                val preferences = context.dataStore.data.firstOrNull() ?: return@launch
                val snapshot = PlaybackQueueSnapshotCodec.decode(preferences[LastPlaybackQueueKey])
                // A user action made while the service was starting takes precedence over the snapshot.
                val playerWasEmpty = player.mediaItemCount == 0
                if (playerWasEmpty && snapshot != null) {
                    queueManager.isFmMode = snapshot.isFmMode
                    val items = snapshot.ids.mapIndexed { index, id ->
                        if (index == snapshot.currentIndex && snapshot.currentSong != null) {
                            snapshot.currentSong.toMediaItem()
                        } else {
                            createPlaceholder(id)
                        }
                    }
                    player.setMediaItems(items, snapshot.currentIndex, snapshot.positionMs)
                    player.playWhenReady = false
                    player.prepare()
                    currentMediaMetadata.value = player.currentMetadata
                }

                if (playerWasEmpty) {
                    val savedShuffleMode = !queueManager.isFmMode && (preferences[IsShuffleModeKey] ?: false)
                    val savedRepeatMode = if (queueManager.isFmMode) Player.REPEAT_MODE_ALL
                        else preferences[RepeatModeKey] ?: Player.REPEAT_MODE_ALL
                    player.repeatMode = savedRepeatMode
                    player.shuffleModeEnabled = savedShuffleMode
                    queueManager.setShuffleModeEnabled(savedShuffleMode)
                    Timber.tag("MusicService").d(
                        "Restored queue: ${snapshot?.ids?.size ?: 0}, FM: ${queueManager.isFmMode}, shuffle: $savedShuffleMode, repeat: $savedRepeatMode"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Failed to restore player state")
            } finally {
                restoringQueue = false
            }
        }
    }

    private fun scheduleQueueSave() {
        if (restoringQueue) return
        queueSaveJob?.cancel()
        queueSaveJob = scope.launch {
            delay(500)
            savePlaybackQueue()
        }
    }

    private suspend fun savePlaybackQueue() {
        val snapshot = if (player.mediaItemCount > 0 && player.currentMediaItemIndex >= 0) {
            PlaybackQueueSnapshot(
                ids = player.mediaItems.map(MediaItem::mediaId),
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                currentSong = snapshotCurrentSong(),
                isFmMode = queueManager.isFmMode,
            )
        } else null
        try {
            context.dataStore.edit { preferences ->
                if (snapshot == null) preferences.remove(LastPlaybackQueueKey)
                else preferences[LastPlaybackQueueKey] = PlaybackQueueSnapshotCodec.encode(snapshot)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag("MusicService").e(e, "Failed to save playback queue")
        }
    }

    private fun snapshotCurrentSong(): MediaMetadata? {
        val item = player.currentMediaItem ?: return null
        val current = player.currentMetadata ?: (item.localConfiguration?.tag as? SongEntity)?.let {
            MediaMetadata(
                id = it.id,
                title = it.title,
                coverUrl = it.coverUrl,
                artists = listOf(MediaMetadata.Artist(it.artistId, it.artistName)),
                duration = it.duration,
                album = MediaMetadata.Album(it.albumId, it.albumName),
            )
        }
        return current?.takeIf { it.id.toString() == item.mediaId }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (!transitionController.isTransitioning && events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                audioPlayer.startSmooth()
            } else {
                audioPlayer.pauseSmooth()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            scheduleQueueSave()
        }
    }


    private suspend fun recordHistory(mediaItem: MediaItem) {
        val metadata = mediaItem.mediaMetadata
        val artistList = metadata.extras?.getStringArrayList("artist_list") ?: listOf("未知歌手")
        val song = Song(
            id = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "未知标题",
            artist = artistList,
            album = metadata.albumTitle?.toString() ?: "未知专辑",
            cover = metadata.artworkUri?.toString() ?: "",
            duration = metadata.durationMs ?: 0,
        )
        historyRepository.addToHistory(song)
    }


    private fun desktopLyricsButton(enabled: Boolean) =
        CommandButton.Builder(
            if (enabled) CommandButton.ICON_SUBTITLES else CommandButton.ICON_SUBTITLES_OFF,
        )
            .setDisplayName(if (enabled) "关闭桌面歌词" else "显示桌面歌词")
            .setSessionCommand(desktopLyricsToggleCommand)
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .build()

    private fun updateDesktopLyricsNotificationButton() {
        if (::mediaSession.isInitialized) {
            mediaSession.setMediaButtonPreferences(listOf(desktopLyricsButton(desktopLyricsEnabled)))
        }
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val defaultSessionCommands = if (controller.isTrusted) {
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            } else {
                MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_SESSION_AND_LIBRARY_COMMANDS
            }
            val defaultPlayerCommands = if (controller.isTrusted) {
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            } else {
                MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS
            }
            val availableSessionCommands = SessionCommands.Builder()
                .addSessionCommands(defaultSessionCommands.commands)
                .add(desktopLyricsToggleCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(defaultPlayerCommands)
                .setMediaButtonPreferences(listOf(desktopLyricsButton(desktopLyricsEnabled)))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
            if (customCommand.customAction != DESKTOP_LYRICS_TOGGLE_ACTION) {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            scope.launch {
                desktopLyricsEnabled = !(dataStore.data.first()[DesktopLyricsEnabledKey] ?: false)
                dataStore.edit { it[DesktopLyricsEnabledKey] = desktopLyricsEnabled }
                updateDesktopLyricsNotificationButton()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    fun playNext(items: List<MediaItem>) {
        scope.launch {
            queueManager.playNext(items)
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        scope.launch {
            queueManager.addToQueue(items)
        }
    }

    fun setShuffleModeEnabled(isShuffle: Boolean) {
        queueManager.setShuffleModeEnabled(isShuffle)
    }

    fun previewEqualizerProfile(profile: EqualizerProfile) {
        parametricEqualizerProcessor.setProfile(profile)
        standbyEqualizerProcessor.setProfile(profile)
    }

    override fun onDestroy() {
        queueSaveJob?.cancel()
        if (!restoringQueue) {
            runBlocking { savePlaybackQueue() }
        }
        transitionController.release()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        queueManager.release()
        audioPlayer.release()
        audioEffectsController.release()
        standbyAudioEffectsController.release()
        desktopLyricsController.close()
        player.release()
        CacheManager.release()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {

    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val simpleCache = CacheManager.getSimpleCache(context)

        return ResolvingDataSource.Factory(getCacheDataSourceFactory(context)) { dataSpec ->
            runBlocking {
                val quality = context.dataStore[MusicQualityKey]?.lowercase(getDefault()) ?: MusicQuality.EXHIGH.text
                val mediaId = dataSpec.key ?: throw SourceNotFoundException("Missing media cache key")
                val cacheKey = "$mediaId|$quality"
                if (isContentFullyCached(simpleCache, cacheKey)) {
                    Timber.tag("ResolvingDataSource").d("Fully cached on disk: $cacheKey")
                    return@runBlocking dataSpec.buildUpon().setKey(cacheKey).build()
                }
                val uri = mediaUriProvider.resolveMediaUri(mediaId, quality)
                dataSpec.buildUpon().setUri(uri).setKey(cacheKey).build()
            }
        }
    }

    private fun createRenderersFactory(
        equalizer: ParametricEqualizerProcessor,
        probe: TransitionAudioProbe,
    ) =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink.Builder(this@MusicService)
                // Media3 bypasses user audio processors for high-resolution float output.
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        arrayOf(equalizer, probe),
                        SilenceSkippingAudioProcessor(2_000_000, 0.01f, 2_000_000, 0, 256),
                        SonicAudioProcessor()
                    )
                ).build()
        }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder
    override fun onPlayerError(error: PlaybackException) {
        Timber.tag("MusicService").e( "Player Error: ${error.errorCodeName}, ${error.message}")
        val mediaId = player.currentMediaItem?.mediaId
        if (isSourceError(error) && mediaId != null) {
            // A signed URL may have expired between preloading and playback. Refresh it once for
            // the same item before deciding the item is unavailable.
            if (retriedMediaId != mediaId) {
                retriedMediaId = mediaId
                mediaUriProvider.invalidateRemoteUrl(mediaId)
                Timber.tag("MusicService").i("Refreshing playback URL for $mediaId")
                player.prepare()
                player.play()
                return
            }

            consecutiveFailedItems++
            Timber.tag("MusicService").e("Play failure detected. Count: $consecutiveFailedItems")
            if (consecutiveFailedItems > MAX_CONSECUTIVE_FAILED_ITEMS) {
                Toast.makeText(context, "播放失败，已连续跳过多首歌曲", Toast.LENGTH_LONG).show()
                player.stop()
                retriedMediaId = null
                return
            }

            // 尝试跳到下一首
            if (player.hasNextMediaItem()) {
                // 不要在后台线程 Toast，发送事件或者只打印日志
                // 如果非要提示，用 Handler
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "资源无法加载，自动跳过", Toast.LENGTH_SHORT).show()
                }
                player.seekToNext()
                player.prepare()
                player.play()
            } else {
                // 列表播完了，或者没有下一首
                player.stop()
                Toast.makeText(context, "播放结束，部分歌曲无法加载", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "播放出错: ${error.errorCodeName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isSourceError(error: PlaybackException): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SourceNotFoundException || cause is java.io.IOException) return true
            cause = cause.cause
        }
        return error.errorCodeName.startsWith("ERROR_CODE_IO_")
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && player.playerError == null) {
            consecutiveFailedItems = 0
            retriedMediaId = null
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            checkFmModeLoadMore()
        }
    }

    private fun checkFmModeLoadMore() {
        if (!queueManager.isFmMode) return

        val current = player.currentMediaItemIndex
        val total = player.mediaItemCount
        val threshold = 3 // 剩余少于3首时加载

        if (total - current <= threshold) {
            scope.launch {
                queueManager.fetchAndAppendFmRecommendations()
            }
        }
    }

    fun isFmMode(): Boolean {
        return queueManager.isFmMode
    }

    companion object {
        private const val DESKTOP_LYRICS_TOGGLE_ACTION = "com.ljyh.mei.action.TOGGLE_DESKTOP_LYRICS"
        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        private const val PLAYLIST_PRELOAD_DURATION_US = 5_000_000L
        private const val MAX_CONSECUTIVE_FAILED_ITEMS = 5
    }
}
