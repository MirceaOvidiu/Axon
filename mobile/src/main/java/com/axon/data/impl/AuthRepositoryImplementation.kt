package com.axon.data.impl

import android.content.Context
import com.axon.domain.model.AuthResult
import com.axon.domain.model.AuthState
import com.axon.domain.model.User
import com.axon.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class AuthRepositoryImplementation @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val currentUser: StateFlow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = firebaseAuth.currentUser?.toUser()
    )

    override val authState: StateFlow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val state = if (auth.currentUser != null) AuthState.AUTHENTICATED
                        else AuthState.NOT_AUTHENTICATED
            trySend(state)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = if (firebaseAuth.currentUser != null) AuthState.AUTHENTICATED
                       else AuthState.NOT_AUTHENTICATED
    )

    override suspend fun signInWithGoogle(): AuthResult {
        return try {
            // Configure Google Sign In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.axon.R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)

            AuthResult(
                user = null,
                isSuccess = false,
                errorMessage = "Google Sign In must be handled in the UI layer"
            )
        } catch (e: Exception) {
            AuthResult(
                user = null,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser()

            if (user != null) {
                updateUserInFirestore(user)
            }

            AuthResult(
                user = user,
                isSuccess = user != null,
                errorMessage = if (user == null) "Authentication failed" else null
            )
        } catch (e: Exception) {
            AuthResult(
                user = null,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String, displayName: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

            // Update the profile with display name
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
            )?.await()

            val user = result.user?.toUser()

            if (user != null) {
                updateUserInFirestore(user)
            }

            AuthResult(
                user = user,
                isSuccess = user != null,
                errorMessage = if (user == null) "Account creation failed" else null
            )
        } catch (e: Exception) {
            AuthResult(
                user = null,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun signOut(): Boolean {
        return try {
            firebaseAuth.signOut()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteAccount(): Boolean {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Delete user data from Firestore
                firestore.collection("users").document(user.uid).delete().await()

                // Delete the authentication account
                user.delete().await()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toUser()
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Boolean {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .setPhotoUri(photoUrl?.toUri())
                    .build()

                user.updateProfile(profileUpdates).await()

                // Update Firestore
                val updatedUser = user.toUser()
                updateUserInFirestore(updatedUser)

                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun resetPassword(email: String): Boolean {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun updateUserInFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            uid = uid,
            email = email ?: "",
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            lastLoginAt = System.currentTimeMillis()
        )
    }
}
