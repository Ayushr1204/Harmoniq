package com.harmoniq.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.model.Playlist
import com.harmoniq.app.data.model.User
import com.harmoniq.app.data.repository.UserRepository
import com.harmoniq.app.util.CloudinaryUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

data class ProfileEditState(
    val user: User? = null,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()
    
    init {
        loadUserData()
    }
    
    fun loadUserData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // Load user - this uses a snapshot listener so it will update automatically
            userRepository.getCurrentUser().collect { user ->
                _state.value = _state.value.copy(user = user, isLoading = false)
            }
        }
        
        viewModelScope.launch {
            // Load playlists
            userRepository.getUserPlaylists().collect { playlists ->
                _state.value = _state.value.copy(playlists = playlists)
            }
        }
    }
    
    suspend fun uploadProfilePicture(uri: Uri): String? {
        return try {
            android.util.Log.d("ProfileEditViewModel", "Starting profile picture upload from URI: $uri")
            
            // Read image from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("ProfileEditViewModel", "Could not open input stream from URI")
                return null
            }
            
            // Create temporary file
            val tempFile = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            android.util.Log.d("ProfileEditViewModel", "Temporary file created: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
            
            // Upload to Cloudinary
            val imageUrl = uploadToCloudinary(tempFile)
            
            // Clean up temp file
            tempFile.delete()
            
            if (imageUrl != null) {
                android.util.Log.d("ProfileEditViewModel", "Profile picture uploaded successfully: $imageUrl")
            } else {
                android.util.Log.e("ProfileEditViewModel", "Failed to upload profile picture")
            }
            
            imageUrl
        } catch (e: Exception) {
            android.util.Log.e("ProfileEditViewModel", "Error uploading profile picture: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun uploadToCloudinary(file: File): String? {
        return CloudinaryUploader.uploadImage(file, folder = "profile_pictures")
    }
    
    suspend fun updateProfile(
        displayName: String,
        email: String,
        photoUrl: String? = null
    ): Boolean {
        return try {
            val updates = mutableMapOf<String, String?>()
            if (displayName.isNotBlank()) {
                updates["displayName"] = displayName
            }
            if (email.isNotBlank()) {
                updates["email"] = email
            }
            photoUrl?.let { updates["photoUrl"] = it }
            
            userRepository.updateUserProfile(
                displayName = updates["displayName"],
                email = updates["email"],
                photoUrl = updates["photoUrl"]
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
            false
        }
    }
    
    private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
        value = update(value)
    }
}

