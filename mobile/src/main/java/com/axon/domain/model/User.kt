package com.axon.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
)

data class AuthResult(
    val user: User?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

enum class AuthState {
    LOADING,
    AUTHENTICATED,
    NOT_AUTHENTICATED,
    ERROR
}
