package com.harmoniq.app.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.harmoniq.app.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPosition.value = newPosition.positionMs
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _currentPosition.value = mediaController?.currentPosition ?: 0L
                }
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                _isConnected.value = true
            },
            MoreExecutors.directExecutor()
        )
    }

    fun disconnect() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        _isConnected.value = false
    }

    fun playSong(song: Song) {
        mediaController?.let { controller ->
            val mediaItem = MusicPlaybackService.createMediaItem(song)
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.let { controller ->
            val mediaItems = songs.map { MusicPlaybackService.createMediaItem(it) }
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _currentPosition.value = position
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun setShuffleMode(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(repeatMode: Int) {
        mediaController?.repeatMode = repeatMode
    }
    
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }
    
    fun updateQueue(songs: List<Song>, currentIndex: Int) {
        mediaController?.let { controller ->
            // Get current position and playback state to maintain it
            val currentPosition = controller.currentPosition
            val wasPlaying = controller.isPlaying
            
            // Check if current song is the same - if so, we might be able to avoid the update
            val currentMediaItem = controller.currentMediaItem
            val currentMediaId = currentMediaItem?.mediaId
            val isSameCurrentSong = currentMediaId != null && 
                                   currentIndex < songs.size && 
                                   songs[currentIndex].id == currentMediaId
            
            val mediaItems = songs.map { MusicPlaybackService.createMediaItem(it) }
            
            // Set media items with current position to maintain playback position
            controller.setMediaItems(mediaItems, currentIndex, currentPosition)
            
            // Immediately resume playback if it was playing
            // For Media3, we need to ensure playback continues smoothly
            if (wasPlaying) {
                // Call play() immediately - Media3 should handle the transition smoothly
                // If the current song is the same, playback should continue without pause
                controller.play()
            }
        }
    }
    
    fun addSongsToQueue(songs: List<Song>) {
        mediaController?.let { controller ->
            // Only add songs if we're not changing the current song
            // This prevents restarting playback
            val mediaItems = songs.map { MusicPlaybackService.createMediaItem(it) }
            val currentSize = controller.mediaItemCount
            controller.addMediaItems(currentSize, mediaItems)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.let { controller ->
            val currentParams = controller.playbackParameters
            controller.playbackParameters = currentParams.withSpeed(speed)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        // Gapless playback in Media3 is handled automatically by ExoPlayer
        // when proper audio attributes are set (which we already do)
        // The preference is stored and can be used for UI indication
        // ExoPlayer handles crossfade/gapless transitions automatically
    }

    fun getCurrentPosition(): Long {
        val pos = mediaController?.currentPosition ?: 0L
        _currentPosition.value = pos
        return pos
    }

    fun getDuration(): Long {
        return mediaController?.duration ?: 0L
    }
    
    fun getCurrentMediaItem(): MediaItem? {
        return mediaController?.currentMediaItem
    }
    
    // Flow for position updates
    fun positionFlow(): Flow<Long> = flow {
        while (true) {
            emit(getCurrentPosition())
            kotlinx.coroutines.delay(100)
        }
    }
}

