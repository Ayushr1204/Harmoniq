package com.harmoniq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Artist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetailState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val artistImageUrl: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    fun loadArtistDetail(artist: Artist) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, artist = artist) }
            
            try {
                // Get all songs by this artist (handles multiple artists)
                val allSongs = musicRepository.getSongsByArtistName(artist.name)
                
                // Get a random album art for the artist image
                val artistImageUrl = if (allSongs.isNotEmpty()) {
                    allSongs.random().albumArtUrl
                } else {
                    artist.imageUrl
                }
                
                _state.update {
                    it.copy(
                        songs = allSongs,
                        artistImageUrl = artistImageUrl,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load artist details"
                    )
                }
            }
        }
    }

    fun loadArtistDetailById(artistId: String, artistName: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // First try to get artist by ID from Firestore
                var artist = musicRepository.getArtistById(artistId)
                
                // If not found in Firestore and we have a name, create artist from name
                if (artist == null && artistName != null) {
                    // Get songs by artist name
                    val allSongs = musicRepository.getSongsByArtistName(artistName)
                    if (allSongs.isNotEmpty()) {
                        val imageUrl = allSongs.random().albumArtUrl
                        artist = Artist(
                            id = artistId,
                            name = artistName,
                            imageUrl = imageUrl,
                            songCount = allSongs.size
                        )
                        
                        _state.update { it.copy(artist = artist) }
                        
                        // Get a random album art for the artist image
                        val artistImageUrl = allSongs.random().albumArtUrl
                        
                        _state.update {
                            it.copy(
                                songs = allSongs,
                                artistImageUrl = artistImageUrl,
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                }
                
                // If still not found, try to extract name from ID (for generated artists)
                if (artist == null) {
                    // Try to reconstruct name from ID (replace _ with space and capitalize)
                    val reconstructedName = artistId.replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { word ->
                            word.lowercase().replaceFirstChar { it.uppercaseChar() }
                        }
                    
                    val allSongs = musicRepository.getSongsByArtistName(reconstructedName)
                    if (allSongs.isNotEmpty()) {
                        val imageUrl = allSongs.random().albumArtUrl
                        artist = Artist(
                            id = artistId,
                            name = reconstructedName,
                            imageUrl = imageUrl,
                            songCount = allSongs.size
                        )
                    }
                }
                
                if (artist == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Artist not found"
                        )
                    }
                    return@launch
                }
                
                _state.update { it.copy(artist = artist) }
                
                // Get all songs by this artist (handles multiple artists)
                val allSongs = musicRepository.getSongsByArtistName(artist.name)
                
                // Get a random album art for the artist image
                val artistImageUrl = if (allSongs.isNotEmpty()) {
                    allSongs.random().albumArtUrl
                } else {
                    artist.imageUrl
                }
                
                _state.update {
                    it.copy(
                        songs = allSongs,
                        artistImageUrl = artistImageUrl,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load artist details"
                    )
                }
            }
        }
    }
}

private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

