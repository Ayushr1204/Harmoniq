package com.harmoniq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Watch playlist changes
                musicRepository.getPlaylistFlow(playlistId).collect { playlist ->
                    if (playlist == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Playlist not found"
                            )
                        }
                        return@collect
                    }
                    
                    _state.update { it.copy(playlist = playlist) }
                    
                    // Fetch songs for this playlist
                    val songs = playlist.songIds.mapNotNull { songId ->
                        musicRepository.getSongById(songId)
                    }
                    
                    _state.update {
                        it.copy(
                            songs = songs,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load playlist"
                    )
                }
            }
        }
    }

    fun addSong(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSong(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }
}

private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

