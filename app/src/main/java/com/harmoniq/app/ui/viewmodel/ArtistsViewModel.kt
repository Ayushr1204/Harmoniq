package com.harmoniq.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Artist
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

data class ArtistsState(
    val artists: List<Artist> = emptyList(),
    val filteredArtists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistsState())
    val state: StateFlow<ArtistsState> = _state.asStateFlow()

    init {
        loadArtists()
    }

    private fun loadArtists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // First try to load artists from Firestore
                val firestoreArtists = try {
                    musicRepository.getAllArtists()
                        .catch { e -> 
                            Log.e("ArtistsViewModel", "Artists error: ${e.message}")
                            emit(emptyList())
                        }
                        .first()
                } catch (e: Exception) {
                    Log.e("ArtistsViewModel", "Error getting artists: ${e.message}")
                    emptyList()
                }
                
                Log.d("ArtistsViewModel", "Loaded ${firestoreArtists.size} artists from Firestore")
                
                if (firestoreArtists.isNotEmpty()) {
                    _state.update { 
                        it.copy(
                            artists = firestoreArtists,
                            filteredArtists = filterArtists(firestoreArtists, it.searchQuery),
                            isLoading = false
                        )
                    }
                } else {
                    // If artists collection is empty, generate from songs
                    Log.d("ArtistsViewModel", "No artists in Firestore, generating from songs")
                    generateArtistsFromSongs()
                }
            } catch (e: Exception) {
                Log.e("ArtistsViewModel", "Error loading artists: ${e.message}")
                // Fallback: generate from songs
                generateArtistsFromSongs()
            }
        }
    }

    private fun generateArtistsFromSongs() {
        viewModelScope.launch {
            try {
                // Get all songs once
                val songsList = try {
                    musicRepository.getAllSongs()
                        .catch { e -> 
                            Log.e("ArtistsViewModel", "Songs error: ${e.message}")
                            emit(emptyList())
                        }
                        .first()
                } catch (e: Exception) {
                    Log.e("ArtistsViewModel", "Error getting songs: ${e.message}")
                    emptyList()
                }
                
                Log.d("ArtistsViewModel", "Found ${songsList.size} songs to generate artists from")
                
                if (songsList.isNotEmpty()) {
                    val artistMap = mutableMapOf<String, MutableList<Song>>()
                    
                    songsList.forEach { song ->
                        // Split artist names by common separators
                        val artistNames = song.artistName.split(Regex("[,&]|feat\\.|ft\\.|featuring"))
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        artistNames.forEach { artistName ->
                            if (!artistMap.containsKey(artistName)) {
                                artistMap[artistName] = mutableListOf()
                            }
                            artistMap[artistName]?.add(song)
                        }
                    }
                    
                    val artists = artistMap.map { (artistName, songs) ->
                        val imageUrl = songs.randomOrNull()?.albumArtUrl ?: ""
                        Artist(
                            id = artistName.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), ""),
                            name = artistName,
                            imageUrl = imageUrl,
                            songCount = songs.size
                        )
                    }.sortedBy { it.name }
                    
                    Log.d("ArtistsViewModel", "Generated ${artists.size} artists from songs")
                    
                    _state.update {
                        it.copy(
                            artists = artists,
                            filteredArtists = filterArtists(artists, it.searchQuery),
                            isLoading = false
                        )
                    }
                } else {
                    Log.w("ArtistsViewModel", "No songs found to generate artists")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "No songs found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ArtistsViewModel", "Error generating artists: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load artists"
                    )
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { 
            it.copy(
                searchQuery = query,
                filteredArtists = filterArtists(it.artists, query)
            )
        }
    }

    private fun filterArtists(artists: List<Artist>, query: String): List<Artist> {
        if (query.isBlank()) {
            return artists
        }
        val queryLower = query.lowercase()
        return artists.filter { 
            it.name.lowercase().contains(queryLower)
        }
    }

    fun refresh() {
        loadArtists()
    }
}

private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

