package com.axon.presentation.screens.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axon.R
import com.axon.domain.model.AuthState
import com.axon.presentation.screens.dashboard.backgroundDark
import com.axon.presentation.screens.dashboard.cardDark
import com.axon.presentation.screens.dashboard.primaryColor
import com.axon.presentation.theme.AxonTheme

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Navigate to dashboard if authenticated
    when (authState) {
        AuthState.AUTHENTICATED -> {
            onNavigateToDashboard()
            return
        }
        else -> {
            // Show auth screen
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo and Title
                    Image(
                        painter = painterResource(id = R.drawable.pulse),
                        contentDescription = "Axon Logo",
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Axon",
                        color = primaryColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (showForgotPassword) "Reset Password"
                               else if (isSignUp) "Create Account"
                               else "Welcome Back",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    if (showForgotPassword) {
                        ForgotPasswordForm(
                            email = email,
                            onEmailChange = { email = it },
                            onResetPassword = {
                                viewModel.resetPassword(email)
                            },
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
                            isLoading = uiState.isLoading
                        )
                    }

                    // Error Message
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSignUp) {
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display Name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() &&
                     (!isSignUp || displayName.isNotBlank())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (isSignUp) "Already have an account?" else "Need an account?",
                    color = primaryColor
                )
            }

            if (!isSignUp) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot Password?",
                        color = primaryColor
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (resetEmailSent) {
            Text(
                text = "Password reset email sent! Check your inbox.",
                color = Color.Green,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            Text(
                text = "Enter your email address and we'll send you a password reset link.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray
            ),
            enabled = !resetEmailSent
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!resetEmailSent) {
            Button(
                onClick = onResetPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading && email.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        TextButton(onClick = onBackToSignIn) {
            Text(
                text = "Back to Sign In",
                color = primaryColor
            )
        }
    }
}
