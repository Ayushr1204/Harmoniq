package com.harmoniq.app.ui.viewmodel

import android.util.Log
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import com.harmoniq.app.data.model.LyricLine
import com.harmoniq.app.data.model.Song
import com.harmoniq.app.data.repository.MusicRepository
import com.harmoniq.app.data.repository.UserRepository
import com.harmoniq.app.service.MusicServiceConnection
import com.harmoniq.app.ui.theme.Cyan
import com.harmoniq.app.util.extractAccentColorFromImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val shuffledQueue: List<Song> = emptyList(), // Shuffled order when shuffle is enabled
    val shuffledIndex: Int = 0, // Current index in shuffled queue
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLiked: Boolean = false,
    val currentLyricIndex: Int = -1,
    val accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF00D4FF) // Default cyan
)

enum class RepeatMode {
    OFF, ONE, ALL
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository,
    private val musicService: MusicServiceConnection,
    private val userPreferences: com.harmoniq.app.data.preferences.UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    private val imageLoader = ImageLoader(context)

    private var progressJob: Job? = null
    
    // Cache for preloaded songs to avoid redundant loading
    private val preloadedSongs = mutableSetOf<String>()

    init {
        // Connect to music service
        musicService.connect()
        
        // Observe user's liked songs to update isLiked state
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                val currentSong = _state.value.currentSong
                if (currentSong != null && user != null) {
                    val isLiked = user.likedSongs.contains(currentSong.id)
                    _state.update { it.copy(isLiked = isLiked) }
                }
            }
        }
        
        // Observe service state
        viewModelScope.launch {
            musicService.isConnected.collect { connected ->
                if (connected) {
                    startProgressUpdates()
                    // Apply saved playback speed when service connects
                    val currentSpeed = userPreferences.playbackSpeed.first()
                    musicService.setPlaybackSpeed(currentSpeed)
                }
            }
        }
        
        // Observe playback speed changes and apply them
        viewModelScope.launch {
            userPreferences.playbackSpeed.collect { speed ->
                if (musicService.isConnected.value) {
                    musicService.setPlaybackSpeed(speed)
                }
            }
        }
        
        viewModelScope.launch {
            musicService.isPlaying.collect { playing ->
                _state.update { it.copy(isPlaying = playing) }
            }
        }
        
        viewModelScope.launch {
            musicService.currentPosition.collect { position ->
                _state.update { it.copy(currentPosition = position) }
                updateCurrentLyric()
            }
        }
        
        // Observe media item changes from service (when song changes externally, e.g., shuffle)
        // Use a more efficient approach with longer delay and only check when needed
        viewModelScope.launch {
            var lastMediaId: String? = null
            while (true) {
                kotlinx.coroutines.delay(500) // Reduced frequency - check every 500ms instead of 300ms
                val currentMediaItem = musicService.getCurrentMediaItem()
                val currentSong = _state.value.currentSong
                
                // If media item changed but our state hasn't updated, sync state
                if (currentMediaItem != null) {
                    val mediaId = currentMediaItem.mediaId
                    // Only process if media ID actually changed
                    if (mediaId != lastMediaId && (currentSong == null || mediaId != currentSong.id)) {
                        lastMediaId = mediaId
                        // Song changed externally (e.g., shuffle, auto-next), find it in queue
                        val state = _state.value
                        val activeQueue = if (state.isShuffled && state.shuffledQueue.isNotEmpty()) {
                            state.shuffledQueue
                        } else {
                            state.queue
                        }
                        
                        val newSong = activeQueue.find { it.id == mediaId }
                        if (newSong != null && (currentSong == null || newSong.id != currentSong.id)) {
                            val activeIndex = activeQueue.indexOf(newSong)
                            val originalIndex = state.queue.indexOf(newSong)
                            
                            // Extract color asynchronously without blocking
                            viewModelScope.launch {
                                val accentColor = extractAccentColorFromImage(
                                    imageUrl = newSong.albumArtUrl,
                                    imageLoader = imageLoader,
                                    context = context,
                                    defaultColor = Cyan
                                )
                                
                                // Check if song is liked
                                val user = userRepository.getCurrentUser().first()
                                val isLiked = user?.likedSongs?.contains(newSong.id) ?: false
                                
                                val position = musicService.getCurrentPosition()
                                val duration = musicService.getDuration()
                                
                                _state.update { 
                                    it.copy(
                                        currentSong = newSong,
                                        accentColor = accentColor,
                                        currentIndex = originalIndex.takeIf { it >= 0 } ?: 0,
                                        shuffledIndex = if (state.isShuffled && state.shuffledQueue.isNotEmpty()) activeIndex else 0,
                                        currentPosition = position,
                                        duration = if (duration > 0) duration else newSong.duration,
                                        isLiked = isLiked
                                    )
                                }
                                
                                // Update play count and recently played
                                musicRepository.incrementPlayCount(newSong.id)
                                userRepository.addToRecentlyPlayed(newSong.id)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds a circular queue starting from the selected song
     * Format: [selected song, songs after it till end, songs from beginning till just before selected]
     */
    private fun buildCircularQueue(selectedSong: Song, allSongs: List<Song>): List<Song> {
        if (allSongs.isEmpty() || !allSongs.contains(selectedSong)) {
            return listOf(selectedSong)
        }
        
        val selectedIndex = allSongs.indexOf(selectedSong)
        val queue = mutableListOf<Song>()
        
        // Start with selected song
        queue.add(selectedSong)
        
        // Add all songs after the selected song (till end)
        if (selectedIndex < allSongs.size - 1) {
            queue.addAll(allSongs.subList(selectedIndex + 1, allSongs.size))
        }
        
        // Add all songs from beginning till just before the selected song
        if (selectedIndex > 0) {
            queue.addAll(allSongs.subList(0, selectedIndex))
        }
        
        return queue
    }
    
    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        // Use the provided queue (which should be built based on source)
        val finalQueue = if (queue.isEmpty() || queue.size == 1) {
            // If no queue provided or only one song, use just that song
            listOf(song)
        } else {
            // Build circular queue from the provided list
            buildCircularQueue(song, queue)
        }
        
        // Update state immediately for instant UI response
        _state.update { 
            it.copy(
                currentSong = song,
                queue = finalQueue,
                shuffledQueue = emptyList(),
                shuffledIndex = 0,
                currentIndex = 0,
                isShuffled = false, // Reset shuffle when playing new song
                duration = song.duration,
                isLiked = false // Will update asynchronously
            )
        }
        
        // Play the queue starting with the selected song immediately
        musicService.playSongs(finalQueue, 0)
        
        // Check if song is liked and extract accent color asynchronously (non-blocking)
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().first()
            val isLiked = user?.likedSongs?.contains(song.id) ?: false
            
            // Extract accent color from album art (async, non-blocking)
            val accentColor = extractAccentColorFromImage(
                imageUrl = song.albumArtUrl,
                imageLoader = imageLoader,
                context = context,
                defaultColor = Cyan
            )
            
            _state.update {
                it.copy(
                    isLiked = isLiked,
                    accentColor = accentColor
                )
            }
        }
        
        // Update play count and recently played asynchronously (non-blocking)
        viewModelScope.launch {
            musicRepository.incrementPlayCount(song.id)
            userRepository.addToRecentlyPlayed(song.id)
        }
        
        startProgressUpdates()
        
        // Preload upcoming songs and album art
        preloadUpcomingSongs(finalQueue, 0)
    }
    
    /**
     * Preload upcoming songs' album art images to reduce lag when switching songs.
     * Preloads the next 2-3 songs in the queue.
     */
    private fun preloadUpcomingSongs(queue: List<Song>, currentIndex: Int) {
        if (queue.isEmpty()) return
        
        viewModelScope.launch {
            // Preload next 2-3 songs
            val preloadCount = minOf(3, queue.size - currentIndex - 1)
            for (i in 1..preloadCount) {
                val nextIndex = currentIndex + i
                if (nextIndex < queue.size) {
                    val nextSong = queue[nextIndex]
                    // Only preload if not already preloaded
                    if (!preloadedSongs.contains(nextSong.id)) {
                        preloadAlbumArt(nextSong.albumArtUrl)
                        preloadedSongs.add(nextSong.id)
                    }
                }
            }
            
            // Also preload previous song if available (for going back)
            if (currentIndex > 0) {
                val prevSong = queue[currentIndex - 1]
                if (!preloadedSongs.contains(prevSong.id)) {
                    preloadAlbumArt(prevSong.albumArtUrl)
                    preloadedSongs.add(prevSong.id)
                }
            }
        }
    }
    
    /**
     * Preload an album art image into Coil's cache.
     */
    private suspend fun preloadAlbumArt(imageUrl: String) {
        if (imageUrl.isBlank()) return
        
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(512) // Preload at a reasonable size for UI
                .build()
            
            // Execute the request to load into cache
            imageLoader.execute(request)
        } catch (e: Exception) {
            // Silently fail - preloading is best effort
            Log.d("PlayerViewModel", "Failed to preload image: ${e.message}")
        }
    }

    fun togglePlayPause() {
        musicService.togglePlayPause()
    }
    
    // Play a song from the current queue without resetting the queue
    fun playFromQueue(song: Song) {
        val currentState = _state.value
        
        // Use the active queue based on shuffle state
        val activeQueue = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            currentState.shuffledQueue
        } else {
            currentState.queue
        }
        
        val activeIndex = activeQueue.indexOf(song)
        if (activeIndex < 0) return
        
        // Also find index in regular queue for currentIndex
        val regularIndex = currentState.queue.indexOf(song).takeIf { it >= 0 } ?: 0
        
        // Update state immediately for instant UI response
        _state.update { 
            it.copy(
                currentSong = song,
                currentIndex = regularIndex,
                shuffledIndex = if (currentState.isShuffled) activeIndex else 0,
                currentPosition = 0L,
                duration = song.duration,
                isLiked = false // Will update asynchronously
            )
        }
        
        // Play from the active queue at the new index immediately
        musicService.playSongs(activeQueue, activeIndex)
        
        // Update liked status and extract accent color asynchronously (non-blocking)
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().first()
            val isLiked = user?.likedSongs?.contains(song.id) ?: false
            
            // Extract accent color from album art (async, non-blocking)
            val accentColor = extractAccentColorFromImage(
                imageUrl = song.albumArtUrl,
                imageLoader = imageLoader,
                context = context,
                defaultColor = Cyan
            )
            
            _state.update {
                it.copy(
                    isLiked = isLiked,
                    accentColor = accentColor
                )
            }
        }
        
        // Update play count and recently played asynchronously (non-blocking)
        viewModelScope.launch {
            musicRepository.incrementPlayCount(song.id)
            userRepository.addToRecentlyPlayed(song.id)
        }
        
        // Preload upcoming songs
        preloadUpcomingSongs(activeQueue, activeIndex)
    }

    fun seekTo(position: Long) {
        musicService.seekTo(position)
        _state.update { it.copy(currentPosition = position) }
        updateCurrentLyric()
    }

    fun playNext() {
        val currentState = _state.value
        if (currentState.queue.isEmpty()) return

        // If repeat one, just restart current song
        if (currentState.repeatMode == RepeatMode.ONE) {
            seekTo(0)
            return
        }

        // Use shuffled queue if shuffle is enabled, otherwise use regular queue
        val activeQueue = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            currentState.shuffledQueue
        } else {
            currentState.queue
        }
        
        val currentActiveIndex = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            currentState.shuffledIndex
        } else {
            currentState.currentIndex
        }

        // Calculate next index
        val nextIndex = when {
            currentActiveIndex < activeQueue.size - 1 -> currentActiveIndex + 1
            currentState.repeatMode == RepeatMode.ALL -> 0
            else -> return // No next song
        }

        val nextSong = activeQueue[nextIndex]
        
        // Update state immediately for instant UI response
        val regularIndex = if (currentState.isShuffled) {
            currentState.queue.indexOf(nextSong).takeIf { it >= 0 } ?: 0
        } else {
            nextIndex
        }
        
        _state.update {
            it.copy(
                currentSong = nextSong,
                currentIndex = regularIndex,
                shuffledIndex = if (currentState.isShuffled) nextIndex else 0,
                currentPosition = 0L,
                duration = nextSong.duration,
                isLiked = false // Will update asynchronously
            )
        }
        
        // Tell ExoPlayer to play the next song immediately
        musicService.playSongs(activeQueue, nextIndex)
        
        // Update liked status and extract accent color asynchronously (non-blocking)
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().first()
            val isLiked = user?.likedSongs?.contains(nextSong.id) ?: false
            
            // Extract accent color from album art (async, non-blocking)
            val accentColor = extractAccentColorFromImage(
                imageUrl = nextSong.albumArtUrl,
                imageLoader = imageLoader,
                context = context,
                defaultColor = Cyan
            )
            
            _state.update {
                it.copy(
                    isLiked = isLiked,
                    accentColor = accentColor
                )
            }
        }
        
        // Update play count and recently played asynchronously (non-blocking)
        viewModelScope.launch {
            musicRepository.incrementPlayCount(nextSong.id)
            userRepository.addToRecentlyPlayed(nextSong.id)
        }
        
        // Preload upcoming songs
        preloadUpcomingSongs(activeQueue, nextIndex)
    }

    fun playPrevious() {
        val currentState = _state.value
        if (currentState.queue.isEmpty()) return

        // If more than 3 seconds in, restart current song
        if (currentState.currentPosition > 3000) {
            seekTo(0)
            return
        }

        // Use shuffled queue if shuffle is enabled, otherwise use regular queue
        val activeQueue = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            currentState.shuffledQueue
        } else {
            currentState.queue
        }
        
        val currentActiveIndex = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            currentState.shuffledIndex
        } else {
            currentState.currentIndex
        }

        // Calculate previous index
        val prevIndex = when {
            currentActiveIndex > 0 -> currentActiveIndex - 1
            currentState.repeatMode == RepeatMode.ALL -> activeQueue.size - 1
            else -> 0 // Restart current song if at beginning
        }

        val prevSong = activeQueue[prevIndex]
        
        // Update state immediately for instant UI response
        val regularIndex = if (currentState.isShuffled) {
            currentState.queue.indexOf(prevSong).takeIf { it >= 0 } ?: 0
        } else {
            prevIndex
        }
        
        _state.update {
            it.copy(
                currentSong = prevSong,
                currentIndex = regularIndex,
                shuffledIndex = if (currentState.isShuffled) prevIndex else 0,
                currentPosition = 0L,
                duration = prevSong.duration,
                isLiked = false // Will update asynchronously
            )
        }
        
        // Tell ExoPlayer to play the previous song immediately
        musicService.playSongs(activeQueue, prevIndex)
        
        // Update liked status and extract accent color asynchronously (non-blocking)
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().first()
            val isLiked = user?.likedSongs?.contains(prevSong.id) ?: false
            
            // Extract accent color from album art (async, non-blocking)
            val accentColor = extractAccentColorFromImage(
                imageUrl = prevSong.albumArtUrl,
                imageLoader = imageLoader,
                context = context,
                defaultColor = Cyan
            )
            
            _state.update {
                it.copy(
                    isLiked = isLiked,
                    accentColor = accentColor
                )
            }
        }
        
        // Update play count and recently played asynchronously (non-blocking)
        viewModelScope.launch {
            musicRepository.incrementPlayCount(prevSong.id)
            userRepository.addToRecentlyPlayed(prevSong.id)
        }
        
        // Preload upcoming songs
        preloadUpcomingSongs(activeQueue, prevIndex)
    }

    fun toggleShuffle() {
        val currentState = _state.value
        val newShuffle = !currentState.isShuffled
        
        if (newShuffle && currentState.queue.isNotEmpty()) {
            val currentSong = currentState.currentSong
            if (currentSong == null) {
                // No current song, just shuffle the queue
                val shuffledQueue = currentState.queue.shuffled()
                _state.update { 
                    it.copy(
                        isShuffled = newShuffle,
                        shuffledQueue = shuffledQueue,
                        shuffledIndex = 0
                    ) 
                }
                musicService.updateQueue(shuffledQueue, 0)
            } else {
                // Shuffle the circular queue while keeping current song at position 0
                // Get all songs except current song
                val otherSongs = currentState.queue.filter { it.id != currentSong.id }.shuffled()
                
                // Build shuffled circular queue: [current song, shuffled other songs]
                val shuffledQueue = listOf(currentSong) + otherSongs
                
                _state.update { 
                    it.copy(
                        isShuffled = newShuffle,
                        shuffledQueue = shuffledQueue,
                        shuffledIndex = 0 // Current song is always at index 0
                    ) 
                }
                
                // Update the service with shuffled queue without restarting playback
                musicService.updateQueue(shuffledQueue, 0)
                // Preload upcoming songs from shuffled queue
                preloadUpcomingSongs(shuffledQueue, 0)
            }
        } else {
            // Restore original queue
            val currentSong = currentState.currentSong
            val originalIndex = if (currentSong != null) {
                currentState.queue.indexOf(currentSong).takeIf { it >= 0 } ?: currentState.currentIndex
            } else {
                currentState.currentIndex
            }
            
            _state.update { 
                it.copy(
                    isShuffled = newShuffle,
                    shuffledQueue = emptyList(),
                    shuffledIndex = 0,
                    currentIndex = originalIndex
                ) 
            }
            
            // Update the service with original queue without restarting playback
            if (currentSong != null && currentState.queue.isNotEmpty()) {
                musicService.updateQueue(currentState.queue, originalIndex)
                // Preload upcoming songs from original queue
                preloadUpcomingSongs(currentState.queue, originalIndex)
            }
        }
        
        musicService.setShuffleMode(newShuffle)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentState = _state.value
        if (fromIndex == toIndex) return
        if (fromIndex < 0 || toIndex < 0) return
        
        if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            // Reorder shuffled queue
            if (fromIndex >= currentState.shuffledQueue.size || toIndex >= currentState.shuffledQueue.size) return
            
            val newShuffledQueue = currentState.shuffledQueue.toMutableList()
            val item = newShuffledQueue.removeAt(fromIndex)
            newShuffledQueue.add(toIndex, item)
            
            // Update shuffled index if needed
            val newShuffledIndex = when {
                fromIndex == currentState.shuffledIndex -> toIndex
                fromIndex < currentState.shuffledIndex && toIndex >= currentState.shuffledIndex -> currentState.shuffledIndex - 1
                fromIndex > currentState.shuffledIndex && toIndex <= currentState.shuffledIndex -> currentState.shuffledIndex + 1
                else -> currentState.shuffledIndex
            }
            
            _state.update {
                it.copy(
                    shuffledQueue = newShuffledQueue,
                    shuffledIndex = newShuffledIndex
                )
            }
            
            musicService.moveQueueItem(fromIndex, toIndex)
        } else {
            // Reorder regular queue
            if (currentState.queue.isEmpty()) return
            if (fromIndex >= currentState.queue.size || toIndex >= currentState.queue.size) return
            
            val newQueue = currentState.queue.toMutableList()
            val item = newQueue.removeAt(fromIndex)
            newQueue.add(toIndex, item)
            
            // Update current index if the currently playing song's position changed
            val newCurrentIndex = when {
                fromIndex == currentState.currentIndex -> toIndex
                fromIndex < currentState.currentIndex && toIndex >= currentState.currentIndex -> currentState.currentIndex - 1
                fromIndex > currentState.currentIndex && toIndex <= currentState.currentIndex -> currentState.currentIndex + 1
                else -> currentState.currentIndex
            }
            
            _state.update {
                it.copy(
                    queue = newQueue,
                    currentIndex = newCurrentIndex
                )
            }
            
            musicService.moveQueueItem(fromIndex, toIndex)
        }
    }
    
    fun addRandomSongsToQueue(songs: List<Song>, count: Int = 12) {
        if (songs.isEmpty()) return
        
        val currentState = _state.value
        val randomSongs = songs.shuffled().take(count)
        
        if (currentState.currentSong == null) {
            // No song playing, start playing and set all random songs as queue
            val firstSong = randomSongs.first()
            
            _state.update { 
                it.copy(
                    currentSong = firstSong,
                    queue = randomSongs,
                    shuffledQueue = emptyList(),
                    shuffledIndex = 0,
                    currentIndex = 0,
                    isShuffled = false,
                    duration = firstSong.duration
                )
            }
            
            // Extract accent color
            viewModelScope.launch {
                val accentColor = extractAccentColorFromImage(
                    imageUrl = firstSong.albumArtUrl,
                    imageLoader = imageLoader,
                    context = context,
                    defaultColor = Cyan
                )
                _state.update { it.copy(accentColor = accentColor) }
            }
            
            // Play the queue
            musicService.playSongs(randomSongs, 0)
            
            viewModelScope.launch {
                musicRepository.incrementPlayCount(firstSong.id)
                userRepository.addToRecentlyPlayed(firstSong.id)
            }
            
            startProgressUpdates()
            
            // Preload upcoming songs
            preloadUpcomingSongs(randomSongs, 0)
        } else {
            // Add songs to end of current queue
            val newQueue = currentState.queue.toMutableList()
            
            // Add only songs not already in queue
            val songsToAdd = randomSongs.filter { newSong -> 
                newQueue.none { it.id == newSong.id } 
            }
            newQueue.addAll(songsToAdd)
            
            // Also update shuffled queue if shuffle is on
            val newShuffledQueue = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
                val shuffled = currentState.shuffledQueue.toMutableList()
                val shuffledSongsToAdd = songsToAdd.filter { newSong ->
                    shuffled.none { it.id == newSong.id }
                }
                shuffled.addAll(shuffledSongsToAdd)
                shuffled
            } else {
                emptyList()
            }
            
            _state.update { 
                it.copy(
                    queue = newQueue,
                    shuffledQueue = newShuffledQueue
                ) 
            }
            
            // Add songs to queue without restarting current song
            // Only add new songs that aren't already in the player's queue
            viewModelScope.launch {
                val activeQueue = if (currentState.isShuffled && newShuffledQueue.isNotEmpty()) {
                    newShuffledQueue
                } else {
                    newQueue
                }
                // Use addSongsToQueue to append without restarting playback
                val currentQueueSize = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
                    currentState.shuffledQueue.size
                } else {
                    currentState.queue.size
                }
                val songsToAdd = activeQueue.drop(currentQueueSize)
                if (songsToAdd.isNotEmpty()) {
                    musicService.addSongsToQueue(songsToAdd)
                }
            }
            
            // Preload upcoming songs
            val activeQueue = if (currentState.isShuffled && newShuffledQueue.isNotEmpty()) {
                newShuffledQueue
            } else {
                newQueue
            }
            val currentIndex = if (currentState.isShuffled) {
                currentState.shuffledIndex
            } else {
                currentState.currentIndex
            }
            preloadUpcomingSongs(activeQueue, currentIndex)
        }
    }
    
    fun clearUpcomingQueue() {
        val currentState = _state.value
        if (currentState.currentSong == null || currentState.queue.isEmpty()) return
        
        // Keep only the current song
        val newQueue = listOf(currentState.currentSong!!)
        
        _state.update { 
            it.copy(
                queue = newQueue,
                currentIndex = 0
            ) 
        }
        
        musicService.updateQueue(newQueue, 0)
    }
    
    fun addSongToQueue(song: Song) {
        val currentState = _state.value
        
        // Check if song already in queue
        if (currentState.queue.any { it.id == song.id }) {
            return // Song already in queue
        }
        
        if (currentState.currentSong == null) {
            // No song playing, add to queue and start playing this song
            val newQueue = listOf(song)
            _state.update { 
                it.copy(
                    currentSong = song,
                    queue = newQueue,
                    currentIndex = 0,
                    shuffledQueue = emptyList(),
                    shuffledIndex = 0,
                    isShuffled = false,
                    duration = song.duration
                ) 
            }
            
            // Extract accent color
            viewModelScope.launch {
                val accentColor = extractAccentColorFromImage(
                    imageUrl = song.albumArtUrl,
                    imageLoader = imageLoader,
                    context = context,
                    defaultColor = Cyan
                )
                _state.update { it.copy(accentColor = accentColor) }
            }
            
            // Play the song
            playSong(song)
            return
        }
        
        // Add to end of queue
        val newQueue = currentState.queue.toMutableList()
        newQueue.add(song)
        
        // Also add to shuffled queue if shuffle is on
        val newShuffledQueue = if (currentState.isShuffled && currentState.shuffledQueue.isNotEmpty()) {
            val shuffled = currentState.shuffledQueue.toMutableList()
            if (shuffled.none { it.id == song.id }) {
                shuffled.add(song)
            }
            shuffled
        } else {
            emptyList()
        }
        
        _state.update { 
            it.copy(
                queue = newQueue,
                shuffledQueue = newShuffledQueue
            ) 
        }
        
        // Add song to queue without restarting current song
        viewModelScope.launch {
            // Ensure service is connected - connect if not already connected
            if (!musicService.isConnected.value) {
                musicService.connect()
                // Wait for connection
                var attempts = 0
                while (!musicService.isConnected.value && attempts < 10) {
                    kotlinx.coroutines.delay(50)
                    attempts++
                }
            }
            
            // Add the song to queue
            if (musicService.isConnected.value) {
                musicService.addSongsToQueue(listOf(song))
            }
        }
    }
    
    fun toggleRepeat() {
        val newMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = newMode) }
        
        val repeatModeInt = when (newMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        musicService.setRepeatMode(repeatModeInt)
    }

    fun toggleLike() {
        val song = _state.value.currentSong ?: return
        val isLiked = _state.value.isLiked
        
        _state.update { it.copy(isLiked = !isLiked) }
        
        viewModelScope.launch {
            if (isLiked) {
                userRepository.removeFromLikedSongs(song.id)
            } else {
                userRepository.addToLikedSongs(song.id)
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val position = musicService.getCurrentPosition()
                val duration = musicService.getDuration()
                
                _state.update { 
                    it.copy(
                        currentPosition = position,
                        duration = if (duration > 0) duration else it.duration
                    )
                }
                
                // Auto-play next if song ended
                if (duration > 0 && position >= duration - 100) {
                    playNext()
                }
                
                updateCurrentLyric()
            }
        }
    }

    private fun updateCurrentLyric() {
        val song = _state.value.currentSong ?: return
        val position = _state.value.currentPosition
        val lyrics = song.lyrics
        
        if (lyrics.isEmpty()) return
        
        val index = lyrics.indexOfLast { it.timestamp <= position }
        if (index != _state.value.currentLyricIndex) {
            _state.update { it.copy(currentLyricIndex = index) }
        }
    }

    fun updatePosition(position: Long) {
        seekTo(position)
    }

    fun addToLiked() {
        val song = _state.value.currentSong ?: return
        if (!_state.value.isLiked) {
            _state.update { it.copy(isLiked = true) }
            viewModelScope.launch {
                userRepository.addToLikedSongs(song.id)
            }
        }
    }

    fun removeFromLiked() {
        val song = _state.value.currentSong ?: return
        if (_state.value.isLiked) {
            _state.update { it.copy(isLiked = false) }
            viewModelScope.launch {
                userRepository.removeFromLikedSongs(song.id)
            }
        }
    }

    fun addSongToPlaylist(playlistId: String) {
        val song = _state.value.currentSong ?: return
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, song.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}

