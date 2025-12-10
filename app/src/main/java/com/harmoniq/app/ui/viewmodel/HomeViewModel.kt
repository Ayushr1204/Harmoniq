package com.harmoniq.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Album
import com.harmoniq.app.data.model.Artist
import com.harmoniq.app.data.model.Mood
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.model.User
import com.harmoniq.app.data.repository.MusicRepository
import com.harmoniq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val user: User? = null,
    val suggestions: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val allSongs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val selectedMood: Mood = Mood.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    private var userDataJob: kotlinx.coroutines.Job? = null

    init {
        loadData()
    }
    
    fun loadUserData() {
        // Cancel previous user data collection if it exists
        userDataJob?.cancel()
        
        // Start new user data collection
        userDataJob = viewModelScope.launch {
            try {
                userRepository.getCurrentUser()
                    .catch { e -> Log.e("HomeViewModel", "User error: ${e.message}") }
                    .collect { user ->
                        // Load recently played songs (newest first, limit to 10)
                        val recentSongs = if (user != null && user.recentlyPlayed.isNotEmpty()) {
                            // user.recentlyPlayed is already ordered with newest at index 0
                            user.recentlyPlayed.take(10).mapNotNull { songId ->
                                musicRepository.getSongById(songId)
                            }
                        } else {
                            emptyList()
                        }
                        
                        _state.update { 
                            it.copy(
                                user = user,
                                recentlyPlayed = recentSongs
                            ) 
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "User error: ${e.message}")
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            // Don't auto-sign in anonymously anymore - let user choose to login or continue as guest
            // The login screen will handle anonymous sign-in if user chooses "Continue as Guest"

            // Load user data
            loadUserData()

            // Load all songs
            launch {
                try {
                    musicRepository.getAllSongs()
                        .catch { e -> 
                            Log.e("HomeViewModel", "Songs error: ${e.message}")
                            _state.update { it.copy(isLoading = false, error = e.message) }
                        }
                        .collect { songs ->
                            Log.d("HomeViewModel", "Loaded ${songs.size} songs")
                            _state.update { state ->
                                // Filter by selected mood
                                val filtered = if (state.selectedMood == Mood.ALL) songs 
                                               else songs.filter { it.moods.any { mood -> mood.equals(state.selectedMood.name, ignoreCase = true) } }
                                
                                // Only shuffle suggestions once when songs are first loaded, filtered by mood
                                val suggestions = if (state.suggestions.isEmpty() && filtered.isNotEmpty()) {
                                    filtered.shuffled().take(10)
                                } else {
                                    state.suggestions
                                }
                                
                                state.copy(
                                    allSongs = songs,
                                    filteredSongs = filtered,
                                    suggestions = suggestions,
                                    isLoading = false
                                )
                            }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Songs error: ${e.message}")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }

            // Load most played - with error handling
            launch {
                try {
                    musicRepository.getMostPlayedSongs(20)
                        .catch { e -> Log.e("HomeViewModel", "Most played error: ${e.message}") }
                        .collect { songs ->
                            _state.update { it.copy(mostPlayed = songs) }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Most played error: ${e.message}")
                }
            }

            // Load recently added - with error handling
            launch {
                try {
                    musicRepository.getRecentlyAddedSongs(20)
                        .catch { e -> Log.e("HomeViewModel", "Recent error: ${e.message}") }
                        .collect { songs ->
                            _state.update { it.copy(recentlyAdded = songs) }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Recent error: ${e.message}")
                }
            }

            // Load artists - with error handling
            launch {
                try {
                    musicRepository.getAllArtists()
                        .catch { e -> Log.e("HomeViewModel", "Artists error: ${e.message}") }
                        .collect { artists ->
                            _state.update { it.copy(artists = artists) }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Artists error: ${e.message}")
                }
            }

            // Load albums - with error handling
            launch {
                try {
                    musicRepository.getAllAlbums()
                        .catch { e -> Log.e("HomeViewModel", "Albums error: ${e.message}") }
                        .collect { albums ->
                            _state.update { it.copy(albums = albums) }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Albums error: ${e.message}")
                }
            }
        }
    }

    fun selectMood(mood: Mood) {
        _state.update { state ->
            val filtered = if (mood == Mood.ALL) {
                state.allSongs
            } else {
                state.allSongs.filter { it.moods.any { songMood -> songMood.equals(mood.name, ignoreCase = true) } }
            }
            // Update suggestions based on new mood
            val moodFilteredSuggestions = if (mood == Mood.ALL) {
                filtered.shuffled().take(10)
            } else {
                filtered.shuffled().take(10)
            }
            state.copy(
                selectedMood = mood,
                filteredSongs = filtered,
                suggestions = moodFilteredSuggestions
            )
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null
    
    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        
        // Cancel previous search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _state.update { state ->
                state.copy(
                    filteredSongs = if (state.selectedMood == Mood.ALL) state.allSongs
                                   else state.allSongs.filter { it.moods.any { songMood -> songMood.equals(state.selectedMood.name, ignoreCase = true) } }
                )
            }
            return
        }

        // Debounce search with 300ms delay
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            try {
                val results = musicRepository.searchSongs(query)
                _state.update { state ->
                    val filtered = if (state.selectedMood == Mood.ALL) results
                                  else results.filter { it.moods.any { songMood -> songMood.equals(state.selectedMood.name, ignoreCase = true) } }
                    state.copy(filteredSongs = filtered)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Search error: ${e.message}")
            }
        }
    }

    fun refreshSuggestions() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            // Filter by selected mood and shuffle on background thread
            val currentState = _state.value
            val filteredSongs = if (currentState.selectedMood == Mood.ALL) {
                currentState.allSongs
            } else {
                currentState.allSongs.filter { 
                    it.moods.any { mood -> mood.equals(currentState.selectedMood.name, ignoreCase = true) } 
                }
            }
            val shuffled = filteredSongs.shuffled().take(10)
            _state.update { state ->
                state.copy(suggestions = shuffled)
            }
        }
    }

    fun refresh() {
        loadData()
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

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }
}
