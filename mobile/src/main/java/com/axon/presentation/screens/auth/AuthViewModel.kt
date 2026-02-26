package com.axon.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axon.domain.model.AuthState
import com.axon.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val authState = authRepository.authState
    val currentUser = authRepository.currentUser

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogleIdToken(idToken)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (!result.isSuccess) result.errorMessage else null
            )
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInWithEmailAndPassword(email, password)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (!result.isSuccess) result.errorMessage else null
            )
        }
    }

    fun signUpWithEmailAndPassword(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signUpWithEmailAndPassword(email, password, displayName)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (!result.isSuccess) result.errorMessage else null
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = authRepository.signOut()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (!success) "Sign out failed" else null
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val success = authRepository.resetPassword(email)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (!success) "Failed to send reset email" else null,
                resetEmailSent = success
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearResetEmailStatus() {
        _uiState.value = _uiState.value.copy(resetEmailSent = false)
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resetEmailSent: Boolean = false
)
