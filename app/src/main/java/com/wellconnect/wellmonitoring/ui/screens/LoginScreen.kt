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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.UserDataStore
import com.wellconnect.wellmonitoring.network.LoginRequest
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64
import com.wellconnect.wellmonitoring.utils.InputSanitizer

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    userDataStore: UserDataStore,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isRegistrationMode by remember { mutableStateOf(false) }
    val api = RetrofitBuilder.create("http://192.168.0.98:8090")

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
                text = if (isRegistrationMode) "Register" else "Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isRegistrationMode) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Validate inputs
                            val validationResults = InputSanitizer.validateUserInput(
                                email = email,
                                password = password
                            )

                            // Check if any validation failed
                            val failedValidations = validationResults.filter { !it.value.isValid }
                            if (failedValidations.isNotEmpty()) {
                                errorMessage = failedValidations.values.first().errorMessage
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            // Use sanitized values
                            val sanitizedEmail = validationResults["email"]!!.sanitizedValue!!
                            val sanitizedPassword = validationResults["password"]!!.sanitizedValue!!

                            val encryptedPassword = encryptPassword(sanitizedPassword)
                            val loginRequest = LoginRequest(
                                email = sanitizedEmail,
                                password = encryptedPassword
                            )

                            val response = api.login(loginRequest)
                            
                            if (response.isSuccessful && response.body() != null) {
                                val loginResponse = response.body()!!
                                if (loginResponse.status == "success" && loginResponse.userData?.user != null) {
                                    userDataStore.saveUserData(
                                        com.wellconnect.wellmonitoring.data.UserData(
                                            email = loginResponse.userData.user.email,
                                            firstName = loginResponse.userData.user.firstName,
                                            lastName = loginResponse.userData.user.lastName,
                                            username = loginResponse.userData.user.username,
                                            role = loginResponse.userData.user.role
                                        )
                                    )
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                    }
                                } else {
                                    errorMessage = loginResponse.message
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                }
                            } else {
                                errorMessage = "Login failed. Please check your credentials."
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
                Text(if (isRegistrationMode) "Register" else "Login")
            }

            TextButton(
                onClick = { isRegistrationMode = !isRegistrationMode }
            ) {
                Text(
                    if (isRegistrationMode) 
                        "Already have an account? Login" 
                    else 
                        "Don't have an account? Sign up"
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun validateFields(
    email: String,
    password: String,
    isRegistration: Boolean,
    firstName: String,
    lastName: String,
    username: String
): Boolean {
    if (email.isBlank() || password.isBlank()) return false
    if (isRegistration) {
        if (firstName.isBlank() || lastName.isBlank() || username.isBlank()) return false
    }
    return true
}

@RequiresApi(Build.VERSION_CODES.O)
private fun encryptPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return Base64.getEncoder().encodeToString(digest)
}

