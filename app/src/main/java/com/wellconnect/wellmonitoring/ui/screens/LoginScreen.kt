package com.wellconnect.wellmonitoring.ui.screens

import android.os.Build
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.ui.components.PasswordField
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.utils.encryptPassword
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel
) {
    val userState = userViewModel.state.value
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = userState is UiState.Loading

    // Observe login state changes
    LaunchedEffect(userState) {
        when (userState) {
            is UiState.Success -> {
                // Navigate to home only on successful login
                navController.navigate(Routes.HOME_SCREEN) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(userState.message)
            }
            else -> { /* No action needed */ }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible },
                passwordStrength = null // No strength display for login
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }
                            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Please enter a valid email address"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }
                            val encryptedPassword = encryptPassword(password)
                            val request = LoginRequest(
                                email = email.trim(),
                                password = encryptedPassword,

                            )
                            userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.Login(request))
                        } catch (e: Exception) {
                            errorMessage = "Login failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                            snackbarHostState.showSnackbar(errorMessage!!)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Login")
            }

            TextButton(
                onClick = {
                    navController.navigate(Routes.REGISTER_SCREEN)
                }) {
                Text("Don't have an account? Sign up")
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}


