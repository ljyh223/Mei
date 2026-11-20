package com.ljyh.mei.ui.screen.artist

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.constants.AppleArtistPortraitEnabledKey
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import com.ljyh.mei.data.repository.ArtistRepository
import com.ljyh.mei.data.repository.artwork.AppleArtistArtworkRepository
import com.ljyh.mei.di.repository.ColorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: ArtistRepository,
    private val colorRepository: ColorRepository,
    private val appleArtworks: AppleArtistArtworkRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _artistDetail = MutableStateFlow<Resource<ArtistDetail>>(Resource.Loading)
    val artistDetail: StateFlow<Resource<ArtistDetail>> = _artistDetail

    private val _artistAlbums = MutableStateFlow<Resource<ArtistAlbum>>(Resource.Loading)
    val artistAlbums: StateFlow<Resource<ArtistAlbum>> = _artistAlbums

    private val _artistSongs = MutableStateFlow<Resource<ArtistSong>>(Resource.Loading)
    val artistSongs: StateFlow<Resource<ArtistSong>> = _artistSongs

    private val subscriptionController = ArtistSubscriptionController(viewModelScope, repository::isSubscribed, repository::setSubscribed)
    internal val subscription = subscriptionController.state
    val events = subscriptionController.events
    fun loadSubscription(id: String, account: String) = subscriptionController.bind(id, account)
    fun toggleSubscription() = subscriptionController.toggle()

    fun cachedArtistColor(url: String): Color? = colorRepository.getFromMemory(url)

    suspend fun artistColor(url: String): Color = colorRepository.getColorOrExtract(context, url)

    private val artworkController = ArtistArtworkController(
        scope = viewModelScope,
        enabled = context.dataStore.data.map { it[AppleArtistPortraitEnabledKey] ?: false }.catch { emit(false) },
        resolve = { id, artist -> appleArtworks.resolve(id, artist.name, artistDisplayAliases(artist)) },
    )
    internal val artworkSelection = artworkController.state
    private var detailId: String? = null
    private var songsId: String? = null
    private var albumsId: String? = null
    private var detailRevision = 0
    private var songsRevision = 0
    private var albumsRevision = 0

    fun getArtistDetail(id: String, force: Boolean = false) {
        if (detailId == id && !force) return
        detailId = id
        val revision = ++detailRevision
        artworkController.bind(id, null)
        viewModelScope.launch {
            val result = repository.getArtistDetail(id)
            if (detailId != id || detailRevision != revision) return@launch
            _artistDetail.value = result
            val artist = (result as? Resource.Success)?.data?.data?.artist
            artworkController.bind(id, artist)
        }
    }

    fun getArtistAlbums(id: String, force: Boolean = false) {
        if (albumsId == id && !force) return
        albumsId = id
        val revision = ++albumsRevision
        viewModelScope.launch {
            val result = repository.getArtistAlbums(id)
            if (albumsId == id && albumsRevision == revision) _artistAlbums.value = result
        }
    }

    fun getArtistSongs(id: String, force: Boolean = false) {
        if (songsId == id && !force) return
        songsId = id
        val revision = ++songsRevision
        viewModelScope.launch {
            val result = repository.getArtistSongs(id)
            if (songsId == id && songsRevision == revision) _artistSongs.value = result
        }
    }
}
