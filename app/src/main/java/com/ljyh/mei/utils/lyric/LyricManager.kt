package com.ljyh.mei.utils.lyric

import android.content.Context
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.di.QQSongRepository
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
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
        combine(netLyricResult, qqLyricResult, amLyricResult) { net, qq, am ->
            Triple(net, qq, am)
        }.onEach { (net, qq, am) ->
            mergeAndApply(net, qq, am)
        }.launchIn(scope)
    }

    fun loadLyrics(metadata: MediaMetadata) {
        val songId = metadata.id.toString()
        if (currentSongId == songId) return

        currentSongId = songId
        fetchJob?.cancel()

        _lyricData.value = createDefaultLyricData("歌词加载中", source = LyricSource.Loading)
        netLyricResult.value = Resource.Loading
        qqLyricResult.value = Resource.Loading
        amLyricResult.value = Resource.Loading
        _qqSearchResult.value = Resource.Loading
        lrcFallbackContent = null

        fetchJob = scope.launch {
            delay(100)

            launch { fetchNetEaseLyric(songId) }
            launch { fetchAMLLyric(songId) }

            // Load cached QQ song mapping if available
            val localSong = qqSongRepository.getQQSong(songId).firstOrNull()
            if (localSong != null) {
                fetchQQLyric(localSong)
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

    private var currentQQSong: QQSong? = null

    fun fetchQQLyric(song: QQSong) {
        scope.launch {
            currentQQSong = song
            qqLyricResult.value = Resource.Loading
            try {
                val result = repository.getLyricNew(
                    song.title, song.album, song.artist, song.duration, song.qid.toLong()
                )
                qqLyricResult.value = result
                if (result is Resource.Success) {
                    val qrcT = result.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data.qrcT
                    if (qrcT != 0) {
                        fetchQQLyricLrc(song)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "QQ fetch error")
                qqLyricResult.value = Resource.Error("QQ fetch failed")
            }
        }
    }

    private suspend fun fetchQQLyricLrc(song: QQSong) {
        try {
            val lrcResult = repository.getLyricLrc(
                song.title, song.album, song.artist, song.duration, song.qid.toLong()
            )
            if (lrcResult is Resource.Success) {
                val lrcContent = QRCUtils.decodeLyric(
                    lrcResult.data.musicMusichallSongPlayLyricInfoGetPlayLyricInfo.data.lyric
                )
                lrcFallbackContent = lrcContent
                remergeLyrics()
            }
        } catch (e: Exception) {
            Timber.e(e, "QQ LRC fallback fetch error")
        }
    }

    private var lrcFallbackContent: String? = null

    private fun remergeLyrics() {
        scope.launch {
            mergeAndApply(
                netLyricResult.value,
                qqLyricResult.value,
                amLyricResult.value
            )
        }
    }

    private suspend fun mergeAndApply(
        net: Resource<Lyric>,
        qq: Resource<LyricResult>,
        am: Resource<String>
    ) {
        val songIdAtStart = currentSongId

        val merged = withContext(Dispatchers.IO) {
            val isPureMusic = (net as? Resource.Success)?.data?.pureMusic == true
            val sources = mutableListOf<LyricSourceData>()

            (am as? Resource.Success)?.let { sources.add(LyricSourceData.AM(it.data)) }
            (net as? Resource.Success)?.data?.let { sources.add(LyricSourceData.NetEase(it)) }
            (qq as? Resource.Success)?.data?.musicMusichallSongPlayLyricInfoGetPlayLyricInfo?.data?.let { data ->
                try {
                    val isQRC = data.qrcT != 0
                    val decoded = data.copy(
                        lyric = QRCUtils.decodeLyric(data.lyric),
                        trans = QRCUtils.decodeLyric(data.trans, true),
                        roma = QRCUtils.decodeLyric(data.roma)
                    )
                    sources.add(LyricSourceData.QQMusic(decoded, isQRC, lrcFallbackContent))
                } catch (e: Exception) {
                    Timber.e(e, "QRC decoding failed")
                }
            }

            mergeLyrics(sources, isPureMusic)
        }

        if (currentSongId == songIdAtStart) {
            _lyricData.value = merged
        }
    }

    fun searchQQSong(keyword: String) {
        scope.launch {
            _qqSearchResult.value = Resource.Loading
            _qqSearchResult.value = repository.searchNew(keyword)
        }
    }

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
            qqSongRepository.insertSong(qqSong)
            fetchQQLyric(qqSong)
        }
    }

    fun cancelAll() {
        fetchJob?.cancel()
        currentSongId = null
    }
}
