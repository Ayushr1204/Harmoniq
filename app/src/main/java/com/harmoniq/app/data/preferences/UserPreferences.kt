package com.harmoniq.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "harmoniq_settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    companion object {
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val THEME_MODE = stringPreferencesKey("theme_mode") // dark, light, system
        val ACCENT_COLOR = stringPreferencesKey("accent_color") // cyan, purple, green, orange, pink
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality") // auto, high, medium, low
    }

    // Playback Speed
    val playbackSpeed: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PLAYBACK_SPEED] ?: 1.0f
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED] = speed
        }
    }

    // Gapless Playback
    val gaplessPlayback: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[GAPLESS_PLAYBACK] ?: true
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK] = enabled
        }
    }

    // Theme Mode
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "dark"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    // Accent Color
    val accentColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[ACCENT_COLOR] ?: "dynamic"
    }

    suspend fun setAccentColor(color: String) {
        dataStore.edit { preferences ->
            preferences[ACCENT_COLOR] = color
        }
    }

    // Audio Quality
    val audioQuality: Flow<String> = dataStore.data.map { preferences ->
        preferences[AUDIO_QUALITY] ?: "auto"
    }

    suspend fun setAudioQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY] = quality
        }
    }
}

