package com.wellconnect.wellmonitoring.ui.screens

import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.network.RegisterRequest
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64
import com.wellconnect.wellmonitoring.utils.InputSanitizer

data class PasswordStrength(
    val strength: String,
    val color: Color,
    val message: String
)

fun getPasswordStrength(password: String): PasswordStrength {
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }

    val conditions = listOf(hasMinLength, hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar)
    val metConditions = conditions.count { it }

    return when {
        password.isEmpty() -> PasswordStrength("Empty", Color.Gray, "Enter a password")
        !hasMinLength && metConditions <= 1 -> PasswordStrength(
            "Very Weak",
            Color.Red.copy(alpha = 0.7f),
            "Password is too short and lacks complexity"
        )
        metConditions == 1 -> PasswordStrength(
            "Weak",
            Color(0xFFFF6B6B),
            "Add uppercase, numbers, or special characters"
        )
        metConditions == 2 -> PasswordStrength(
            "Medium",
            Color(0xFFFFB84D),
            "Good start! Add more variety for stronger security"
        )
        metConditions == 3 || metConditions == 4 -> PasswordStrength(
            "Strong",
            Color(0xFF59C135),
            "Strong password!"
        )
        else -> PasswordStrength(
            "Very Strong",
            Color(0xFF2E7D32),
            "Excellent password strength!"
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val api = RetrofitBuilder.create("http://192.168.0.98:8090")

    val passwordStrength = getPasswordStrength(password)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

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

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
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
                
                // Password strength indicator
                if (password.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = when (passwordStrength.strength) {
                                "Very Weak" -> 0.2f
                                "Weak" -> 0.4f
                                "Medium" -> 0.6f
                                "Strong" -> 0.8f
                                "Very Strong" -> 1f
                                else -> 0f
                            },
                            color = passwordStrength.color,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${passwordStrength.strength}: ${passwordStrength.message}",
                            color = passwordStrength.color,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Validate all inputs
                            val validationResults = InputSanitizer.validateUserInput(
                                email = email,
                                password = password,
                                username = username,
                                firstName = firstName,
                                lastName = lastName
                            )

                            // Check if any validation failed
                            val failedValidations = validationResults.filter { !it.value.isValid }
                            if (failedValidations.isNotEmpty()) {
                                errorMessage = failedValidations.values.first().errorMessage
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            // Check if passwords match
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            // Use sanitized values
                            val sanitizedEmail = validationResults["email"]!!.sanitizedValue!!
                            val sanitizedPassword = validationResults["password"]!!.sanitizedValue!!
                            val sanitizedUsername = validationResults["username"]!!.sanitizedValue!!
                            val sanitizedFirstName = validationResults["firstName"]!!.sanitizedValue!!
                            val sanitizedLastName = validationResults["lastName"]!!.sanitizedValue!!

                            val encryptedPassword = encryptPassword(sanitizedPassword)
                            val registerRequest = RegisterRequest(
                                email = sanitizedEmail,
                                password = encryptedPassword,
                                firstName = sanitizedFirstName,
                                lastName = sanitizedLastName,
                                username = sanitizedUsername
                            )

                            val response = api.register(registerRequest)
                            
                            if (response.isSuccessful && response.body() != null) {
                                val registerResponse = response.body()!!
                                if (registerResponse.status == "success") {
                                    // Save user data and navigate directly to home screen
                                    userDataStore.saveUserData(
                                        com.wellconnect.wellmonitoring.data.UserData(
                                            email = sanitizedEmail,
                                            firstName = sanitizedFirstName,
                                            lastName = sanitizedLastName,
                                            username = sanitizedUsername,
                                            role = "user", // Default role for new registrations
                                            themePreference = 0 // Default to system theme
                                        )
                                    )
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                    }
                                } else {
                                    errorMessage = registerResponse.message
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                }
                            } else {
                                errorMessage = "Registration failed. Please try again."
                                snackbarHostState.showSnackbar(errorMessage!!)
                            }
                        } catch (e: Exception) {
                            errorMessage = "Registration failed: ${e.message}"
                            snackbarHostState.showSnackbar(errorMessage!!)
                            Log.e("SignUpScreen", "Registration failed", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Sign Up")
            }

            TextButton(
                onClick = { navController.navigate(Routes.LOGIN_SCREEN) }
            ) {
                Text("Already have an account? Login")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun validateFields(
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    username: String
): Boolean {
    return email.isNotBlank() && 
           password.isNotBlank() && 
           firstName.isNotBlank() && 
           lastName.isNotBlank() && 
           username.isNotBlank()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun encryptPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return Base64.getEncoder().encodeToString(digest)
} 