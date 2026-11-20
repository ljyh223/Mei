package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ArtistArtworkSelection(
    val enabled: Boolean = false,
    val artwork: AppleArtistArtwork? = null,
)

/** The opt-in gates both resolution and presentation, including previously cached artwork. */
internal class ArtistArtworkController(
    private val scope: CoroutineScope,
    enabled: Flow<Boolean>,
    private val resolve: suspend (String, ArtistDetail.Data.Artist) -> AppleArtistArtwork?,
) {
    private val _state = MutableStateFlow(ArtistArtworkSelection())
    val state = _state.asStateFlow()
    private var artist: Pair<String, ArtistDetail.Data.Artist?>? = null
    private var job: Job? = null
    private var revision = 0

    init {
        scope.launch {
            enabled.distinctUntilChanged().collect { value ->
                if (_state.value.enabled != value) {
                    _state.value = ArtistArtworkSelection(enabled = value)
                    refresh()
                }
            }
        }
    }

    fun bind(id: String, details: ArtistDetail.Data.Artist?) {
        val next = id to details
        if (artist == next) return
        artist = next
        refresh()
    }

    private fun refresh() {
        val generation = ++revision
        job?.cancel()
        _state.value = _state.value.copy(artwork = null)
        val (id, details) = artist ?: return
        if (!_state.value.enabled || details == null) return
        job = scope.launch {
            val artwork = try {
                withTimeoutOrNull(20_000) { resolve(id, details) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (generation == revision && _state.value.enabled) {
                _state.value = _state.value.copy(artwork = artwork)
            }
        }
    }
}
