package com.harmoniq.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.harmoniq.app.data.preferences.UserPreferences
import com.harmoniq.app.navigation.HarmoniqNavGraph
import com.harmoniq.app.service.MusicPlaybackService
import com.harmoniq.app.ui.screens.SplashScreen
import com.harmoniq.app.ui.theme.HarmoniqTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start the music service
        startMusicService()
        
        setContent {
            // Collect theme mode from preferences
            val themeMode by userPreferences.themeMode.collectAsState(initial = "dark")
            
            HarmoniqTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashScreen(
                            onAnimationComplete = { showSplash = false }
                        )
                    } else {
                        HarmoniqNavGraph()
                    }
                }
            }
        }
    }
    
    private fun startMusicService() {
        val serviceIntent = Intent(this, MusicPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

