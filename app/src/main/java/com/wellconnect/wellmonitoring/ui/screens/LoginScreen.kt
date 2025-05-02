package com.wellconnect.wellmonitoring.ui.screens

import android.os.Build
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.LoginRequest
import com.wellconnect.wellmonitoring.data.UserData
import com.wellconnect.wellmonitoring.data.UserDataStore
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.components.PasswordField
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.utils.encryptPassword
import com.wellconnect.wellmonitoring.utils.getBaseApiUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.InternalSerializationApi

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun LoginScreen(
    userDataStore: UserDataStore,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val baseUrl = getBaseApiUrl(context)
    val api = RetrofitBuilder.getServerApi(context)

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

            // Use PasswordField for password input
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
                    scope.launch {
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
                            
                            Log.d("LoginScreen", "Attempting to login with email: $email")
                            
                            try {
                                val encrypted = encryptPassword(password)
                                val req = LoginRequest(email.trim(), encrypted)
                                Log.d("LoginScreen", "Base URL: $baseUrl")
                                
                                // Add a timeout to the request
                                withContext(Dispatchers.IO) {
                                    val res = try {
                                        // Use a timeout for the request
                                        withTimeout(10000L) { // 10 second timeout
                                            api.login(req)
                                        }
                                    } catch (e: TimeoutCancellationException) {
                                        Log.e("LoginScreen", "Login request timed out", e)
                                        errorMessage = "Login request timed out. The server_crt may be unavailable."
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar(errorMessage!!)
                                        }
                                        return@withContext
                                    } catch (e: Exception) {
                                        Log.e("LoginScreen", "Exception during login request", e)
                                        errorMessage = "Connection error: ${e.localizedMessage ?: "Unknown error"}"
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar(errorMessage!!)
                                        }
                                        return@withContext
                                    }
                                    

                                    if (res.isSuccessful && res.body()?.status == "success" && res.body()?.data?.user != null) {
                                        Log.d("LoginScreen", "Login successful")
                                        val serverData = res.body()!!.data!!
                                        val user = serverData.user
                                        
                                        // Create UserData with data from server_crt
                                        val userData = UserData(
                                            email = user.email,
                                            firstName = user.firstName,
                                            lastName = user.lastName,
                                            username = user.username,
                                            role = user.role,
                                            location = serverData.location?.let {
                                                com.wellconnect.wellmonitoring.data.Location(
                                                    latitude = it.latitude,
                                                    longitude = it.longitude
                                                )
                                            } ?: com.wellconnect.wellmonitoring.data.Location(0.0, 0.0),

                                        )
                                        
                                        userDataStore.saveUserData(userData)
                                        
                                        withContext(Dispatchers.Main) {
                                            navController.navigate(Routes.HOME_SCREEN) {
                                                popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        val responseCode = res.code()
                                        val responseBody = res.errorBody()?.string() ?: "Unknown error"
                                        Log.e("LoginScreen", "Login failed with code $responseCode: $responseBody")
                                        
                                        errorMessage = when (responseCode) {
                                            401 -> "Invalid email or password"
                                            403 -> "Account locked. Please contact support."
                                            404 -> "User not found"
                                            500 -> "Server error. Please try again later."
                                            503 -> "Server unavailable. Please try again later."
                                            else -> res.body()?.message ?: "Login failed (Error $responseCode)"
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar(errorMessage!!)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Unexpected error during login", e)
                                errorMessage = "Login failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                                snackbarHostState.showSnackbar(errorMessage!!)
                            }
                        } catch (e: Exception) {
                            errorMessage = "Login failed: ${e.message}"
                            snackbarHostState.showSnackbar(errorMessage!!)
                            Log.e("LoginScreen", "Login failed", e)
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


