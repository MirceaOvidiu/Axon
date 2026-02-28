package com.axon.domain.repository

import com.axon.domain.model.AuthResult
import com.axon.domain.model.AuthState
import com.axon.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val authState: StateFlow<AuthState>

    suspend fun signInWithGoogle(): AuthResult
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult
    suspend fun signUpWithEmailAndPassword(email: String, password: String, displayName: String): AuthResult
    suspend fun signOut(): Boolean
    suspend fun deleteAccount(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun updateProfile(displayName: String?, photoUrl: String?): Boolean
    suspend fun resetPassword(email: String): Boolean
}
