package com.harmoniq.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Album
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    fun loadAlbumDetail(album: Album) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, album = album) }
            
            try {
                // Get all songs by album name
                val songs = musicRepository.getSongsByAlbumName(album.title)
                
                Log.d("AlbumDetailViewModel", "Loaded ${songs.size} songs for album: ${album.title}")
                
                _state.update {
                    it.copy(
                        songs = songs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Error loading album detail: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load album details"
                    )
                }
            }
        }
    }

    fun loadAlbumDetailById(albumId: String, albumTitle: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Try to get album from Firestore first
                val album = musicRepository.getAlbumById(albumId)
                
                if (album != null) {
                    // Album found in Firestore
                    loadAlbumDetail(album)
                } else if (albumTitle != null) {
                    // Album not in Firestore, but we have the title - generate from songs
                    val songs = musicRepository.getSongsByAlbumName(albumTitle)
                    
                    if (songs.isNotEmpty()) {
                        val firstSong = songs.first()
                        val generatedAlbum = Album(
                            id = albumId,
                            title = albumTitle,
                            artistId = firstSong.artistId,
                            artistName = firstSong.artistName,
                            coverUrl = firstSong.albumArtUrl,
                            releaseDate = songs.minOfOrNull { it.releaseDate } ?: 0L,
                            songCount = songs.size,
                            totalDuration = songs.sumOf { it.duration },
                            genre = firstSong.genre
                        )
                        
                        _state.update {
                            it.copy(
                                album = generatedAlbum,
                                songs = songs,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Album not found"
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Album not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Error loading album detail: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load album details"
                    )
                }
            }
        }
    }
}

private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

