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

data class MostPlayedDetailState(
    val songs: List<Song> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MostPlayedDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MostPlayedDetailState())
    val state: StateFlow<MostPlayedDetailState> = _state.asStateFlow()

    init {
        loadMostPlayed()
        loadLikedSongs()
        loadPlaylists()
    }

    private fun loadMostPlayed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                musicRepository.getMostPlayedSongs(100).collect { songs ->
                    // Sort by playCount descending
                    val sortedSongs = songs.sortedByDescending { it.playCount }
                    _state.update {
                        it.copy(
                            songs = sortedSongs,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load most played songs"
                    )
                }
            }
        }
    }

    private fun loadLikedSongs() {
        viewModelScope.launch {
            try {
                userRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        val likedSongIds = user.likedSongs
                        val likedSongs = likedSongIds.mapNotNull { songId ->
                            musicRepository.getSongById(songId)
                        }
                        _state.update { it.copy(likedSongs = likedSongs) }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
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

    fun addToLiked(songId: String) {
        viewModelScope.launch {
            userRepository.addToLikedSongs(songId)
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

