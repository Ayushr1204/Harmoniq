package com.harmoniq.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.harmoniq.app.MainActivity
import com.harmoniq.app.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var cache: SimpleCache? = null
    
    companion object {
        private const val CHANNEL_ID = "harmoniq_music_channel"
        private const val NOTIFICATION_ID = 1
        private const val CACHE_SIZE = 100 * 1024 * 1024L // 100 MB - enough for 2-3 songs
        private const val CACHE_DIR = "exoplayer_cache"
        
        fun createMediaItem(song: Song): MediaItem {
            return MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistName)
                        .setAlbumTitle(song.albumName)
                        .setArtworkUri(android.net.Uri.parse(song.albumArtUrl))
                        .build()
                )
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        // Initialize cache for preloading songs
        val cacheDir = File(getCacheDir(), CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        cache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
        )
        
        // Create DataSourceFactory with caching
        val upstreamFactory = DefaultDataSourceFactory(this, "Harmoniq")
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        
        // Create LoadControl with increased buffer for preloading
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000, // Min buffer: 50 seconds
                120000, // Max buffer: 120 seconds (2 minutes)
                2500, // Buffer for playback: 2.5 seconds
                5000 // Buffer for playback after rebuffer: 5 seconds
            )
            .setTargetBufferBytes(-1) // No limit on buffer size
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        
        // Create ExoPlayer with cache and increased buffer
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .build()

        // Create pending intent for notification click
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
            
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback notification"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Harmoniq")
        .setContentText("Playing music")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setOngoing(true)
        .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        cache?.release()
        cache = null
        super.onDestroy()
    }
}

