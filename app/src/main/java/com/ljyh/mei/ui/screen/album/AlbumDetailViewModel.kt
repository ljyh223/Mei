package com.ljyh.mei.ui.screen.album

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.DynamicCoverSources
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.data.repository.DynamicCoverRepository
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.AlbumContent
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.AlbumSubscriptionRepository
import com.ljyh.mei.data.repository.PlaylistRepository
import com.ljyh.mei.di.repository.ColorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val albumSubscriptions: AlbumSubscriptionRepository,
    private val colorRepository: ColorRepository,
    private val dynamicCoverRepository: DynamicCoverRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _albumDetail = MutableStateFlow<Resource<AlbumContent>>(Resource.Loading)
    val albumDetail: StateFlow<Resource<AlbumContent>> = _albumDetail

    private val _subscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val subscribeAlbum: StateFlow<Resource<BaseResponse>> = _subscribeAlbum

    private val _unSubscribeAlbum = MutableStateFlow<Resource<BaseResponse>>(Resource.Loading)
    val unSubscribeAlbum: StateFlow<Resource<BaseResponse>> = _unSubscribeAlbum

    private val _subscriptionState = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val subscriptionState: StateFlow<Resource<Boolean>> = _subscriptionState
    private val _subscriptionPending = MutableStateFlow(false)
    val subscriptionPending: StateFlow<Boolean> = _subscriptionPending
    private var subscriptionLoadJob: Job? = null


    private val _motionArtwork = MutableStateFlow<DynamicCover?>(null)
    val motionArtwork: StateFlow<DynamicCover?> = _motionArtwork
    private var artworkJob: Job? = null
    private var artworkRequest: Triple<AlbumContent?, DynamicCoverSources, Boolean>? = null

    internal fun loadMotionArtwork(
        content: AlbumContent?,
        mode: AlbumArtworkMode,
        sources: DynamicCoverSources,
        englishOnly: Boolean,
        allowMotion: Boolean = true,
    ) {
        val enabled = allowMotion && (mode == AlbumArtworkMode.Auto || mode == AlbumArtworkMode.Motion)
        // NetEase motion belongs to a song; Apple motion identifies the album itself.
        val albumSources = DynamicCoverSources(appleMusic = sources.appleMusic, netease = false)
        val request = Triple(content, if (enabled) albumSources else DynamicCoverSources.Disabled, englishOnly)
        if (artworkRequest == request) return
        artworkRequest = request
        artworkJob?.cancel()
        _motionArtwork.value = null
        if (content == null || !request.second.anyEnabled) return
        val first = content.songs.firstOrNull() ?: return
        // Album artists identify the release, not a guest credited on the first track.
        val sample = first.copy(
            title = content.album.name,
            coverUrl = content.album.picUrl,
            album = MediaMetadata.Album(content.album.id, content.album.name),
            artists = content.album.artistNames.mapIndexed { index, name ->
                MediaMetadata.Artist(content.album.artistIds.getOrNull(index) ?: 0, name)
            }.ifEmpty { first.artists },
        )
        artworkJob = viewModelScope.launch {
            try {
                val cover = dynamicCoverRepository.resolve(sample, englishOnly, request.second)
                if (artworkRequest == request) _motionArtwork.value = cover
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Decoration cannot fail the album or hold up its tracks.
            }
        }
    }

    fun getAlbumDetail(id: String) {
        viewModelScope.launch {
            _albumDetail.value = Resource.Loading
            _albumDetail.value = repository.getAlbumDetail(id)
        }
    }

    fun subscribeAlbum(album: AlbumOverview) = updateSubscription(album.id.toString(), true)

    fun unSubscribeAlbum(id: String) = updateSubscription(id, false)

    private fun updateSubscription(id: String, subscribed: Boolean) {
        if (_subscriptionState.value !is Resource.Success || _subscriptionPending.value) return
        _subscriptionPending.value = true
        viewModelScope.launch {
            try {
                if (subscribed) _subscribeAlbum.value = Resource.Loading
                else _unSubscribeAlbum.value = Resource.Loading
                // Only server-confirmed changes enter Room; its observation updates the button.
                val result = albumSubscriptions.setSubscribed(id.toLong(), subscribed)
                if (subscribed) _subscribeAlbum.value = result else _unSubscribeAlbum.value = result
            } finally {
                _subscriptionPending.value = false
            }
        }
    }

    fun isSubscribe(id: Long) {
        subscriptionLoadJob?.cancel()
        subscriptionLoadJob = viewModelScope.launch {
            albumSubscriptions.observe(id).collect { _subscriptionState.value = it }
        }
    }

    fun cachedCoverColor(url: String): Color? = colorRepository.getFromMemory(url)

    suspend fun coverColor(url: String, image: coil3.Image): Color =
        colorRepository.getColorFromLoadedArtwork(url, image)

    suspend fun coverColor(url: String): Color? =
        url.takeIf { it.isNotBlank() }?.let { colorRepository.getColorOrExtract(context, it) }

    suspend fun resolveSongUrls(ids: List<String>, quality: MusicQuality) =
        repository.getSongUrlV1(ids, quality)

}
