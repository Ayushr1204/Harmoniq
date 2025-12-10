package com.harmoniq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmoniq.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    init {
        // Check if already logged in
        _state.value = LoginState(isLoggedIn = userRepository.isLoggedIn)
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            val result = userRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoggedIn = true, error = null)
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoggedIn = false,
                        error = exception.message ?: "Failed to sign in"
                    )
                }
            )
        }
    }
    
    fun createAccount(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            val result = userRepository.createAccountWithEmail(email, password, displayName)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoggedIn = true, error = null)
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoggedIn = false,
                        error = exception.message ?: "Failed to create account"
                    )
                }
            )
        }
    }
    
    fun signInAnonymously() {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            val success = userRepository.signInAnonymously()
            if (success) {
                _state.value = _state.value.copy(isLoggedIn = true, error = null)
            } else {
                _state.value = _state.value.copy(
                    isLoggedIn = false,
                    error = "Failed to sign in as guest"
                )
            }
        }
    }
}

