package com.axon.presentation.screens.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.axon.R
import com.axon.domain.model.AuthState
import com.axon.presentation.components.AxonLogo
import com.axon.presentation.components.LogoSize
import com.axon.presentation.screens.dashboard.primaryColor
import com.axon.presentation.theme.AxonTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    when (authState) {
        AuthState.AUTHENTICATED -> {
            onNavigateToDashboard()
            return
        }
        else -> {}
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Credential Manager Google Sign-In
    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(R.string.default_web_client_id)

    fun launchGoogleSignIn() {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(googleIdToken.idToken)
                } else {
                    Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(context, e.message ?: "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }

    AxonTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E14),
                            Color(0xFF151C25),
                            Color(0xFF0A0E14)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {

                    Spacer(modifier = Modifier.height(24.dp))

                    AxonLogo(
                        textColor = Color.White,
                        size = LogoSize.LARGE
                    )
                }

                // Auth Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF151C25).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (showForgotPassword) "Reset Password"
                                   else if (isSignUp) "Join Axon"
                                   else "Welcome Back",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = if (showForgotPassword) "Enter your email to reset password"
                                   else if (isSignUp) "Create your account to get started"
                                   else "Sign in to continue your journey",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        if (showForgotPassword) {
                            ForgotPasswordForm(
                                email = email,
                                onEmailChange = { email = it },
                                onResetPassword = { viewModel.resetPassword(email) },
                                onBackToSignIn = {
                                    showForgotPassword = false
                                    viewModel.clearError()
                                },
                                isLoading = uiState.isLoading,
                                resetEmailSent = uiState.resetEmailSent
                            )
                        } else {
                            AuthForm(
                                isSignUp = isSignUp,
                                email = email,
                                password = password,
                                displayName = displayName,
                                onEmailChange = { email = it },
                                onPasswordChange = { password = it },
                                onDisplayNameChange = { displayName = it },
                                onSubmit = {
                                    if (isSignUp) {
                                        viewModel.signUpWithEmailAndPassword(email, password, displayName)
                                    } else {
                                        viewModel.signInWithEmailAndPassword(email, password)
                                    }
                                },
                                onToggleMode = {
                                    isSignUp = !isSignUp
                                    viewModel.clearError()
                                },
                                onForgotPassword = {
                                    showForgotPassword = true
                                    viewModel.clearError()
                                },
                                onGoogleSignIn = ::launchGoogleSignIn,
                                isLoading = uiState.isLoading
                            )
                        }

                        uiState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = error,
                                    color = Color.Red.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))
            }
        }
    }
}

@Composable
private fun AuthForm(
    isSignUp: Boolean,
    email: String,
    password: String,
    displayName: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Input Fields
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Display Name", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email", color = Color.Gray, fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password", color = Color.Gray, fontSize = 14.sp) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Primary Action Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                disabledContainerColor = primaryColor.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() &&
                     (!isSignUp || displayName.isNotBlank())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isSignUp) "Create Account" else "Sign In",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Divider with "OR" label
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.Gray.copy(alpha = 0.3f)
            )
            Text(
                text = "  or  ",
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }

        // Google Sign-In button
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.Gray
            ),
            enabled = !isLoading
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Bottom Actions
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (isSignUp) "Sign In" else "Sign Up",
                    color = primaryColor.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light
                )
            }

            if (!isSignUp) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot Password?",
                        color = primaryColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordForm(
    email: String,
    onEmailChange: (String) -> Unit,
    onResetPassword: () -> Unit,
    onBackToSignIn: () -> Unit,
    isLoading: Boolean,
    resetEmailSent: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (resetEmailSent) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Green.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "✓ Password reset email sent!\nCheck your inbox and follow the instructions.",
                    color = Color.Green.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = Color.Gray, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = primaryColor,
                unfocusedLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !resetEmailSent
        )

        if (!resetEmailSent) {
            Button(
                onClick = onResetPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && email.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Reset Email",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        TextButton(
            onClick = onBackToSignIn,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "← Back to Sign In",
                color = primaryColor.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
