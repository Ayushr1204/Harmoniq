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

data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val userId = userRepository.currentUserId ?: return@launch

            // Load playlists
            launch {
                musicRepository.getUserPlaylists(userId).collect { playlists ->
                    _state.update { it.copy(playlists = playlists) }
                }
            }

            // Load liked songs from user data
            launch {
                userRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        val likedSongIds = user.likedSongs
                        val likedSongs = likedSongIds.mapNotNull { songId ->
                            musicRepository.getSongById(songId)
                        }
                        _state.update { it.copy(likedSongs = likedSongs) }
                        
                        val recentIds = user.recentlyPlayed
                        val recentSongs = recentIds.mapNotNull { songId ->
                            musicRepository.getSongById(songId)
                        }
                        _state.update { it.copy(recentlyPlayed = recentSongs, isLoading = false) }
                    }
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", onCreated: ((String?) -> Unit)? = null) {
        viewModelScope.launch {
            val userId = userRepository.currentUserId ?: return@launch
            val playlist = Playlist(
                name = name,
                description = description,
                userId = userId
            )
            val playlistId = musicRepository.createPlaylist(playlist)
            onCreated?.invoke(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun updatePlaylistName(playlistId: String, newName: String) {
        viewModelScope.launch {
            musicRepository.updatePlaylistName(playlistId, newName)
        }
    }

    fun updatePlaylistDescription(playlistId: String, newDescription: String) {
        viewModelScope.launch {
            musicRepository.updatePlaylistDescription(playlistId, newDescription)
        }
    }

    fun addToLikedSongs(songId: String) {
        viewModelScope.launch {
            userRepository.addToLikedSongs(songId)
        }
    }

    fun removeFromLikedSongs(songId: String) {
        viewModelScope.launch {
            userRepository.removeFromLikedSongs(songId)
        }
    }

    fun removeFromRecentlyPlayed(songId: String) {
        viewModelScope.launch {
            userRepository.removeFromRecentlyPlayed(songId)
        }
    }

    fun refresh() {
        loadLibrary()
    }
}

