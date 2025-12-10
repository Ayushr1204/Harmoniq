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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsState(
    val albums: List<Album> = emptyList(),
    val filteredAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsState())
    val state: StateFlow<AlbumsState> = _state.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // First try to load albums from Firestore
                val firestoreAlbums = try {
                    musicRepository.getAllAlbums()
                        .catch { e -> 
                            Log.e("AlbumsViewModel", "Albums error: ${e.message}")
                            emit(emptyList())
                        }
                        .first()
                } catch (e: Exception) {
                    Log.e("AlbumsViewModel", "Error getting albums: ${e.message}")
                    emptyList()
                }
                
                Log.d("AlbumsViewModel", "Loaded ${firestoreAlbums.size} albums from Firestore")
                
                if (firestoreAlbums.isNotEmpty()) {
                    _state.update { 
                        it.copy(
                            albums = firestoreAlbums,
                            filteredAlbums = filterAlbums(firestoreAlbums, it.searchQuery),
                            isLoading = false
                        )
                    }
                } else {
                    // If albums collection is empty, generate from songs
                    Log.d("AlbumsViewModel", "No albums in Firestore, generating from songs")
                    generateAlbumsFromSongs()
                }
            } catch (e: Exception) {
                Log.e("AlbumsViewModel", "Error loading albums: ${e.message}")
                // Fallback: generate from songs
                generateAlbumsFromSongs()
            }
        }
    }

    private fun generateAlbumsFromSongs() {
        viewModelScope.launch {
            try {
                // Get all songs once
                val songsList = try {
                    musicRepository.getAllSongs()
                        .catch { e -> 
                            Log.e("AlbumsViewModel", "Songs error: ${e.message}")
                            emit(emptyList())
                        }
                        .first()
                } catch (e: Exception) {
                    Log.e("AlbumsViewModel", "Error getting songs: ${e.message}")
                    emptyList()
                }
                
                Log.d("AlbumsViewModel", "Found ${songsList.size} songs to generate albums from")
                
                if (songsList.isNotEmpty()) {
                    // Group songs by album name
                    val albumMap = mutableMapOf<String, MutableList<Song>>()
                    
                    songsList.forEach { song ->
                        val albumName = song.albumName.trim()
                        if (albumName.isNotEmpty()) {
                            if (!albumMap.containsKey(albumName)) {
                                albumMap[albumName] = mutableListOf()
                            }
                            albumMap[albumName]?.add(song)
                        }
                    }
                    
                    // Create albums from grouped songs
                    val albums = albumMap.map { (albumName, songs) ->
                        // Use the first song's cover as the album cover
                        val firstSong = songs.firstOrNull()
                        val coverUrl = firstSong?.albumArtUrl ?: ""
                        val artistName = firstSong?.artistName ?: ""
                        val artistId = firstSong?.artistId ?: ""
                        
                        // Calculate total duration
                        val totalDuration = songs.sumOf { it.duration }
                        
                        // Get the earliest release date
                        val releaseDate = songs.minOfOrNull { it.releaseDate } ?: 0L
                        
                        // Get genre from first song
                        val genre = firstSong?.genre ?: ""
                        
                        Album(
                            id = albumName.lowercase()
                                .replace(" ", "_")
                                .replace(Regex("[^a-z0-9_]"), ""),
                            title = albumName,
                            artistId = artistId,
                            artistName = artistName,
                            coverUrl = coverUrl,
                            releaseDate = releaseDate,
                            songCount = songs.size,
                            totalDuration = totalDuration,
                            genre = genre
                        )
                    }.sortedByDescending { it.releaseDate } // Sort by release date, newest first
                    
                    Log.d("AlbumsViewModel", "Generated ${albums.size} albums from songs")
                    
                    _state.update {
                        it.copy(
                            albums = albums,
                            filteredAlbums = filterAlbums(albums, it.searchQuery),
                            isLoading = false
                        )
                    }
                } else {
                    Log.w("AlbumsViewModel", "No songs found to generate albums")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "No songs found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AlbumsViewModel", "Error generating albums: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load albums"
                    )
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { 
            it.copy(
                searchQuery = query,
                filteredAlbums = filterAlbums(it.albums, query)
            )
        }
    }

    private fun filterAlbums(albums: List<Album>, query: String): List<Album> {
        if (query.isBlank()) {
            return albums
        }
        val queryLower = query.lowercase()
        return albums.filter { 
            it.title.lowercase().contains(queryLower) ||
            it.artistName.lowercase().contains(queryLower)
        }
    }

    fun refresh() {
        loadAlbums()
    }
}

private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

