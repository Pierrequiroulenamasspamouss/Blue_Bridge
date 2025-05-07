package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.UserEvent
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.WaterNeed
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.components.MiniMap
import com.wellconnect.wellmonitoring.ui.components.PasswordField
import com.wellconnect.wellmonitoring.ui.components.WaterUsageTypeSelector
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.utils.encryptPassword
import com.wellconnect.wellmonitoring.utils.getPasswordStrength
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("MutableCollectionMutableState", "AutoboxingStateCreation")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPriority by remember { mutableStateOf(6) }  // Default priority for "Other"
    var customType by remember { mutableStateOf("") }  // For custom type input
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    val waterNeeds = remember { mutableStateOf(mutableListOf<WaterNeed>()) }
    var currentWaterNeedAmount by remember { mutableStateOf("") }
    var currentWaterNeedType by remember { mutableStateOf("") }
    var currentWaterNeedDescription by remember { mutableStateOf("") }
    var isWellOwner by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val api = RetrofitBuilder.getServerApi(context)

    // State for location
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }

    // Get current location if permission is granted
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        lastUpdated = currentTime
                    )
                }
            }
        }
    }

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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Required fields section
            Text(
                text = "Required Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Email (required)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Password (required)
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password *",
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible },
                passwordStrength = passwordStrength
            )
            
            // Confirm password (required)
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password *",
                isVisible = confirmPasswordVisible,
                onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
            )
            
            // First Name (required)
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Last Name (required)
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Optional fields section
            Text(
                text = "Optional Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            
            // Username (optional)
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Location selection using MiniMap
            Text(
                text = "Your Location",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            
            // MiniMap component
            MiniMap(
                currentLocation = currentLocation,
                selectedLocation = selectedLocation,
                onLocationSelected = { location ->
                    selectedLocation = location
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            
            // Phone Number (optional)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Water Needs (optional)
            if (waterNeeds.value.isNotEmpty()) {
                Text(
                    text = "Water Needs",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Display added water needs
                waterNeeds.value.forEach { need ->
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Amount: ${need.amount} liters", style = MaterialTheme.typography.bodyMedium)
                            Text("Type: ${need.usageType}", style = MaterialTheme.typography.bodySmall)
                            Text("Priority: P${need.priority}", style = MaterialTheme.typography.bodySmall)
                            Text("Description: ${need.description}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Collapsible water needs section
            var showWaterNeedsSection by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showWaterNeedsSection = !showWaterNeedsSection },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showWaterNeedsSection) "Hide Water Needs Form" else "Add Water Needs (Optional)")
            }
            
            if (showWaterNeedsSection) {
                OutlinedTextField(
                    value = currentWaterNeedAmount,
                    onValueChange = {
                        if (it.isEmpty() || it.all(Char::isDigit)) currentWaterNeedAmount = it
                    },
                    label = { Text("Amount (liters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                WaterUsageTypeSelector(
                    currentWaterNeedType = currentWaterNeedType,
                    onWaterNeedTypeChange = { currentWaterNeedType = it },
                    currentPriority = currentPriority,
                    onPriorityChange = { currentPriority = it },
                    customType = customType,
                    onCustomTypeChange = { customType = it }
                )
                
                OutlinedTextField(
                    value = currentWaterNeedDescription,
                    onValueChange = { currentWaterNeedDescription = it },
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        val amount = currentWaterNeedAmount.toIntOrNull()
                        if (amount != null && amount > 0) {
                            // Get priority based on type (or use the selected priority for "Other")
                            val priority = when (currentWaterNeedType) {
                                "Absolute emergency" -> 0
                                "Medical" -> 1
                                "Drinking" -> 2
                                "Farming" -> 3
                                "Industry" -> 4
                                else -> currentPriority // Use selected priority for "Other"
                            }

                            waterNeeds.value.add(
                                WaterNeed(
                                    amount = amount,
                                    usageType = if (currentWaterNeedType == "Other") customType else currentWaterNeedType,
                                    description = currentWaterNeedDescription,
                                    priority = priority
                                )
                            )
                            currentWaterNeedAmount = ""
                            currentWaterNeedType = ""
                            currentWaterNeedDescription = ""
                            customType = ""
                            currentPriority = 6 // Reset to default for next entry
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Water Need")
                }
            }

            // Well owner toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Are you a well owner?")
                Switch(checked = isWellOwner, onCheckedChange = { isWellOwner = it })
            }

            // Register button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Validate required inputs
                            if (email.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()) {
                                errorMessage = "Please fill in all required fields (marked with *)"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Please enter a valid email address"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                snackbarHostState.showSnackbar(errorMessage!!)
                                return@launch
                            }

                            // Use default values for optional fields
                            val actualUsername = username.takeIf { it.isNotBlank() } ?: firstName.lowercase()
                            
                            // Use selected location or current location or default
                            val locationData = selectedLocation ?: currentLocation ?: Location(
                                latitude = 0.0,
                                longitude = 0.0,
                                lastUpdated = currentTime
                            )

                            val encrypted = encryptPassword(password)
                            var request = RegisterRequest(
                                email = email.trim(),
                                password = encrypted,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                username = actualUsername,
                                location = locationData,
                                waterNeeds = waterNeeds.value,
                                isWellOwner = isWellOwner,
                                role = "user",
                                phoneNumber = phoneNumber.trim().ifBlank { null },
                                themePreference = 0 // Default theme
                            )

                            // Send registration request
                            Log.d("RegisterScreen", "Sending registration request: $request")

                            // Call API and wait for response
                            val registrationSuccess = api.register(request)
                            
                            if (registrationSuccess.isSuccessful && registrationSuccess.body()?.status == "success") {
                                // If server reports success, show success message

                                userViewModel.handleEvent(
                                    UserEvent.Login(
                                        LoginRequest(
                                            email = email,
                                            password = encrypted
                                        )
                                    )
                                )
                                snackbarHostState.showSnackbar("User was successfully registered")
                                
                                // Then trigger the register event in the view model to save user data locally
                                //userViewModel.handleEvent(Register(request))
                                
                                // Navigate to home screen after registration
                                navController.navigate(Routes.HOME_SCREEN) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                // Handle error
                                val errorMsg = registrationSuccess.errorBody()?.string() ?: "Registration failed"
                                snackbarHostState.showSnackbar(errorMsg)
                            }
                        } catch (e: Exception) {
                            errorMessage = "Authentication failed: ${e.message}"
                            snackbarHostState.showSnackbar(errorMessage!!)
                            Log.e("RegisterScreen", "Auth failed", e)
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
                onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Login")
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}

