package com.harmoniq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.preferences.UserPreferences
import com.harmoniq.app.service.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val playbackSpeed: Float = 1.0f,
    val gaplessPlayback: Boolean = true,
    val themeMode: String = "dark",
    val accentColor: String = "dynamic",
    val audioQuality: String = "auto",
    val isRefreshing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val musicService: MusicServiceConnection
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.playbackSpeed.collect { speed ->
                _state.update { it.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            userPreferences.gaplessPlayback.collect { enabled ->
                _state.update { it.copy(gaplessPlayback = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            userPreferences.accentColor.collect { color ->
                _state.update { it.copy(accentColor = color) }
            }
        }
        viewModelScope.launch {
            userPreferences.audioQuality.collect { quality ->
                _state.update { it.copy(audioQuality = quality) }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferences.setPlaybackSpeed(speed)
            // Apply to music service immediately
            musicService.setPlaybackSpeed(speed)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setGaplessPlayback(enabled)
            // Apply to music service immediately
            musicService.setGaplessPlayback(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(color)
        }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch {
            userPreferences.setAudioQuality(quality)
        }
    }

    fun rescanLibrary(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            // Simulate rescan delay
            kotlinx.coroutines.delay(1500)
            _state.update { it.copy(isRefreshing = false) }
            onComplete()
        }
    }
}

