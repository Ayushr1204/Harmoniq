package com.harmoniq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.repository.MusicRepository
import com.harmoniq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LikedSongsDetailState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LikedSongsDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LikedSongsDetailState())
    val state: StateFlow<LikedSongsDetailState> = _state.asStateFlow()

    init {
        loadLikedSongs()
        loadPlaylists()
    }

    private fun loadLikedSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                userRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        val likedSongIds = user.likedSongs
                        val likedSongs = likedSongIds.mapNotNull { songId ->
                            musicRepository.getSongById(songId)
                        }
                        _state.update {
                            it.copy(
                                songs = likedSongs,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                songs = emptyList(),
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load liked songs"
                    )
                }
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                val userId = userRepository.currentUserId ?: return@launch
                musicRepository.getUserPlaylists(userId).collect { playlists ->
                    _state.update { it.copy(playlists = playlists) }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun removeFromLiked(songId: String) {
        viewModelScope.launch {
            userRepository.removeFromLikedSongs(songId)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }
}

