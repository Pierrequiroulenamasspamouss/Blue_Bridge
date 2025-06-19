package com.bluebridge.bluebridgeapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.AppEvent
import com.bluebridge.bluebridgeapp.data.AppEventChannel
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.utils.encryptPassword
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    val userState by userViewModel.state

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }


    // Handle user state changes
    LaunchedEffect(userState) {
        when (userState) {
            is UiState.Success -> {
                navController.navigate(Routes.HOME_SCREEN) {
                    popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                }
            }
            is UiState.Error -> {
                isLoading = false
/* NOT USED FOR NOW. TODO : use them by fixing the errorMessage that is returned
                val errorMessage = when ((userState as UiState.Error).message) {
                    "Invalid email or password" -> "The email or password you entered is incorrect. Please check your credentials and try again."
                    "Account not found" -> "No account found with this email address. Please check your email or sign up for a new account."
                    "Server error" -> "Unable to connect to the server. Please check your internet connection and try again later."
                    "Network error" -> "Please check your internet connection and try again."
                    "Invalid credentials" -> "The email or password format is invalid. Please check your input."
                    "Account locked" -> "Your account has been locked due to multiple failed attempts. Please contact support."
                    "Email not verified" -> "Please verify your email address before logging in."
                    "Session expired" -> "Your session has expired. Please log in again."
                    "Invalid email format" -> "Please enter a valid email address."
                    "Password too short" -> "Password must be at least 8 characters long."
                    "Password too weak" -> "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character."
                    "Account disabled" -> "This account has been disabled. Please contact support for assistance."
                    "Too many attempts" -> "Too many login attempts. Please try again later or reset your password."
                    "Maintenance mode" -> "The system is currently under maintenance. Please try again later."
                    "Invalid loginToken" -> "Your login session is invalid. Please log in again."
                    "Rate limited" -> "Too many login attempts. Please wait a few minutes before trying again."
                    else -> "An unexpected error occurred. Please try again or contact support if the problem persists."
                }
                AppEventChannel.sendEvent(AppEvent.ShowError(errorMessage))

 */
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true
            )

            // Login button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        coroutineScope.launch {
                            AppEventChannel.sendEvent(AppEvent.ShowError("Please enter both email and password"))
                        }
                            return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val request = LoginRequest(email = email, password = encryptPassword(password))
                            userViewModel.login(request)
                        } catch (e: Exception) {
                            isLoading = false
                            AppEventChannel.sendEvent(AppEvent.ShowError("An error occurred during login"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            // Register link
            TextButton(
                onClick = { navController.navigate(Routes.REGISTER_SCREEN) }
            ) {
                Text("Don't have an account? Sign up")
            }

            // Guest login button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            userViewModel.loginAsGuest()
                        } catch (e: Exception) {
                            AppEventChannel.sendEvent(AppEvent.ShowError("Failed to login as guest : $e"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue as Guest")
            }
        }
    }
}