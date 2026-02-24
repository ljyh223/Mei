package com.ljyh.mei.utils.lyric

import android.content.Context
import android.widget.Toast
import com.ljyh.mei.constants.AutoMatchQQMusicLyricKey
import com.ljyh.mei.constants.MatchSuccessToastKey
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.utils.get
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.di.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.QRCUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricManager @Inject constructor(
    private val repository: PlayerRepository,
    private val qqSongRepository: QQSongRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _lyricData = MutableStateFlow(createDefaultLyricData("歌词加载中", source = LyricSource.Loading))
    val lyricData: StateFlow<LyricData> = _lyricData.asStateFlow()

    private val _qqSearchResult = MutableStateFlow<Resource<SearchResult>>(Resource.Loading)
    val qqSearchResult: StateFlow<Resource<SearchResult>> = _qqSearchResult.asStateFlow()

    private var currentSongId: String? = null
    private var fetchJob: Job? = null

    // Internal states for merging
    private val netLyricResult = MutableStateFlow<Resource<Lyric>>(Resource.Loading)
    private val qqLyricResult = MutableStateFlow<Resource<LyricResult>>(Resource.Loading)
    private val amLyricResult = MutableStateFlow<Resource<String>>(Resource.Loading)

    init {
        // Automatically merge whenever any source updates
        combine(netLyricResult, qqLyricResult, amLyricResult) { net, qq, am ->
            Triple(net, qq, am)
        }.onEach { (net, qq, am) ->
            mergeAndApply(net, qq, am)
        }.launchIn(scope)
    }

    /**
     * Called when the active song changes.
     */
    fun loadLyrics(metadata: MediaMetadata) {
        val songId = metadata.id.toString()
        if (currentSongId == songId) return

        currentSongId = songId
        fetchJob?.cancel()
        
        // Reset states for the new song
        _lyricData.value = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)
        netLyricResult.value = Resource.Loading
        qqLyricResult.value = Resource.Loading
        amLyricResult.value = Resource.Loading
        _qqSearchResult.value = Resource.Loading

        fetchJob = scope.launch {
            delay(100) // Debounce rapid switching
            
            // Fetch NetEase & AML in parallel
            launch { fetchNetEaseLyric(songId) }
            launch { fetchAMLLyric(songId) }

            // Handle QQ Music (DB first, then search)
            val useQQ = context.dataStore[UseQQMusicLyricKey] ?: true
            if (useQQ) {
                handleQQMusic(metadata)
            }
        }
    }

    private suspend fun fetchNetEaseLyric(id: String) {
        try {
            netLyricResult.value = repository.getLyricV1(id)
        } catch (e: Exception) {
            Timber.e(e, "NetEase fetch error")
            netLyricResult.value = Resource.Error("NetEase fetch failed")
        }
    }

    private suspend fun fetchAMLLyric(id: String) {
        try {
            amLyricResult.value = repository.getAMLLyric(id)
        } catch (e: Exception) {
            Timber.e(e, "AML fetch error")
            amLyricResult.value = Resource.Error("AML fetch failed")
        }
    }

    private suspend fun handleQQMusic(metadata: MediaMetadata) {
        // Check local database mapping
        val localSong = qqSongRepository.getQQSong(metadata.id.toString()).firstOrNull()
        if (localSong != null) {
            fetchQQLyric(localSong)
        } else {
            // Search for matches
            val result = repository.searchNew(metadata.title)
            _qqSearchResult.value = result
            
            val autoMatch = context.dataStore[AutoMatchQQMusicLyricKey] ?: false
            if (autoMatch && result is Resource.Success) {
                performAutoMatch(metadata, result.data)
            }
        }
    }

    private suspend fun performAutoMatch(metadata: MediaMetadata, searchData: SearchResult) {
        val songList = searchData.req0.data.body.song.list
        if (songList.isEmpty()) return

        val result = withContext(Dispatchers.Default) {
            val targetInfo = LyricMatchAlgorithm.SongMatchInfo(
                id = metadata.id.toString(),
                title = metadata.title,
                artist = metadata.artists.joinToString(",") { it.name },
                album = metadata.album.title,
                duration = metadata.duration
            )

            val candidates = songList.map { song ->
                LyricMatchAlgorithm.SongMatchInfo(
                    id = song.id.toString(),
                    title = song.title,
                    artist = song.singer.joinToString(",") { it.name },
                    album = song.album.title,
                    duration = (song.interval * 1000)
                )
            }

            LyricMatchAlgorithm.findBestMatch(targetInfo, candidates, 75f)
        }

        result?.let { match ->
            val matchedSong = songList.find { it.id.toString() == match.song.id }
            matchedSong?.let { s ->
                val qqSong = QQSong(
                    id = metadata.id.toString(),
                    qid = s.id.toString(),
                    title = s.title,
                    artist = s.singer.joinToString(",") { it.name },
                    album = s.album.title,
                    duration = s.interval * 1000
                )
                // For auto-match, we just fetch without forcing DB entry (user can select manually to fix)
                fetchQQLyric(qqSong)
                
                if (context.dataStore[MatchSuccessToastKey] ?: true) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "已自动匹配: ${s.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun fetchQQLyric(song: QQSong) {
        scope.launch {
            qqLyricResult.value = Resource.Loading
            try {
                qqLyricResult.value = repository.getLyricNew(
                    song.title, song.album, song.artist, song.duration, song.qid.toLong()
                )
            } catch (e: Exception) {
                Timber.e(e, "QQ fetch error")
                qqLyricResult.value = Resource.Error("QQ fetch failed")
            }
        }
    }

    private suspend fun mergeAndApply(
        net: Resource<Lyric>,
        qq: Resource<LyricResult>,
        am: Resource<String>
    ) {
        val songIdAtStart = currentSongId // Capture current state
        
        val merged = withContext(Dispatchers.IO) {
            val isPureMusic = (net as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()
            
            (am as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (net as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qq as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let { data ->
                try {
                    val decoded = data.copy(
                        lyric = QRCUtils.decodeLyric(data.lyric),
                        trans = QRCUtils.decodeLyric(data.trans, true),
                        roma = QRCUtils.decodeLyric(data.roma)
                    )
                    sources.add(LyricSourceData.QQMusic(decoded))
                } catch (e: Exception) {
                    Timber.e(e, "QRC decoding failed")
                }
            }
            
            mergeLyrics(sources, isPureMusic)
        }

        // Verify song hasn't changed before updating UI
        if (currentSongId == songIdAtStart) {
            _lyricData.value = merged
        }
    }

    /**
     * User manually selects a lyric entry.
     */
    fun selectQQSongForLyric(metadata: MediaMetadata, song: SearchResult.Req0.Data.Body.Song.S) {
        scope.launch {
            val qqSong = QQSong(
                id = metadata.id.toString(),
                qid = song.id.toString(),
                title = song.title,
                artist = song.singer.joinToString(",") { it.name },
                album = song.album.title,
                duration = song.interval
            )
            qqSongRepository.insertSong(qqSong) // Save to DB for future use
            fetchQQLyric(qqSong)
        }
    }

    fun cancelAll() {
        fetchJob?.cancel()
        currentSongId = null
    }
}
